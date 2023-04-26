package it.pagopa.pn.template.config;

import com.amazonaws.services.eventbridge.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class EventBridgeConfiguration {

    @Value("${aws.region}")
    private String awsRegion;

    @Bean
    public AmazonEventBridgeAsync amazonEventBridgeAsync() {
        return AmazonEventBridgeAsyncClientBuilder.standard()
                .withRegion(awsRegion)
                .build();
    }

}