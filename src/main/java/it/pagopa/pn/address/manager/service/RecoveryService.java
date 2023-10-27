package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchSendStatus;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import lombok.CustomLog;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.http.HttpStatus;
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

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.BatchStatus.TAKEN_CHARGE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_ADDRESSMANAGER_BATCHREQUEST;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@CustomLog
public class RecoveryService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final AddressBatchRequestService addressBatchRequestService;
    private final SqsService sqsService;
    private final EventService eventService;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    private final PostelBatchRepository postelBatchRepository;

    public RecoveryService(AddressBatchRequestRepository addressBatchRequestRepository,
                           AddressBatchRequestService addressBatchRequestService,
                           SqsService sqsService, EventService eventService,
                           PnAddressManagerConfig pnAddressManagerConfig,
                           PostelBatchRepository postelBatchRepository) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.addressBatchRequestService = addressBatchRequestService;
        this.sqsService = sqsService;
        this.eventService = eventService;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.postelBatchRepository = postelBatchRepository;
    }

    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-request.recovery-delay}")
    @SchedulerLock(name = "batchRequestRecovery", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lockAtMostFor}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-request.lockAtLeastFor}")
    public void recoveryBatchRequest() {
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recoveryBatchRequest start");
        addressBatchRequestRepository.getBatchRequestToRecovery()
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> {
                    request.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
                    request.setStatus(BatchStatus.NOT_WORKED.getValue());
                })
                .flatMap(request -> addressBatchRequestRepository.resetBatchRequestForRecovery(request)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info(ADDRESS_NORMALIZER_ASYNC + "conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .count()
                .subscribe(batchRequests -> log.info(ADDRESS_NORMALIZER_ASYNC + "executed batch recovery on {} requests", batchRequests),
                        e -> log.error(ADDRESS_NORMALIZER_ASYNC + "failed execution of batch request recovery", e));
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recoveryBatchRequest end");
    }

    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.postel.recovery-delay}")
    @SchedulerLock(name = "postelBatch", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lockAtMostFor}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-request.lockAtLeastFor}")
    public void recoveryPostelActivation() {
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recovery postel activation start");
        postelBatchRepository.getPostelBatchToRecover()
                .flatMapIterable(postelBatch -> postelBatch)
                .flatMap(postelBatch -> postelBatchRepository.resetPostelBatchForRecovery(postelBatch)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info(ADDRESS_NORMALIZER_ASYNC + "conditional check failed - skip recovery  batchId {}", postelBatch.getBatchId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .doOnNext(addressBatchRequestService::callPostelActivationApi)
                .count()
                .subscribe(c -> log.info(ADDRESS_NORMALIZER_ASYNC + "executed batch recovery on {} polling", c),
                        e -> log.error(ADDRESS_NORMALIZER_ASYNC + "failed execution of postel activation recovery", e));
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recovery postel activation end");
    }


    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-request.eventbridge-recovery-delay}")
    @SchedulerLock(name = "sendToEventBridgeRecovery", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lockAtMostFor}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-request.lockAtLeastFor}")
    public void recoveryBatchSendToEventbridge() {
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recoveryBatchSendToEventBridge start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String reservationId = UUID.randomUUID().toString();
                Mono.just(page.items())
                        .flatMap(requestToRecover -> execBatchSendToEventBridge(requestToRecover)
                                .thenReturn(requestToRecover.size()))
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + reservationId))
                        .subscribe(c -> log.info(ADDRESS_NORMALIZER_ASYNC + "executed batch EventBridge recovery on {} requests", c),
                                e -> log.error(ADDRESS_NORMALIZER_ASYNC + "failed execution of batch request EventBridge recovery", e));
            } else {
                log.info(ADDRESS_NORMALIZER_ASYNC + "no batch request to send to EventBridge to recover");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recoveryBatchSendToEventBridge end");
    }

    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-clean-request}")
    @SchedulerLock(name = "cleanStoppedRequest", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lockAtMostFor}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-request.lockAtLeastFor}")
    public void cleanStoppedRequest() {
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recovery postel activation start");
        Page<PostelBatch> page = postelBatchRepository.getPostelBatchToClean()
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn(ADDRESS_NORMALIZER_ASYNC + "can not get batch request - DynamoDB Mono<Page> is null");
                    return new PostelException(ADDRESS_NORMALIZER_ASYNC + "can not get batch request");
                });

        if (!page.items().isEmpty()) {
            page.items().forEach(postelBatch -> {
                List<BatchRequest> batchRequestList = addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(postelBatch.getBatchId(), BatchStatus.WORKING)
                        .block();

                if(!CollectionUtils.isEmpty(batchRequestList)) {

                    batchRequestList.forEach(batchRequest -> {
                        batchRequest.setStatus(TAKEN_CHARGE.getValue());
                        addressBatchRequestRepository.update(batchRequest).block();
                    });

                    addressBatchRequestService.incrementAndCheckRetry(batchRequestList, null, postelBatch.getBatchId())
                            .doOnNext(request -> log.debug(ADDRESS_NORMALIZER_ASYNC + " increment retry for {} request", batchRequestList.size()))
                            .block();

                    postelBatchRepository.deleteItem(postelBatch.getBatchId());
                }

            });
        } else {
            log.info(ADDRESS_NORMALIZER_ASYNC + "no Postel batch to clean");
        }
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestToSend(lastEvaluatedKey, pnAddressManagerConfig.getNormalizer().getBatchRequest().getQueryMaxSize())
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn(ADDRESS_NORMALIZER_ASYNC + " can not get batch request - DynamoDB Mono<Page> is null");
                    return new PnInternalAddressManagerException(ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST,
                            ADDRESS_NORMALIZER_ASYNC + ERROR_MESSAGE_ADDRESSMANAGER_BATCHREQUEST,
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST);
                });
    }

    private Mono<Void> execBatchSendToEventBridge(List<BatchRequest> batchRequest) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(batchRequest.stream())
                .doOnNext(item -> item.setLastReserved(now))
                .flatMap(item -> addressBatchRequestRepository.setNewReservationIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info(ADDRESS_NORMALIZER_ASYNC + "conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(this::evaluateStatusAndSendStatus)
                .filter(item -> BatchSendStatus.ERROR.getValue().equalsIgnoreCase(item.getSendStatus()))
                .flatMap(item -> sqsService.sendToDlqQueue(item)
                        .thenReturn(item)
                        .doOnNext(r -> {
                            log.info(ADDRESS_NORMALIZER_ASYNC + "sent to dlq queue message for correlationId: {}", item.getCorrelationId());
                            item.setSendStatus(BatchSendStatus.SENT_TO_DLQ.name());
                        }))
                .flatMap(addressBatchRequestRepository::update)
                .then();
    }

    private Mono<BatchRequest> evaluateStatusAndSendStatus(BatchRequest item) {
        if (!BatchStatus.ERROR.getValue().equalsIgnoreCase(item.getStatus())) {
            String message = item.getMessage();
            return eventService.sendEvent(message)
                    .doOnNext(putEventsResult -> {
                        log.info("Event with correlationId {} sent successfully", item.getCorrelationId());
                        log.debug("Sent event result: {}", putEventsResult.getEntries());
                        item.setSendStatus(BatchSendStatus.SENT.getValue());
                    })
                    .doOnError(throwable -> {
                        log.error("Send event with correlationId {} failed", item.getCorrelationId(), throwable);
                        if (item.getRetry() >= pnAddressManagerConfig.getNormalizer().getBatchRequest().getMaxRetry()) {
                            log.info(ADDRESS_NORMALIZER_ASYNC + "retry exhausted for send event for correlationId: {}", item.getCorrelationId());
                            item.setSendStatus(BatchSendStatus.ERROR.getValue());
                        }
                        item.setSendStatus(BatchSendStatus.NOT_SENT.getValue());
                    })
                    .thenReturn(item);
        } else {
            item.setSendStatus(BatchSendStatus.ERROR.getValue());
            return Mono.just(item);
        }
    }
}
