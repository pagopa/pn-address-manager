package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchSendStatus;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
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

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.BatchStatus.TAKEN_CHARGE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_ADDRESSMANAGER_BATCHREQUEST;

@Service
@CustomLog
@RequiredArgsConstructor
public class RecoveryService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final AddressBatchRequestService addressBatchRequestService;
    private final SqsService sqsService;
    private final EventService eventService;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final AddressUtils addressUtils;
    private final PostelBatchRepository postelBatchRepository;

    /**
     * The recoveryBatchRequest function is responsible for recovering batch requests that have been
     * assigned to a batch but not yet processed. This function will recover all the requests
     * that are in the TAKEN_CHARGE status and have lastReserved data more than configurable delay have
     * passed since the last processing (lastReserved column)
     */
    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-request.recovery-delay}")
    @SchedulerLock(name = "batchRequestRecovery", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-most}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-least}")
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

    /**
     * The recoveryPostelActivation function is a scheduled function that checks the
     * postelBatchRepository for any batches that have been marked as failed,
     * and resets them to be ready for recovery.
     * The resetPostelBatchForRecovery function in the PostelBatchRepository class uses conditional check expressions to ensure
     * that only batches with a status of FAILED are updated.
     * Finally, recall postel Normalizer for each postel batch.
     * If there are no such batches, then nothing happens and the scheduler waits until its next run time before checking again.
     */
    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.postel.recovery-delay}")
    @SchedulerLock(name = "postelBatch", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-most}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-least}")
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


    /**
     * The recoveryBatchSendToEventbridge function is a scheduled function that recovers any batch requests
     * that were not sent to EventBridge due to an error in the sendToEventbridge function.
     * The recoveryBatchSendToEventbridge function uses the getBatchRequest() method, which queries DynamoDB for all BatchRequests with
     * a sendStatus of NOT_SENT;.
     * If there are no pending batch requests, then it logs this information and ends execution.
     * Otherwise, it creates a reservationId (a UUID) and executes execBatchSendToEventBridge().
     */
    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-request.eventbridge-recovery-delay}")
    @SchedulerLock(name = "sendToEventBridgeRecovery", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-most}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-least}")
    public void recoveryBatchSendToEventbridge() {
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recoveryBatchSendToEventBridge start");
        Page<PnRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                Mono.just(page.items())
                        .flatMap(requestToRecover -> execBatchSendToEventBridge(requestToRecover)
                                .thenReturn(requestToRecover.size()))
                        .subscribe(c -> log.info(ADDRESS_NORMALIZER_ASYNC + "executed batch EventBridge recovery on {} requests", c),
                                e -> log.error(ADDRESS_NORMALIZER_ASYNC + "failed execution of batch request EventBridge recovery", e));
            } else {
                log.info(ADDRESS_NORMALIZER_ASYNC + "no batch request to send to EventBridge to recover");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recoveryBatchSendToEventBridge end");
    }

    /**
     * The cleanStoppedRequest function is a scheduled function that checks for any PostelBatch objects in the DynamoDB table
     * with a status of WORKING and workingTtl expires.
     * If it finds any, it will then retrieve any related batchRequest, reset them and call incrementAndCheckRetry for these batch requests.
     * Finally, delete any expired postelBatch
     */
    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-clean-request}")
    @SchedulerLock(name = "cleanStoppedRequest", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-most}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-least}")
    public void cleanStoppedRequest() {
        log.trace(ADDRESS_NORMALIZER_ASYNC + "recovery postel activation start");
        Page<NormalizzatoreBatch> page = postelBatchRepository.getPostelBatchToClean()
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn(ADDRESS_NORMALIZER_ASYNC + "can not get batch request - DynamoDB Mono<Page> is null");
                    return new PostelException(ADDRESS_NORMALIZER_ASYNC + "can not get batch request");
                });

        if (!page.items().isEmpty()) {
            page.items().forEach(postelBatch -> {
                List<PnRequest> pnRequestList = addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(postelBatch.getBatchId(), BatchStatus.WORKING)
                        .block();

                if(!CollectionUtils.isEmpty(pnRequestList)) {

                    pnRequestList.forEach(batchRequest -> {
                        batchRequest.setStatus(TAKEN_CHARGE.getValue());
                        addressBatchRequestRepository.update(batchRequest).block();
                    });

                    addressBatchRequestService.incrementAndCheckRetry(pnRequestList, null, postelBatch.getBatchId())
                            .doOnNext(request -> log.debug(ADDRESS_NORMALIZER_ASYNC + " increment retry for {} request", pnRequestList.size()))
                            .block();

                    postelBatchRepository.deleteItem(postelBatch.getBatchId());
                }

            });
        } else {
            log.info(ADDRESS_NORMALIZER_ASYNC + "no Postel batch to clean");
        }
    }

    private Page<PnRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
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

    private Mono<Void> execBatchSendToEventBridge(List<PnRequest> pnRequestList) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(pnRequestList.stream())
                .doOnNext(item -> item.setLastReserved(now))
                .flatMap(item -> addressBatchRequestRepository.setNewReservationIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info(ADDRESS_NORMALIZER_ASYNC + "conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(this::evaluateStatusAndSendStatus)
                .flatMap(this::checkSendStatusToSendToDLQ)
                .flatMap(addressBatchRequestRepository::update)
                .then();
    }

    private Mono<PnRequest> checkSendStatusToSendToDLQ(PnRequest item) {
        if(BatchSendStatus.ERROR.getValue().equalsIgnoreCase(item.getSendStatus())) {
            return sqsService.sendToDlqQueue(item)
                    .thenReturn(item)
                    .doOnNext(r -> {
                        log.info(ADDRESS_NORMALIZER_ASYNC + "sent to dlq queue message for correlationId: {}", item.getCorrelationId());
                        item.setSendStatus(BatchSendStatus.SENT_TO_DLQ.name());
                    });
        }
        return Mono.just(item);
    }

    private Mono<PnRequest> evaluateStatusAndSendStatus(PnRequest item) {
        if (!BatchStatus.ERROR.getValue().equalsIgnoreCase(item.getStatus())) {

            NormalizeItemsResult normalizeItemsResult = constructNormalizeItemResult(item);
            EventDetail eventDetail = new EventDetail(normalizeItemsResult, item.getClientId());
            String message = addressUtils.toJson(eventDetail);

            return eventService.sendEvent(message)
                    .doOnNext(putEventsResult -> {
                        log.info("Event with correlationId {} sent successfully", item.getCorrelationId());
                        log.debug("Sent event result: {}", putEventsResult.entries());
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

    private NormalizeItemsResult constructNormalizeItemResult(PnRequest item) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        List<NormalizeResult> itemsResult = addressUtils.getNormalizeResultFromBatchRequest(item);
        normalizeItemsResult.setResultItems(itemsResult);
        normalizeItemsResult.setCorrelationId(item.getCorrelationId());
        return normalizeItemsResult;
    }
}
