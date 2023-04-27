package it.pagopa.pn.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.model.EventDetail;
import it.pagopa.pn.template.rest.v1.dto.AcceptedResponse;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.template.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;


@Slf4j
@Service
public class NormalizeAddressService {

    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final AddressUtils addressUtils;
    private final Scheduler scheduler;

    public NormalizeAddressService(ObjectMapper objectMapper,
                                   AddressUtils addressUtils,
                                   EventService eventService,
                                   @Qualifier("addressManagerScheduler") Scheduler scheduler) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.addressUtils = addressUtils;
        this.scheduler = scheduler;
    }

    public Mono<AcceptedResponse> normalizeAddressAsync(String cxId, NormalizeItemsRequest normalizeItemsRequest) {
        Mono.fromCallable(() -> {
            NormalizeItemsResult normalizeItemsResult = normalizeRequestToResult(normalizeItemsRequest);
            sendEvents(normalizeItemsResult, cxId);
            return normalizeItemsResult;
        }).subscribeOn(scheduler).subscribe(normalizeItemsResult -> log.info("normalizeAddressAsync response: {}", normalizeItemsResult),
                throwable -> log.error("normalizeAddressAsync error: {}", throwable.getMessage(), throwable));

        return Mono.just(mapToAcceptedResponse(normalizeItemsRequest));
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