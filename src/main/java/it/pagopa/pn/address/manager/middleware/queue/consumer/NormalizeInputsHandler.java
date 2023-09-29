package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnAddressGatewayEvent;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@lombok.CustomLog
@RequiredArgsConstructor
public class NormalizeInputsHandler {
    private final NormalizeAddressService normalizeAddressService;

    private static final String HANDLER_PROCESS = "pnAddressManagerRequestConsumer";

    @Bean
    public Consumer<Message<PnAddressGatewayEvent.Payload>> pnAddressManagerRequestConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_PROCESS);
            log.debug(HANDLER_PROCESS + "- message: {}", message);
            MDC.put("correlationId", message.getPayload().getNormalizeItemsRequest().getCorrelationId());
            normalizeAddressService.handleMessage(message.getPayload())
                    .doOnNext(addressOKDto -> log.logEndingProcess(HANDLER_PROCESS))
                    .doOnError(e -> {
                        log.logEndingProcess(HANDLER_PROCESS, false, e.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), e);
                    })
                    .block();
        };
    }

}
