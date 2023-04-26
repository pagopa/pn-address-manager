package it.pagopa.pn.template.service;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsRequestEntry;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EventService {

    private final AmazonEventBridgeAsync amazonEventBridge;
    private final String eventBusName;
    private final String eventBusDetailType;

    public EventService(AmazonEventBridgeAsync amazonEventBridge,
                        @Value("${pn.address.manager.eventbus.name}") String eventBusName,
                        @Value("${pn.address.manager.eventbus.detail.type}") String eventBusDetailType) {
        this.amazonEventBridge = amazonEventBridge;
        this.eventBusName = eventBusName;
        this.eventBusDetailType = eventBusDetailType;
    }

    public void sendEvent(String message) {
        amazonEventBridge.putEventsAsync(putEventsRequestBuilder(message),
                new AsyncHandler<>() {
                    @Override
                    public void onError(Exception e) {
                        log.error("error");
                    }

                    @Override
                    public void onSuccess(PutEventsRequest request, PutEventsResult putEventsResult) {
                        log.info("success");
                        log.debug(putEventsResult.getEntries().toString());
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
        entryObj.setSource("pn-address-manager");
        entries.add(entryObj);
        putEventsRequest.setEntries(entries);
        return putEventsRequest;
    }
}
