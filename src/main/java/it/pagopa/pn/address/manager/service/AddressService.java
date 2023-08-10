package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.client.PagoPaClient;
import it.pagopa.pn.address.manager.client.PnSafeStorageClient;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.model.WsNormAccInputModel;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.address.manager.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;

@Service
@lombok.CustomLog
public class AddressService {

    private final PagoPaClient pagoPaClient;
    private final EventService eventService;
    private final CsvService csvService;
    private final ISINIReceiverService isiniReceiverService;
    private final AddressConverter addressConverter;
    private final AddressUtils addressUtils;
    private final Scheduler scheduler;
    private final boolean flagCsv;


    public AddressService(PagoPaClient pagoPaClient,
                          EventService eventService,
                          CsvService csvService,
                          ISINIReceiverService isiniReceiverService, AddressConverter addressConverter,
                          AddressUtils addressUtils,
                          @Qualifier("addressManagerScheduler") Scheduler scheduler,
                          @Value("${pn.address.manager.flag.csv}") boolean flagCsv){
        this.pagoPaClient = pagoPaClient;
        this.eventService = eventService;
        this.csvService = csvService;
        this.isiniReceiverService = isiniReceiverService;
        this.addressConverter = addressConverter;
        this.addressUtils = addressUtils;
        this.scheduler = scheduler;
        this.flagCsv = flagCsv;
    }

    public Mono<AcceptedResponse> normalizeAddressAsync(NormalizeItemsRequest normalizeItemsRequest, String cxId) {
        if (flagCsv) {
            Mono.fromCallable(() -> {
                        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
                        normalizeItemsResult.setCorrelationId(normalizeItemsRequest.getCorrelationId());
                        normalizeItemsResult.setResultItems(addressUtils.normalizeAddresses(normalizeItemsRequest.getRequestItems()));
                        sendEvents(normalizeItemsResult, cxId);
                        return normalizeItemsResult;
                    }).subscribeOn(scheduler)
                    .subscribe(normalizeItemsResult ->
                            log.info("normalizeAddressAsync response: {}", normalizeItemsResult), throwable ->
                            log.error("normalizeAddressAsync error: {}", throwable.getMessage(), throwable));
        }
        else{
            List<WsNormAccInputModel> wsNormAccInputModels = addressConverter.normalizeRequestToWsNormAccInputModel(normalizeItemsRequest.getRequestItems());
            csvService.writeItemsOnCsv(wsNormAccInputModels, normalizeItemsRequest.getCorrelationId()+".csv", "C:\\Users\\baldivi\\Desktop\\");
			//ToDo: Aggiungere chiamara pnSafeStorageClient createFile
            isiniReceiverService.activateSINIComponent();// TODO: Check response and throw exception if it fails
        }
        return Mono.just(addressConverter.normalizeItemsRequestToAcceptedResponse(normalizeItemsRequest));
    }

    public Mono<DeduplicatesResponse> normalizeAddress(DeduplicatesRequest request) {
        if(flagCsv){
            NormalizedAddressResponse normalizeAddressResponse = addressUtils.normalizeAddress(request.getTargetAddress(), null);

            DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
            deduplicatesResponse.setCorrelationId(request.getCorrelationId());
            deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress(), normalizeAddressResponse.isItalian()));
            deduplicatesResponse.setError(normalizeAddressResponse.getError());
            deduplicatesResponse.setNormalizedAddress(normalizeAddressResponse.getNormalizedAddress());

            return Mono.just(deduplicatesResponse);
        }
        else{
            return pagoPaClient
                    .deduplicaOnline(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(request))
                    .map(addressConverter::createDeduplicatesResponseFromDeduplicaResponse);
        }
    }

    private void sendEvents(NormalizeItemsResult normalizeItemsResult, String cxId){
        EventDetail eventDetail = new EventDetail(normalizeItemsResult, cxId);
        String message = JsonUtils.writeValueAsString(eventDetail);
        eventService.sendEvent(message, normalizeItemsResult.getCorrelationId());
    }
}
