package it.pagopa.pn.address.manager.middleware.queue.producer;

import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaRequest;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
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

        sqsSender.pushDeduplicaRequestEvent(request);

        ArgumentCaptor<DeduplicateTracingEvent> captor = ArgumentCaptor.forClass(DeduplicateTracingEvent.class);
        verify(deduplicateTracingProducer).push(captor.capture());
        DeduplicateTracingEvent event = captor.getValue();

        assertEquals("pn-address-manager", event.getHeader().getPublisher());
        assertNotNull(event.getHeader().getCreatedAt());
        assertEquals(DeduplicateEventType.DEDUPLICATE_REQUEST.name(), event.getPayload().getEventType());
        assertEquals(request, event.getPayload().getData());
    }

    @Test
    void testPushDeduplicaResponseEvent() {
        DeduplicaResponse response = mock(DeduplicaResponse.class);

        sqsSender.pushDeduplicaResponseEvent(response);

        ArgumentCaptor<DeduplicateTracingEvent> captor = ArgumentCaptor.forClass(DeduplicateTracingEvent.class);
        verify(deduplicateTracingProducer).push(captor.capture());
        DeduplicateTracingEvent event = captor.getValue();

        assertEquals("pn-address-manager", event.getHeader().getPublisher());
        assertNotNull(event.getHeader().getCreatedAt());
        assertEquals(DeduplicateEventType.DEDUPLICATE_RESPONSE.name(), event.getPayload().getEventType());
        assertEquals(response, event.getPayload().getData());
    }
}