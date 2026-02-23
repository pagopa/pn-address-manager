package it.pagopa.pn.address.manager.middleware.queue.producer;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
import it.pagopa.pn.address.manager.middleware.queue.model.DeduplicateEventType;
import it.pagopa.pn.address.manager.middleware.queue.model.DeduplicateTracingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SqsSenderTest {

    @Mock
    private DeduplicateTracingProducer deduplicateTracingProducer;

    private SqsSender sqsSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sqsSender = new SqsSender(deduplicateTracingProducer);
    }

    @Test
    void testPushDeduplicaRequestEvent() {
        DeduplicaRequest request = mock(DeduplicaRequest.class);
        String correlationId = "corr-id-req";

        sqsSender.pushDeduplicaRequestEvent(request, correlationId);

        ArgumentCaptor<DeduplicateTracingEvent> captor = ArgumentCaptor.forClass(DeduplicateTracingEvent.class);
        verify(deduplicateTracingProducer).push(captor.capture());
        DeduplicateTracingEvent event = captor.getValue();

        assertEquals(correlationId, event.getHeader().getEventId());
        assertEquals("pn-address-manager", event.getHeader().getPublisher());
        assertNotNull(event.getHeader().getCreatedAt());
        assertEquals(DeduplicateEventType.DEDUPLICATE_REQUEST.name(), event.getPayload().getEventType());
        assertEquals(request, event.getPayload().getData());
    }

    @Test
    void testPushDeduplicaResponseEvent() {
        DeduplicaResponse response = mock(DeduplicaResponse.class);
        String correlationId = "corr-id-res";

        sqsSender.pushDeduplicaResponseEvent(response, correlationId);

        ArgumentCaptor<DeduplicateTracingEvent> captor = ArgumentCaptor.forClass(DeduplicateTracingEvent.class);
        verify(deduplicateTracingProducer).push(captor.capture());
        DeduplicateTracingEvent event = captor.getValue();

        assertEquals(correlationId, event.getHeader().getEventId());
        assertEquals("pn-address-manager", event.getHeader().getPublisher());
        assertNotNull(event.getHeader().getCreatedAt());
        assertEquals(DeduplicateEventType.DEDUPLICATE_RESPONSE.name(), event.getPayload().getEventType());
        assertEquals(response, event.getPayload().getData());
    }
}