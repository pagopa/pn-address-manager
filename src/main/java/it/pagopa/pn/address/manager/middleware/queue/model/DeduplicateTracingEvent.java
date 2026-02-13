package it.pagopa.pn.address.manager.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeduplicateTracingEvent implements GenericEvent<GenericEventHeader, DeduplicateTracingEvent.Payload<?>> {

    private GenericEventHeader header;
    private Payload<?> payload;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder
    @EqualsAndHashCode
    @ToString
    public static class Payload<T> {
        private String eventType;
        private T data;
    }
}