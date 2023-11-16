package it.pagopa.pn.address.manager.service;

import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;


import java.util.ArrayList;
import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
public class EventService {

    private final EventBridgeAsyncClient eventBridgeAsyncClient;
    private final PnAddressManagerConfig pnAddressManagerConfig;


    public Mono<PutEventsResponse> sendEvent(String message) {
        return Mono.fromFuture(eventBridgeAsyncClient.putEvents(putEventsRequestBuilder(message)));
    }

    private PutEventsRequest putEventsRequestBuilder(String message) {
        List<PutEventsRequestEntry> entries = new ArrayList<>();
        PutEventsRequestEntry entryObj = PutEventsRequestEntry.builder()
                .detail(message)
                .eventBusName(pnAddressManagerConfig.getEventBus().getName())
                .detailType(pnAddressManagerConfig.getEventBus().getDetailType())
                .source(pnAddressManagerConfig.getEventBus().getSource())
                .build();
        entries.add(entryObj);
        PutEventsRequest putEventsRequest = PutEventsRequest.builder()
                .entries(entryObj)
                .build();
        log.debug("PutEventsRequest: {}", putEventsRequest);
        return putEventsRequest;
    }
}
