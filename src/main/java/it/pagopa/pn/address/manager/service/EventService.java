package it.pagopa.pn.address.manager.service;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsRequestEntry;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EventService {

    private final AmazonEventBridgeAsync amazonEventBridge;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public EventService(AmazonEventBridgeAsync amazonEventBridge,
                        PnAddressManagerConfig pnAddressManagerConfig) {
        this.amazonEventBridge = amazonEventBridge;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    public Mono<PutEventsResult> sendEvent(String message, String correlationId) {
        return Mono.create(subscriber ->amazonEventBridge.putEventsAsync(putEventsRequestBuilder(message),
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
                }));
    }

    private PutEventsRequest putEventsRequestBuilder(String message) {
        PutEventsRequest putEventsRequest = new PutEventsRequest();
        List<PutEventsRequestEntry> entries = new ArrayList<>();
        PutEventsRequestEntry entryObj = new PutEventsRequestEntry();
        entryObj.setDetail(message);
        entryObj.setEventBusName(pnAddressManagerConfig.getEventBus().getName());
        entryObj.setDetailType(pnAddressManagerConfig.getEventBus().getDetailType());
        entryObj.setSource(pnAddressManagerConfig.getEventBus().getSource());
        entries.add(entryObj);
        putEventsRequest.setEntries(entries);
        log.debug("PutEventsRequest: {}", putEventsRequest);
        return putEventsRequest;
    }
}
