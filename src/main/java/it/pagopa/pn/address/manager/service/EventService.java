package it.pagopa.pn.address.manager.service;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsRequestEntry;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@lombok.CustomLog
public class EventService {

    private final AmazonEventBridgeAsync amazonEventBridge;
    private final String eventBusName;
    private final String eventBusDetailType;
    private final String eventBusSource;

    public EventService(AmazonEventBridgeAsync amazonEventBridge,
                        @Value("${pn.address.manager.eventbus.name}") String eventBusName,
                        @Value("${pn.address.manager.eventbus.source}") String eventBusSource,
                        @Value("${pn.address.manager.eventbus.detail.type}") String eventBusDetailType) {
        this.amazonEventBridge = amazonEventBridge;
        this.eventBusName = eventBusName;
        this.eventBusSource = eventBusSource;
        this.eventBusDetailType = eventBusDetailType;
    }

    public void sendEvent(String message, String correlationId) {
        amazonEventBridge.putEventsAsync(putEventsRequestBuilder(message),
                new AsyncHandler<>() {
                    @Override
                    public void onError(Exception e) {
                        log.error("Send event with correlationId {} failed", correlationId, e);
                    }

                    @Override
                    public void onSuccess(PutEventsRequest request, PutEventsResult putEventsResult) {
                        log.info("Event with correlationId {} sent successfully", correlationId);
                        log.debug("Sent event result: {}", putEventsResult.getEntries());
                    }
                });
    }

    private PutEventsRequest putEventsRequestBuilder(String message) {
        PutEventsRequest putEventsRequest = new PutEventsRequest();
        List<PutEventsRequestEntry> entries = new ArrayList<>();
        PutEventsRequestEntry entryObj = new PutEventsRequestEntry();
        entryObj.setDetail(message);
        entryObj.setEventBusName(eventBusName);
        entryObj.setDetailType(eventBusDetailType);
        entryObj.setSource(eventBusSource);
        entries.add(entryObj);
        putEventsRequest.setEntries(entries);
        log.debug("PutEventsRequest: {}", putEventsRequest);
        return putEventsRequest;
    }
}
