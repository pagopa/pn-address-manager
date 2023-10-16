package it.pagopa.pn.address.manager.service;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import com.amazonaws.services.eventbridge.model.PutEventsResultEntry;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class EventServiceTest {

    @MockBean
    AmazonEventBridgeAsync amazonEventBridge;
    @MockBean
    PnAddressManagerConfig pnAddressManagerConfig;

    EventService eventService;


    @BeforeEach
    public void setUp() {
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.EventBus eventBus = new PnAddressManagerConfig.EventBus();
        eventBus.setName("name");
        eventBus.setSource("source");
        eventBus.setDetailType("detail");
        pnAddressManagerConfig.setEventBus(eventBus);
        eventService = new EventService(amazonEventBridge, pnAddressManagerConfig);
    }

    @Test
    void testSendEvent() {
        when(amazonEventBridge.putEventsAsync(any(), any())).thenAnswer(invocation -> {
            AsyncHandler<PutEventsRequest, PutEventsResult> handler = invocation.getArgument(1);
            PutEventsResult putEventsResult = new PutEventsResult();
            List<PutEventsResultEntry> list = new ArrayList<>();
            putEventsResult.setEntries(list);
            handler.onSuccess(null, putEventsResult);
            return null;
        });
        StepVerifier.create(eventService.sendEvent("Test message"))
                .expectNextCount(0);
    }

}