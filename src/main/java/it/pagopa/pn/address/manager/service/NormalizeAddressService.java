package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.model.WsNormAccInputModel;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;


@Slf4j
@Service
public class NormalizeAddressService {

    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final AddressUtils addressUtils;
    private final CsvService csvService;
    private final boolean flagCsv;
    private final Scheduler scheduler;

    public NormalizeAddressService(ObjectMapper objectMapper,
                                   AddressUtils addressUtils,
                                   EventService eventService,
                                   CsvService csvService,
                                   @Value("${pn.address.manager.flag.csv}") boolean flagCsv,
                                   @Qualifier("addressManagerScheduler") Scheduler scheduler) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.addressUtils = addressUtils;
        this.csvService = csvService;
        this.flagCsv = flagCsv;
        this.scheduler = scheduler;
    }

    public Mono<AcceptedResponse> normalizeAddressAsync(String cxId, NormalizeItemsRequest normalizeItemsRequest) {
        if (flagCsv) {
            Mono.fromCallable(() -> {
                NormalizeItemsResult normalizeItemsResult = normalizeRequestToResult(normalizeItemsRequest);
                sendEvents(normalizeItemsResult, cxId);
                return normalizeItemsResult;
            }).subscribeOn(scheduler).subscribe(normalizeItemsResult -> log.info("normalizeAddressAsync response: {}", normalizeItemsResult),
                    throwable -> log.error("normalizeAddressAsync error: {}", throwable.getMessage(), throwable));
        }
        else{
            List<WsNormAccInputModel> wsNormAccInputModels = addressUtils.normalizeRequestToWsNormAccInputModel(normalizeItemsRequest.getRequestItems());
            csvService.writeItemsOnCsv(wsNormAccInputModels, normalizeItemsRequest.getCorrelationId()+".csv", "C:\\Users\\baldivi\\Desktop\\");
        }
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