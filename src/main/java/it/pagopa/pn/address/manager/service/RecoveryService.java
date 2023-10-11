package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchSendStatus;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import lombok.extern.slf4j.Slf4j;
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

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_ADDRESSMANAGER_BATCHREQUEST;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@Slf4j
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
    public void recoveryBatchRequest() {
        log.trace("ADDRESS MANAGER -> POSTEL - recoveryBatchRequest start");
        addressBatchRequestRepository.getBatchRequestToRecovery()
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> {
                    request.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
                    request.setStatus(BatchStatus.NOT_WORKED.getValue());
                })
                .flatMap(request -> addressBatchRequestRepository.resetBatchRequestForRecovery(request)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("ADDRESS MANAGER -> POSTEL - conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .count()
                .doOnNext(c -> addressBatchRequestService.batchAddressRequest())
                .subscribe(c -> log.info("ADDRESS MANAGER -> POSTEL - executed batch recovery on {} requests", c),
                        e -> log.error("ADDRESS MANAGER -> POSTEL - failed execution of batch request recovery", e));
        log.trace("ADDRESS MANAGER -> POSTEL - recoveryBatchRequest end");
    }

    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.postel.recovery-delay}")
    public void recoveryPostelActivation() {
        log.trace("Normalizer - recovery postel activation start");
        postelBatchRepository.getPostelBatchToRecover()
                .flatMapIterable(postelBatch -> postelBatch)
                .doOnNext(postelBatch -> {
                    postelBatch.setStatus(BatchStatus.NOT_WORKED.getValue());
                    postelBatch.setReservationId(null);
                })
                .flatMap(postelBatch -> postelBatchRepository.resetPostelBatchForRecovery(postelBatch)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("Normalizer - conditional check failed - skip recovery  batchId {}", postelBatch.getBatchId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .doOnNext(addressBatchRequestService::callPostelActivationApi)
                .count()
                .subscribe(c -> log.info("Normalizer - executed batch recovery on {} polling", c),
                        e -> log.error("Normalizer - failed execution of postel activation recovery", e));
        log.trace("Normalizer - recovery postel activation end");
    }


    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-request.eventbridge-recovery-delay}")
    public void recoveryBatchSendToEventbridge() {
        log.trace("Normalizer - recoveryBatchSendToEventBridge start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String reservationId = UUID.randomUUID().toString();
                Flux.fromStream(page.items().stream())
                        .doOnNext(request -> request.setReservationId(null))
                        .flatMap(request -> addressBatchRequestRepository.resetBatchRequestForRecovery(request)
                                .doOnError(ConditionalCheckFailedException.class,
                                        e -> log.info("Normalizer - conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                                .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                        .collectList()
                        .flatMap(requestToRecover -> execBatchSendToEventBridge(requestToRecover, reservationId)
                                .thenReturn(requestToRecover.size()))
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + reservationId))
                        .subscribe(c -> log.info("Normalizer - executed batch EventBridge recovery on {} requests", c),
                                e -> log.error("Normalizer - failed execution of batch request EventBridge recovery", e));
            } else {
                log.info("Normalizer - no batch request to send to SQS to recover");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("Normalizer - recoveryBatchSendToEventBridge end");
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestToSend(lastEvaluatedKey, pnAddressManagerConfig.getNormalizer().getBatchRequest().getMaxSize())
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("Address Manager - can not get batch request - DynamoDB Mono<Page> is null");
                    return new PnAddressManagerException(ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST,
                            ADDRESS_NORMALIZER_ASYNC + ERROR_MESSAGE_ADDRESSMANAGER_BATCHREQUEST ,
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST);
                });
    }

    private Mono<Void> execBatchSendToEventBridge(List<BatchRequest> batchRequest, String reservationId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(batchRequest.stream())
                .doOnNext(item -> {
                    item.setLastReserved(now);
                    item.setReservationId(reservationId);
                })
                .flatMap(item -> addressBatchRequestRepository.setNewReservationIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("Normalize Address - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(this::evaluateStatusAndSendStatus)
                .filter(item -> BatchSendStatus.ERROR.getValue().equalsIgnoreCase(item.getSendStatus()))
                .flatMap(item -> sqsService.sendToDlqQueue(item)
                        .thenReturn(item)
                        .doOnNext(r -> {
                            log.info("PG - DigitalAddress - sent to dlq queue message for correlationId: {}", item.getCorrelationId());
                            item.setSendStatus(BatchSendStatus.SENT_TO_DLQ.name());
                        }))
                .flatMap(addressBatchRequestRepository::update)
                .then();
    }

    private Mono<BatchRequest> evaluateStatusAndSendStatus(BatchRequest item) {
        if (!BatchStatus.ERROR.getValue().equalsIgnoreCase(item.getStatus())) {
            String message = item.getMessage();
            return eventService.sendEvent(message, item.getCorrelationId())
                    .doOnNext(putEventsResult -> {
                        log.info("Normalize Address - sent event for correlationId: {}", item.getCorrelationId());
                        item.setSendStatus(BatchSendStatus.SENT.getValue());
                    })
                    .doOnError(throwable -> {
                        log.info("Normalize Address - error during send event for correlationId: {}", item.getCorrelationId());
                        if (item.getRetry() >= pnAddressManagerConfig.getNormalizer().getBatchRequest().getMaxRetry()) {
                            log.info("Normalize Address - retry exhausted for send event for correlationId: {}", item.getCorrelationId());
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
