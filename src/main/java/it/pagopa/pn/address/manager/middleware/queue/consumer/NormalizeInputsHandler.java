package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.scheduler.Scheduler;

import java.util.function.Consumer;

@Configuration
@CustomLog
public class NormalizeInputsHandler {

    private final NormalizeAddressService normalizeAddressService;

    private static final String HANDLER_REQUEST = "pnAddressManagerRequestConsumer";
    private static final String HANDLER_POSTEL_CALLBACK = "pnAddressManagerPostelCallbackConsumer";

    @Qualifier("addressManagerScheduler")
    private final Scheduler scheduler;

    public NormalizeInputsHandler(NormalizeAddressService normalizeAddressService, Scheduler scheduler) {
        this.normalizeAddressService = normalizeAddressService;
        this.scheduler = scheduler;
    }

    @Bean
    public Consumer<Message<PnNormalizeRequestEvent.Payload>> pnAddressManagerRequestConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_REQUEST);
            log.debug(HANDLER_REQUEST + "- message: {}", message);
            MDC.put("correlationId", message.getPayload().getNormalizeItemsRequest().getCorrelationId());
            normalizeAddressService.handleRequest(message.getPayload())
                    .subscribeOn(scheduler).subscribe(normalizeItemsResult -> log.logEndingProcess(HANDLER_REQUEST),
                            throwable -> {
                                log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                                HandleEventUtils.handleException(message.getHeaders(), throwable);
                            });
        };
    }

    @Bean
    public Consumer<Message<PnPostelCallbackEvent.Payload>> pnAddressManagerPostelCallbackConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_POSTEL_CALLBACK);
            log.debug(HANDLER_POSTEL_CALLBACK + "- message: {}", message);
            MDC.put("batchId", message.getPayload().getRequestId());

            normalizeAddressService.handlePostelCallback(message.getPayload())
                    .subscribeOn(scheduler).subscribe(normalizeItemsResult -> log.logEndingProcess(HANDLER_POSTEL_CALLBACK),
                                throwable -> {
                                    log.logEndingProcess(HANDLER_POSTEL_CALLBACK, false, throwable.getMessage());
                                    HandleEventUtils.handleException(message.getHeaders(), throwable);
                                });
                    };
    }
}
