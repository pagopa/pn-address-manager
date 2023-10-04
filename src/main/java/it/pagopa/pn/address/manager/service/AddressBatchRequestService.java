package it.pagopa.pn.address.manager.service;

import com.amazonaws.services.eventbridge.model.PutEventsResult;
import it.pagopa.pn.address.manager.client.PostelClient;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.model.NormalizeRequestList;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static it.pagopa.pn.address.manager.constant.BatchStatus.NOT_WORKED;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@Slf4j
public class AddressBatchRequestService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final AddressConverter addressConverter;
    private final SqsService sqsService;
    private final PostelClient postelClient;
    private final SafeStorageService safeStorageService;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final EventService eventService;

    private final AddressUtils addressUtils;

    public AddressBatchRequestService(AddressBatchRequestRepository addressBatchRequestRepository,
                                      PostelBatchRepository postelBatchRepository,
                                      AddressConverter addressConverter,
                                      SqsService sqsService,
                                      PostelClient postelClient,
                                      SafeStorageService safeStorageService,
                                      PnAddressManagerConfig pnAddressManagerConfig,
                                      EventService eventService,
                                      AddressUtils addressUtils) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.postelBatchRepository = postelBatchRepository;
        this.addressConverter = addressConverter;
        this.safeStorageService = safeStorageService;
        this.sqsService = sqsService;
        this.postelClient = postelClient;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.eventService = eventService;
        this.addressUtils = addressUtils;
    }

    @Scheduled(fixedDelayString = "${pn.address-manager.postel.batch-request-delay}")
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


    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, pnAddressManagerConfig.getPostel().getBatchRequestMaxSize())
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
                    MDC.put("AWS_messageId", item.getAwsMessageId());
                    item.setBatchId(batchId);
                    item.setStatus(BatchStatus.TAKEN_CHARGE.name());
                    item.setLastReserved(now);
                })
                .flatMap(item -> addressBatchRequestRepository.setNewBatchIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("ADDRESS MANAGER -> POSTEL - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .collectList()
                .filter(requests -> !requests.isEmpty())
                .zipWhen(safeStorageService::callSelfStorageCreateFileAndUpload)
                .flatMap(t -> activatePostelBatch(t, batchId));
    }

    private Mono<Void> activatePostelBatch(Tuple2<List<BatchRequest>, FileCreationResponseDto> t, String batchId) {
        return createPostelBatch(t.getT2().getKey(), batchId)
                .onErrorResume(v -> incrementAndCheckRetry(t.getT1(), v, batchId).then(Mono.error(v)))
                .flatMap(response -> postelClient.activatePostel(t.getT2().getKey())
                        .map(activatePostelResponse -> {
                            log.info("ADDRESS MANAGER -> POSTEL - batchId {} - called activation service", batchId);
                            return updatePostelBatch(response, BatchStatus.WORKING);
                        })
                        .doOnError(e -> log.error("ADDRESS MANAGER -> POSTEL - batchId {} - failed to execute batch", batchId, e))
                        .onErrorResume(e -> Mono.empty())
                        .then());
    }

    private Mono<Void> updatePostelBatch(PostelBatch postelBatch, BatchStatus status) {
        log.info("ADDRESS MANAGER -> POSTEL - batchId {} - update PostelBatch with status: {}", postelBatch.getBatchId(), status);
        postelBatch.setStatus(status.name());
        return postelBatchRepository.update(postelBatch)
                .doOnNext(polling -> log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - updated PostelBatch with status: {}", postelBatch.getBatchId(), status))
                .then();
    }


    private Mono<PostelBatch> createPostelBatch(String fileKey, String batchId) {
        log.info("ADDRESS MANAGER -> POSTEL - batchId {} - creating PostelBatch with fileKey: {}", batchId, fileKey);
        return postelBatchRepository.create(addressConverter.createPostelBatchByBatchIdAndFileKey(batchId, fileKey))
                .flatMap(polling -> {
                    log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - created PostelBatch with fileKey: {}", batchId, fileKey);
                    return setBatchRequestStatusToWorking(batchId)
                            .map(batchRequests -> {
                                log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - created PostelBatch with fileKey: {}", batchId, fileKey);
                                return polling;
                            })
                            .doOnError(e -> log.warn("ADDRESS MANAGER -> POSTEL - batchId {} - failed to create PostelBatch with fileKey: {}", batchId, fileKey, e));
                });
    }

    private Mono<List<BatchRequest>> setBatchRequestStatusToWorking(String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(batchId, BatchStatus.TAKEN_CHARGE)
                .doOnNext(requests -> log.debug("ADDRESS MANAGER -> POSTEL - batchId {} - updating {} requests in status {}", batchId, requests.size(), BatchStatus.WORKING))
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> request.setStatus(BatchStatus.WORKING.getValue()))
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(r -> log.debug("ADDRESS MANAGER -> POSTEL - correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn("ADDRESS MANAGER -> POSTEL - batchId {} - failed to set request in status {}", batchId, BatchStatus.WORKING, e))
                .collectList();
    }


    protected Mono<Void> incrementAndCheckRetry(List<BatchRequest> requests, Throwable throwable, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(requests.stream())
                .doOnNext(r -> {
                    int nextRetry = (r.getRetry() != null) ? (r.getRetry() + 1) : 1;
                    r.setRetry(nextRetry);
                    if (nextRetry >= pnAddressManagerConfig.getPostel().getBatchRequestMaxRetry()
                            || (throwable instanceof PnAddressManagerException exception && exception.getStatus() == HttpStatus.BAD_REQUEST.value())) {
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
                    return sqsService.sendListToDlqQueue(l);
                });
    }

    public Mono<Void> updateBatchRequest(List<BatchRequest> batchRequests, String batchId) {
        return Flux.fromIterable(batchRequests)
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(request -> log.debug("Normalize Address - correlationId {} - set status in {}", request.getCorrelationId(), request.getStatus()))
                .flatMap(this::decodeStatus)
                .filter(request -> NOT_WORKED.getValue().equalsIgnoreCase(request.getStatus()))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(batchRequestList -> incrementAndCheckRetry(batchRequestList, null, batchId));
    }

    public Mono<Void> updateBatchRequestFromBatchId(PostelBatch postelBatch, BatchStatus status) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(postelBatch.getBatchId(), BatchStatus.WORKING)
                .doOnNext(requests -> log.debug("Normalize Address - batchId {} - updating {} requests in status {}", postelBatch.getBatchId(), requests.size(), status))
                .flatMap(batchRequestList -> updateBatchRequest(batchRequestList, postelBatch.getBatchId()))
                .then();
    }


    private Mono<BatchRequest> decodeStatus(BatchRequest request) {
        return switch (BatchStatus.fromValue(request.getStatus())) {
            case WORKED -> eventService.sendEvent(request.getMessage(), request.getClientId())
                    .thenReturn(request);
            case ERROR -> sqsService.sendToDlqQueue(request)
                    .thenReturn(request);
            default -> Mono.just(request);
        };
    }
}
