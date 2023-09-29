package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Slf4j
@Service
public class NormalizeAddressService {

    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final AddressUtils addressUtils;
    private final SqsService sqsService;
    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final Scheduler scheduler;
    private final boolean flagCsv;
    private final long batchTtl;

    public NormalizeAddressService(ObjectMapper objectMapper,
                                   AddressUtils addressUtils,
                                   EventService eventService,
                                   SqsService sqsService,
                                   AddressBatchRequestRepository addressBatchRequestRepository,
                                   @Qualifier("addressManagerScheduler") Scheduler scheduler,
                                   @Value("${pn.address.manager.flag.csv}") boolean flagCsv,
                                   @Value("${pn.address.manager.batch.ttl}") long batchTtl) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.addressUtils = addressUtils;
        this.sqsService = sqsService;
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.scheduler = scheduler;
        this.flagCsv = flagCsv;
        this.batchTtl = batchTtl;
    }

    public Mono<AcceptedResponse> normalizeAddressAsync(String xApiKey, String cxId, NormalizeItemsRequest normalizeItemsRequest) {
        if (flagCsv) {
            Mono.fromCallable(() -> {
                NormalizeItemsResult normalizeItemsResult = normalizeRequestToResult(normalizeItemsRequest);
                sendEvents(normalizeItemsResult, cxId);
                return normalizeItemsResult;
            }).subscribeOn(scheduler).subscribe(normalizeItemsResult -> log.info("normalizeAddressAsync response: {}", normalizeItemsResult),
                    throwable -> log.error("normalizeAddressAsync error: {}", throwable.getMessage(), throwable));
        } else {
            sqsService.pushToInputQueue(InternalCodeSqsDto.builder()
                    .xApiKey(xApiKey)
                    .pnAddressManagerCxId(cxId)
                    .normalizeItemsRequest(normalizeItemsRequest)
                    .build(), cxId);
        }
        return Mono.just(mapToAcceptedResponse(normalizeItemsRequest));
    }

    public Mono<AcceptedResponse> handleMessage(PnAddressGatewayEvent.Payload payload) {
        return createBatchRequest(payload.getPnAddressManagerCxId(),payload.getNormalizeItemsRequest(), payload.getNormalizeItemsRequest().getCorrelationId())
                .doOnNext(batchRequest -> log.info("Created Batch Request for correlationId: {}", payload.getNormalizeItemsRequest().getCorrelationId()))
                .doOnError(throwable -> log.info("Failed to create Batch Request correlationId: {}", payload.getNormalizeItemsRequest().getCorrelationId()))
                .map(v -> mapToAcceptedResponse(payload.getNormalizeItemsRequest()));
    }

    public Mono<BatchRequest> createBatchRequest(String pnAddressManagerCxId, NormalizeItemsRequest normalizeItemsRequest, String correlationId) {
        BatchRequest batchRequest = createNewStartBatchRequest();

        String requestAsString;
        try {
            requestAsString = objectMapper.writeValueAsString(normalizeItemsRequest);
        } catch (JsonProcessingException e) {
            throw new PnAddressManagerException(e.getMessage(), "", 500, ""); // TODO: valorizzare campi
        }
        batchRequest.setAddresses(requestAsString);
        batchRequest.setClientId(pnAddressManagerCxId);
        batchRequest.setCorrelationId(correlationId);

        return addressBatchRequestRepository.create(batchRequest);
    }

    private BatchRequest createNewStartBatchRequest() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchRequest.setRetry(0);
        batchRequest.setLastReserved(now);
        batchRequest.setCreatedAt(now);
        batchRequest.setTtl(now.plusSeconds(batchTtl).toEpochSecond(ZoneOffset.UTC));
        log.trace("New Batch Request: {}", batchRequest);
        return batchRequest;
    }

    private NormalizeItemsResult normalizeRequestToResult(NormalizeItemsRequest normalizeItemsRequest) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        normalizeItemsResult.setCorrelationId(normalizeItemsRequest.getCorrelationId());
        normalizeItemsResult.setResultItems(addressUtils.normalizeAddresses(normalizeItemsRequest.getRequestItems()));
        return normalizeItemsResult;
    }

    private void sendEvents(NormalizeItemsResult normalizeItemsResult, String cxId) throws JsonProcessingException {
        EventDetail eventDetail = new EventDetail(normalizeItemsResult, cxId);
        String message = objectMapper.writeValueAsString(eventDetail);
        eventService.sendEvent(message, normalizeItemsResult.getCorrelationId());
    }

    private AcceptedResponse mapToAcceptedResponse(NormalizeItemsRequest normalizeItemsRequest) {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId(normalizeItemsRequest.getCorrelationId());
        return acceptedResponse;
    }
}