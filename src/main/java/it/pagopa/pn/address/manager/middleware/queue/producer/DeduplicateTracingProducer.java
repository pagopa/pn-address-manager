package it.pagopa.pn.address.manager.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.middleware.queue.model.DeduplicateTracingEvent;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DeduplicateTracingProducer extends AbstractSqsMomProducer<DeduplicateTracingEvent> {

    public DeduplicateTracingProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper) {
        super(sqsClient, topic, objectMapper, DeduplicateTracingEvent.class);
    }
}
