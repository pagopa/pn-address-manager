package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@Slf4j
public class AddressBatchRequestService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final AddressConverter addressConverter;
    private final CsvService csvService;
    private final PnSafeStorageClient pnSafeStorageClient;
    private final UploadDownloadClient uploadDownloadClient;

    private static final int MAX_BATCH_REQUEST_SIZE = 100;

    public AddressBatchRequestService(AddressBatchRequestRepository addressBatchRequestRepository,
                                      PostelBatchRepository postelBatchRepository, AddressConverter addressConverter, CsvService csvService, PnSafeStorageClient pnSafeStorageClient, UploadDownloadClient uploadDownloadClient) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.postelBatchRepository = postelBatchRepository;
        this.addressConverter = addressConverter;
        this.csvService = csvService;
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.uploadDownloadClient = uploadDownloadClient;
    }

    @Scheduled(fixedDelayString = "${pn.address.manager.postel.batch.request.delay}")
    public void batchPecRequest() {
        log.trace("ADDRESS MANAGER -> POSTEL - batchPecRequest start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String batchId = UUID.randomUUID().toString();
                execBatchRequest(page.items(), batchId)
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + batchId))
                        .block();
            } else {
                log.info("ADDRESS MANAGER -> POSTEL - no batch request available");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("ADDRESS MANAGER -> POSTEL - batchPecRequest end");
    }

    @Scheduled(fixedDelayString = "${pn.address.manager.postel.batch.request.recovery.delay}")
    public void recoveryBatchRequest() {
        log.trace("ADDRESS MANAGER -> POSTEL - recoveryBatchRequest start");
        addressBatchRequestRepository.getBatchRequestToRecovery()
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> {
                    request.setStatus(BatchStatus.NOT_WORKED.getValue());
                    request.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
                })
                .flatMap(request -> addressBatchRequestRepository.resetBatchRequestForRecovery(request)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("ADDRESS MANAGER -> POSTEL - conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .count()
                .doOnNext(c -> batchPecRequest())
                .subscribe(c -> log.info("ADDRESS MANAGER -> POSTEL - executed batch recovery on {} requests", c),
                        e -> log.error("ADDRESS MANAGER -> POSTEL - failed execution of batch request recovery", e));
        log.trace("ADDRESS MANAGER -> POSTEL - recoveryBatchRequest end");
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, MAX_BATCH_REQUEST_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("ADDRESS MANAGER -> POSTEL - can not get batch request - DynamoDB Mono<Page> is null");
                    return new PostelException("ADDRESS MANAGER -> POSTEL - can not get batch request");
                });
    }

    private Mono<Void> execBatchRequest(List<BatchRequest> items, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(items.stream())
                .doOnNext(item -> {
                    item.setStatus(BatchStatus.WORKING.getValue());
                    item.setBatchId(batchId);
                    item.setLastReserved(now);
                })
                .flatMap(item -> addressBatchRequestRepository.setNewBatchIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("ADDRESS MANAGER -> POSTEL - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .collectList()
                .filter(requests -> !requests.isEmpty())
                .zipWith(callSelfStorageCreateFile())
                .flatMap(t -> {
                    String csvContent = csvService.writeItemsOnCsvToString(t.getT1());
                    uploadDownloadClient.uploadContent(csvContent, t.getT2(), "sha256");
                    return createPostelBatch(t.getT2().getKey(), batchId);
                    // chiamiamo postel (attivazione)
                })
                //.doOnNext(requests -> log.info("ADDRESS MANAGER -> POSTEL - batchId {} - call activation service", batchId))
                .doOnError(e -> log.error("ADDRESS MANAGER -> POSTEL - batchId {} - failed to execute batch", batchId, e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<FileCreationResponseDto> callSelfStorageCreateFile() {
        FileCreationRequestDto fileCreationRequestDto = new FileCreationRequestDto();
        fileCreationRequestDto.setContentType("text/csv");
        fileCreationRequestDto.setStatus("status");
        fileCreationRequestDto.setDocumentType("documentType");
        return pnSafeStorageClient.createFile(fileCreationRequestDto, "cxId");
    }


    private Mono<Void> createPostelBatch(String fileKey, String batchId) {
        log.info("ADDRESS MANAGER -> POSTEL - batchId {} - creating PostelBatch with fileKey: {}", batchId, fileKey);
        return postelBatchRepository.create(addressConverter.createPostelBatchByBatchIdAndFileKey(batchId, fileKey))
                .doOnNext(polling -> log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - created PostelBatch with fileKey: {}", batchId, fileKey))
                .doOnError(e -> log.warn("ADDRESS MANAGER -> POSTEL - batchId {} - failed to create PostelBatch with fileKey: {}", batchId, fileKey, e))
                .then();
    }
}
