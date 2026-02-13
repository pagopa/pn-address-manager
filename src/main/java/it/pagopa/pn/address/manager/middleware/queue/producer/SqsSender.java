package it.pagopa.pn.address.manager.middleware.queue.producer;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
import it.pagopa.pn.address.manager.middleware.queue.model.DeduplicateTracingEvent;
import it.pagopa.pn.address.manager.middleware.queue.model.DeduplicateEventType;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static it.pagopa.pn.address.manager.middleware.queue.model.DeduplicateEventType.DEDUPLICATE_REQUEST;
import static it.pagopa.pn.address.manager.middleware.queue.model.DeduplicateEventType.DEDUPLICATE_RESPONSE;

@Component
@Slf4j
@RequiredArgsConstructor
public class SqsSender {

    private static final String PUBLISHER = "pn-address-manager";
    private final DeduplicateTracingProducer deduplicateTracingProducer;

    private <T> void pushEvent(T data, DeduplicateEventType eventType, String correlationId) {

        GenericEventHeader header = GenericEventHeader.builder()
                .eventId(correlationId)
                .publisher(PUBLISHER)
                .createdAt(Instant.now())
                .build();

        DeduplicateTracingEvent.Payload<T> payload =
                DeduplicateTracingEvent.Payload.<T>builder()
                        .eventType(eventType.name())
                        .data(data)
                        .build();

        DeduplicateTracingEvent event = new DeduplicateTracingEvent(header, payload);
        deduplicateTracingProducer.push(event);
    }

    public void pushDeduplicaRequestEvent(DeduplicaRequest request, String correlationId) {
        pushEvent(request, DEDUPLICATE_REQUEST, correlationId);
    }

    public void pushDeduplicaResponseEvent(DeduplicaResponse response, String correlationId){
        pushEvent(response, DEDUPLICATE_RESPONSE, correlationId);

    }
}
