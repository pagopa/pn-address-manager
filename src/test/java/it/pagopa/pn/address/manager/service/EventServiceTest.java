package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class EventServiceTest {

    @MockitoBean
    EventBridgeAsyncClient eventBridgeAsyncClient;
    @MockitoBean
    PnAddressManagerConfig pnAddressManagerConfig;

    EventService eventService;


    @BeforeEach
    void setUp() {
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.EventBus eventBus = new PnAddressManagerConfig.EventBus();
        eventBus.setName("name");
        eventBus.setSource("source");
        eventBus.setDetailType("detail");
        pnAddressManagerConfig.setEventBus(eventBus);
        eventService = new EventService(eventBridgeAsyncClient, pnAddressManagerConfig);
    }

    @Test
    void testSendEvent() {
        PutEventsResponse response = PutEventsResponse.builder().build();
        when(eventBridgeAsyncClient.putEvents(any(PutEventsRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(response));
        StepVerifier.create(eventService.sendEvent("Test message"))
                .expectNext(response)
                .verifyComplete();
    }

}