package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {NormalizerCallbackHandler.class})
@ExtendWith(SpringExtension.class)
class NormalizerCallbackHandlerTest {
    @MockitoBean
    private NormalizeAddressService normalizeAddressService;

    @Autowired
    private NormalizerCallbackHandler normalizerCallbackHandler;

    /**
     * Method under test: {@link NormalizerCallbackHandler#pnAddressManagerPostelCallbackConsumer(Message)}
     */
    @Test
    void testNormalizerCallbackConsumer() {
        Message<PnPostelCallbackEvent.Payload> message = getPostelCallbackMessage();
        when(normalizeAddressService.handlePostelCallback(any())).thenReturn(Mono.just("").then());
        //WHEN
        normalizerCallbackHandler.pnAddressManagerPostelCallbackConsumer(message);
        verify(normalizeAddressService, times(1)).handlePostelCallback(any());

    }

    private Message<PnPostelCallbackEvent.Payload> getPostelCallbackMessage() {
        return new Message<>() {
            @Override
            @NotNull
            public PnPostelCallbackEvent.Payload getPayload() {
                return PnPostelCallbackEvent.Payload.builder()
                        .requestId("requestId")
                        .error("error")
                        .build();
            }

            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }
}

