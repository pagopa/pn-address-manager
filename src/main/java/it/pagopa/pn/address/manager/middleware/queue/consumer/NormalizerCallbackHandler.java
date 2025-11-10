package it.pagopa.pn.address.manager.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Component
@CustomLog
@RequiredArgsConstructor
public class NormalizerCallbackHandler extends AbstractConsumerMessage  {

    private static final String HANDLER_POSTEL_CALLBACK = "pnAddressManagerPostelCallbackConsumer";

    private final NormalizeAddressService normalizeAddressService;

    @SqsListener(value = "${pn.address-manager.sqs.callback-queue-name}", acknowledgementMode = SqsListenerAcknowledgementMode.ALWAYS)
    public void pnAddressManagerPostelCallbackConsumer(Message<PnPostelCallbackEvent.Payload> message) {
        initTraceId(message.getHeaders());
        log.logStartingProcess(HANDLER_POSTEL_CALLBACK);
        log.debug(HANDLER_POSTEL_CALLBACK + "- message: {}", message);
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getPayload().getRequestId());

        var mono = normalizeAddressService.handlePostelCallback(message.getPayload())
                .doOnSuccess(unused -> log.logEndingProcess(HANDLER_POSTEL_CALLBACK))
                .doOnError(throwable ->  {
                            log.logEndingProcess(HANDLER_POSTEL_CALLBACK, false, throwable.getMessage());
                            HandleEventUtils.handleException(message.getHeaders(), throwable);
                        });

        MDCUtils.addMDCToContextAndExecute(mono).block();
    }

}
