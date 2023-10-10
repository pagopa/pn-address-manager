package it.pagopa.pn.address.manager.middleware.queue.consumer;

import org.springframework.messaging.MessageHeaders;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.ADDRESS_NORMALIZER_ASYNC;

@lombok.CustomLog
public class HandleEventUtils {
    private HandleEventUtils() {
    }

    public static void handleException(MessageHeaders headers, Throwable t) {
        if (headers != null) {
            log.error(ADDRESS_NORMALIZER_ASYNC + "Generic exception for correlationId={} ex={}", headers.get("correlationId"), t.getCause());
        } else {
            log.error(ADDRESS_NORMALIZER_ASYNC + "Generic exception ex ", t);
        }
    }


}
