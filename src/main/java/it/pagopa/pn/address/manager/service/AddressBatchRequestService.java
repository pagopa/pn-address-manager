package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.client.PostelClient;
import it.pagopa.pn.address.manager.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@Slf4j
public class AddressBatchRequestService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final AddressConverter addressConverter;
    private final CsvService csvService;
    private final SqsService sqsService;
    private final PnSafeStorageClient pnSafeStorageClient;
    private final UploadDownloadClient uploadDownloadClient;
    private final PostelClient postelClient;
    private final int maxRetry;

    private final int maxBatchRequestSize;

    public AddressBatchRequestService(AddressBatchRequestRepository addressBatchRequestRepository,
                                      PostelBatchRepository postelBatchRepository,
                                      AddressConverter addressConverter,
                                      CsvService csvService,
                                      SqsService sqsService, PnSafeStorageClient pnSafeStorageClient,
                                      UploadDownloadClient uploadDownloadClient,
                                      PostelClient postelClient,
                                      @Value("${pn.address.manager.postel.batch.request.max-retry}") int maxRetry,
                                      @Value("${pn.address.manager.postel.batch.request.max-size}") int maxBatchRequestSize) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.postelBatchRepository = postelBatchRepository;
        this.addressConverter = addressConverter;
        this.csvService = csvService;
        this.sqsService = sqsService;
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.uploadDownloadClient = uploadDownloadClient;
        this.postelClient = postelClient;
        this.maxRetry = maxRetry;
        this.maxBatchRequestSize = maxBatchRequestSize;
    }

    @Scheduled(fixedDelayString = "${pn.address.manager.postel.batch.request.delay}")
    public void batchAddressRequest() {
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
                .doOnNext(c -> batchAddressRequest())
                .subscribe(c -> log.info("ADDRESS MANAGER -> POSTEL - executed batch recovery on {} requests", c),
                        e -> log.error("ADDRESS MANAGER -> POSTEL - failed execution of batch request recovery", e));
        log.trace("ADDRESS MANAGER -> POSTEL - recoveryBatchRequest end");
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, maxBatchRequestSize)
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
                    item.setStatus(BatchStatus.TAKEN_CHARGE.getValue());
                    item.setBatchId(batchId);
                    item.setLastReserved(now);
                })
                .flatMap(item -> addressBatchRequestRepository.setNewBatchIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("ADDRESS MANAGER -> POSTEL - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .collectList()
                .filter(requests -> !requests.isEmpty())
                .zipWhen(this::callSelfStorageCreateFileAndUpload)
                .flatMap(t -> createPostelBatch(t.getT2().getKey(), batchId)
                            .onErrorResume(v -> incrementAndCheckRetry(t.getT1(), v, batchId).then(Mono.error(v)))
                            .flatMap(response -> postelClient.activatePostel(t.getT2().getKey())
                            .thenReturn(t.getT1())
                )
                .doOnNext(requests -> log.info("ADDRESS MANAGER -> POSTEL - batchId {} - call activation service", batchId))
                .doOnError(e -> log.error("ADDRESS MANAGER -> POSTEL - batchId {} - failed to execute batch", batchId, e))
                .onErrorResume(e -> Mono.empty())
                .then());
    }

    private Mono<FileCreationResponseDto> callSelfStorageCreateFileAndUpload(List<BatchRequest> requests) {

        FileCreationRequestDto fileCreationRequestDto = new FileCreationRequestDto();
        fileCreationRequestDto.setContentType("text/csv");
        fileCreationRequestDto.setStatus("PRELOADED");
        fileCreationRequestDto.setDocumentType("PN_ADDRESSES_RAW"); // costante configurabile dall'esterno

        String csvContent = csvService.writeItemsOnCsvToString(requests);
        String sha256 = computeSha256(csvContent.getBytes());

        return pnSafeStorageClient.createFile(fileCreationRequestDto, "cxId")
                .doOnNext(fileCreationResponseDto -> uploadDownloadClient.uploadContent(csvContent, fileCreationResponseDto, sha256))
                .doOnError(e -> {
                    log.error("ADDRESS MANAGER -> POSTEL - failed to create file", e);
                    incrementAndCheckRetry(requests, e, requests.get(0).getBatchId()).then(Mono.error(e));
                });
    }


    private Mono<Void> createPostelBatch(String fileKey, String batchId) {
        log.info("ADDRESS MANAGER -> POSTEL - batchId {} - creating PostelBatch with fileKey: {}", batchId, fileKey);
        return postelBatchRepository.create(addressConverter.createPostelBatchByBatchIdAndFileKey(batchId, fileKey))
                .flatMap(polling -> {
                    log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - created PostelBatch with fileKey: {}", batchId, fileKey);
                    return setBatchRequestStatusToWorking(batchId);
                })
                .doOnNext(polling -> log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - created PostelBatch with fileKey: {}", batchId, fileKey))
                .doOnError(e -> log.warn("ADDRESS MANAGER -> POSTEL - batchId {} - failed to create PostelBatch with fileKey: {}", batchId, fileKey, e))
                .then();
    }

    private Mono<Void> setBatchRequestStatusToWorking(String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(batchId, BatchStatus.TAKEN_CHARGE)
                .doOnNext(requests -> log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - updating {} requests in status {}", batchId, requests.size(), BatchStatus.WORKING))
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> request.setStatus(BatchStatus.WORKING.getValue()))
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(r -> log.debug("ADDRESS MANAGER -> POSTEL - correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn("ADDRESS MANAGER -> POSTEL - batchId {} - failed to set request in status {}", batchId, BatchStatus.WORKING, e))
                .collectList()
                .then();
    }

    private String computeSha256(byte[] content) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(content);
            return bytesToBase64(encodedHash);
        } catch (Exception e) {
            throw new PnAddressManagerException("", "", 500, ""); // TODO: valorizzare
        }
    }

    private static String bytesToBase64(byte[] hash) {
        return Base64Utils.encodeToString(hash);
    }


    private Mono<Void> incrementAndCheckRetry(List<BatchRequest> requests, Throwable throwable, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(requests.stream())
                .doOnNext(r -> {
                    int nextRetry = (r.getRetry() != null) ? (r.getRetry() + 1) : 1;
                    r.setRetry(nextRetry);
                    if (nextRetry >= maxRetry || (throwable instanceof PnAddressManagerException exception && exception.getStatus() == HttpStatus.BAD_REQUEST.value())) {
                        r.setStatus(BatchStatus.ERROR.getValue());
                        r.setLastReserved(now);
                        log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - request {} status in {} (retry: {})", batchId, r.getCorrelationId(), r.getStatus(), r.getRetry());
                    }
                })
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(r -> log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - retry incremented for correlationId: {}", batchId, r.getCorrelationId()))
                .doOnError(e -> log.warn("ADDRESS MANAGER -> POSTEL - batchId {} - failed to increment retry", batchId, e))
                .filter(r -> BatchStatus.ERROR.getValue().equals(r.getStatus()))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(l -> {
                    log.debug("ADDRESS MANAGER -> POSTEL - there is at least one request in ERROR - call batch to send to SQS");
                    return sendListToDlqQueue(l);
                });
    }

    public Mono<Void> sendListToDlqQueue(List<BatchRequest> batchRequests) {
        return Flux.fromIterable(batchRequests)
                .map(this::sendToDlqQueue)
                .then();
    }

    public Mono<Void> sendToDlqQueue(BatchRequest batchRequest) {
        InternalCodeSqsDto internalCodeSqsDto = toInternalCodeSqsDto(batchRequest);
        return sqsService.pushToInputDlqQueue(internalCodeSqsDto, batchRequest.getClientId())
                .then();
    }

    private InternalCodeSqsDto toInternalCodeSqsDto(BatchRequest batchRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        NormalizeItemsRequest normalizeItemsRequest;
        try {
           normalizeItemsRequest = objectMapper.readValue(batchRequest.getAddresses(), NormalizeItemsRequest.class);
        } catch (JsonProcessingException e) {
            throw new PnAddressManagerException("", "", 500, ""); // TODO: valorizzare
        }

        return InternalCodeSqsDto.builder()
                .xApiKey(batchRequest.getXApiKey())
                .normalizeItemsRequest(normalizeItemsRequest)
                .pnAddressManagerCxId(batchRequest.getCxId())
                .build();
    }

}
