package it.pagopa.pn.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.model.EventDetail;
import it.pagopa.pn.template.rest.v1.dto.AcceptedResponse;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.template.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class NormalizeAddressService {

    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final AddressUtils addressUtils;

    public NormalizeAddressService(ObjectMapper objectMapper,
                                   AddressUtils addressUtils,
                                   EventService eventService) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.addressUtils = addressUtils;
    }

    public Mono<AcceptedResponse> normalizeAddressAsync(String cxId, Mono<NormalizeItemsRequest> normalizeItemsRequest) {
        return normalizeItemsRequest.map(n -> {
            NormalizeItemsResult normalizeItemsResult = addressUtils.normalizeAddresses(n.getCorrelationId(), n.getRequestItems());
            sendEvents(normalizeItemsResult, cxId);
            return mapToAcceptedResponse(n);
        });
    }

    private AcceptedResponse mapToAcceptedResponse(NormalizeItemsRequest normalizeItemsRequest) {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId(normalizeItemsRequest.getCorrelationId());
        return acceptedResponse;
    }
    private void sendEvents(NormalizeItemsResult normalizeItemsResult, String cxId) {
        try {
            EventDetail eventDetail = new EventDetail(normalizeItemsResult, cxId);
            String message = objectMapper.writeValueAsString(eventDetail);
            eventService.sendEvent(message);
        } catch (JsonProcessingException e) {
            log.error("error");
        }
    }
}