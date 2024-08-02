package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_REQUEST_ID;

@Configuration
@CustomLog
@RequiredArgsConstructor
public class NormalizeInputsHandler {

    private final NormalizeAddressService normalizeAddressService;

    private static final String HANDLER_REQUEST = "pnAddressManagerRequestConsumer";

    @Bean
    public Consumer<Message<PnNormalizeRequestEvent.Payload>> pnAddressManagerRequestConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_REQUEST);
            log.debug(HANDLER_REQUEST + "- message: {}", message);
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getPayload().getNormalizeItemsRequest().getCorrelationId());
            var mono = normalizeAddressService.handleRequest(message.getPayload())
                    .doOnSuccess(unused -> log.logEndingProcess(HANDLER_REQUEST))
                    .doOnError(throwable ->  {
                        log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), throwable);
                    });

            MDCUtils.addMDCToContextAndExecute(mono).block();
        };
    }


}
