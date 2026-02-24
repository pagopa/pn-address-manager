package it.pagopa.pn.address.manager.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.middleware.queue.producer.DeduplicateTracingProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@RequiredArgsConstructor
public class PnAddressManagerMiddlewareConfig {

    private final PnAddressManagerConfig pnAddressManagerConfig;

    @Bean
    public DeduplicateTracingProducer deduplicateProducer(SqsClient sqsClient, ObjectMapper objectMapper) {
        return new DeduplicateTracingProducer(sqsClient, pnAddressManagerConfig.getSqs().getTracingInputQueueName(), objectMapper);
    }
}
