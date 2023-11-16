package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.function.Consumer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {NormalizeInputsHandler.class})
@ExtendWith(SpringExtension.class)
class NormalizeInputsHandlerTest {
    @MockBean
    private NormalizeAddressService normalizeAddressService;

    @Autowired
    private NormalizeInputsHandler normalizeInputsHandler;

    /**
     * Method under test: {@link NormalizeInputsHandler#pnAddressManagerRequestConsumer()}
     */
    @Test
    void testPnAddressManagerRequestConsumer() {
        Message<PnNormalizeRequestEvent.Payload> message = getPnRequestMessage();
        when(normalizeAddressService.handleRequest(any())).thenReturn(Mono.just("").then());
        //WHEN
        Consumer<Message<PnNormalizeRequestEvent.Payload>> consumer = normalizeInputsHandler.pnAddressManagerRequestConsumer();
        consumer.accept(message);
        verify(normalizeAddressService, times(1)).handleRequest(any());

    }

    private Message<PnNormalizeRequestEvent.Payload> getPnRequestMessage() {
        return new Message<>() {
            @Override
            @NotNull
            public PnNormalizeRequestEvent.Payload getPayload() {
                return PnNormalizeRequestEvent.Payload.builder()
                        .normalizeItemsRequest(new NormalizeItemsRequest())
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

