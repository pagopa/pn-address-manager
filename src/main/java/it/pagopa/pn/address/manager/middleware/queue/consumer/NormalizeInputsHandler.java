package it.pagopa.pn.address.manager.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
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
public class NormalizeInputsHandler extends AbstractConsumerMessage {

    private final NormalizeAddressService normalizeAddressService;

    private static final String HANDLER_REQUEST = "pnAddressManagerRequestConsumer";

    @SqsListener(value = "${pn.address-manager.sqs.input-queue-name}", acknowledgementMode = SqsListenerAcknowledgementMode.ALWAYS)
    public void pnAddressManagerRequestConsumer(Message<PnNormalizeRequestEvent.Payload> message) {
        initTraceId(message.getHeaders());
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
    }

}
