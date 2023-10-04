package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.constant.BatchSendStatus;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
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
public class RecoveryService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final AddressBatchRequestService addressBatchRequestService;
    private final SqsService sqsService;
    private final EventService eventService;
    private static final Integer MAX_BATCH_REQUEST_SIZE = 100;

    public RecoveryService(AddressBatchRequestRepository addressBatchRequestRepository,
                           AddressBatchRequestService addressBatchRequestService,
                           SqsService sqsService, EventService eventService) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.addressBatchRequestService = addressBatchRequestService;
        this.sqsService = sqsService;
        this.eventService = eventService;
    }

    @Scheduled(fixedDelayString = "${pn.address.manager.postel.batch.request.recovery.delay}")
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

    @Scheduled(fixedDelayString = "${pn.national-registries.inipec.batch.sqs.recovery.delay}")
    public void recoveryBatchSendToSqs() {
        log.trace("IniPEC - recoveryBatchSendToSqs start");
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
                                        e -> log.info("IniPEC - conditional check failed - skip recovery correlationId: {}", request.getCorrelationId(), e))
                                .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                        .collectList()
                        .flatMap(requestToRecover -> execBatchSendToSqs(requestToRecover, reservationId)
                                .thenReturn(requestToRecover.size()))
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + reservationId))
                        .subscribe(c -> log.info("IniPEC - executed batch SQS recovery on {} requests", c),
                                e -> log.error("IniPEC - failed execution of batch request SQS recovery", e));
            } else {
                log.info("IniPEC - no batch request to send to SQS to recover");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("IniPEC - recoveryBatchSendToSqs end");
    }

    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestToSend(lastEvaluatedKey, MAX_BATCH_REQUEST_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("Address Manager - can not get batch request - DynamoDB Mono<Page> is null");
                    //TODO: FIX EXCEPTION
                    return new PnAddressManagerException("AddressManager - can not get batch request", "", 1, "");
                });
    }

    public Mono<Void> batchSendToSqs(List<BatchRequest> batchRequest) {
        String reservationId = UUID.randomUUID().toString();
        return execBatchSendToSqs(batchRequest, reservationId)
                .doOnSubscribe(s -> log.info("PG - DigitalAddress - sending {} requests to SQS", batchRequest.size()));
    }

    private Mono<Void> execBatchSendToSqs(List<BatchRequest> batchRequest, String reservationId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(batchRequest.stream())
                .doOnNext(item -> {
                    item.setLastReserved(now);
                    item.setReservationId(reservationId);
                })
                .flatMap(item -> addressBatchRequestRepository.setNewReservationIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("PG - DigitalAddress - conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .flatMap(item -> {
                    if (!BatchStatus.ERROR.getValue().equalsIgnoreCase(item.getStatus())) {
                        String message = item.getMessage();
                        eventService.sendEvent(message, item.getCorrelationId());
                        log.info("PG - DigitalAddress - pushed message for correlationId: {}", item.getCorrelationId());
                        item.setSendStatus(BatchSendStatus.SENT.name());
                    } else {
                        return sqsService.sendToDlqQueue(item)
                                .thenReturn(item)
                                .doOnNext(r -> {
                                    log.info("PG - DigitalAddress - send to dlq queue message for correlationId: {}", item.getCorrelationId());
                                    item.setSendStatus(BatchSendStatus.SENT_TO_DLQ.name());
                                });
                    }
                    return null;
                })
                .flatMap(addressBatchRequestRepository::update)
                .then();
    }
}
