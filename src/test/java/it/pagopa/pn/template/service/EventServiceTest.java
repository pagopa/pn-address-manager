package it.pagopa.pn.template.service;

import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private AmazonEventBridgeAsync eventBridgeAsync;

    @Test
    void sendEvent(){
        CompletableFuture<PutEventsResult> future = new CompletableFuture<>();
        when(eventBridgeAsync.putEventsAsync(any(),any())).thenReturn(future);
        Assertions.assertDoesNotThrow(() -> eventService.sendEvent("json","correlationId"));
    }
}
