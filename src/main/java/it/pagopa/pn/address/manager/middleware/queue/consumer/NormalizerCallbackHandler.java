package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@CustomLog
@RequiredArgsConstructor
public class NormalizerCallbackHandler {

    private static final String HANDLER_POSTEL_CALLBACK = "pnAddressManagerPostelCallbackConsumer";

    private final NormalizeAddressService normalizeAddressService;

    @Bean
    public Consumer<Message<PnPostelCallbackEvent.Payload>> pnAddressManagerPostelCallbackConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_POSTEL_CALLBACK);
            log.debug(HANDLER_POSTEL_CALLBACK + "- message: {}", message);
            MDC.put("batchId", message.getPayload().getRequestId());

            var mono = normalizeAddressService.handlePostelCallback(message.getPayload())
                    .doOnSuccess(unused -> log.logEndingProcess(HANDLER_POSTEL_CALLBACK))
                    .doOnError(throwable ->  {
                                log.logEndingProcess(HANDLER_POSTEL_CALLBACK, false, throwable.getMessage());
                                HandleEventUtils.handleException(message.getHeaders(), throwable);
                            });

            MDCUtils.addMDCToContextAndExecute(mono).block();
        };
    }

}
