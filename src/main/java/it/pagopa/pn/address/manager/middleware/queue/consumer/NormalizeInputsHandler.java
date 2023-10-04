package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
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

    private static final String HANDLER_REQUEST = "pnAddressManagerRequestConsumer";
    private static final String HANDLER_POSTEL_CALLBACK = "pnAddressManagerPostelCallbackConsumer";

    @Bean
    public Consumer<Message<PnNormalizeRequestEvent.Payload>> pnAddressManagerRequestConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_REQUEST);
            log.debug(HANDLER_REQUEST + "- message: {}", message);
            MDC.put("correlationId", message.getPayload().getNormalizeItemsRequest().getCorrelationId());
            normalizeAddressService.handleRequest(message.getPayload())
                    .doOnNext(addressOKDto -> log.logEndingProcess(HANDLER_REQUEST))
                    .doOnError(e -> {
                        log.logEndingProcess(HANDLER_REQUEST, false, e.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), e);
                    })
                    .block();
        };
    }

    @Bean
    public Consumer<Message<PnPostelCallbackEvent.Payload>> pnAddressManagerPostelCallbackConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_POSTEL_CALLBACK);
            log.debug(HANDLER_POSTEL_CALLBACK + "- message: {}", message);
            MDC.put("batchId", message.getPayload().getFileKeyInput());
            normalizeAddressService.handlePostelCallback(message.getPayload())
                    .doOnNext(addressOKDto -> log.logEndingProcess(HANDLER_POSTEL_CALLBACK))
                    .doOnError(e -> {
                        log.logEndingProcess(HANDLER_POSTEL_CALLBACK, false, e.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), e);
                    })
                    .block();
        };
    }
}
