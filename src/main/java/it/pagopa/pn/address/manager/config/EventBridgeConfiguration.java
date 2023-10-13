package it.pagopa.pn.address.manager.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsyncClientBuilder;
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
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(awsRegion)
                .build();
    }

}