package it.pagopa.pn.address.manager.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsAsyncClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsync;
import com.amazonaws.services.eventbridge.AmazonEventBridgeAsyncClientBuilder;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class EventBridgeConfiguration {

    private final AwsConfigs props;

    @Bean
    public AmazonEventBridgeAsync amazonEventBridgeAsync() {
        return configureBuilder(AmazonEventBridgeAsyncClientBuilder.standard());
    }

    private <C> C configureBuilder(AwsAsyncClientBuilder<?, C> builder) {
        if( props != null ) {

            String profileName = props.getProfileName();
            if( StringUtils.isNotBlank( profileName ) ) {
                AwsCredentials awsCredentials = ProfileCredentialsProvider.create(profileName).resolveCredentials();
                builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsCredentials.accessKeyId(), awsCredentials.accessKeyId())));
            }

            String regionCode = props.getRegionCode();
            String endpointUrl = props.getEndpointUrl();
            if( StringUtils.isNotBlank( regionCode ) && StringUtils.isNotBlank( endpointUrl )) {
                builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(URI.create( endpointUrl ).toString(), props.getRegionCode()));
            }

        }

        return builder.build();
    }

}