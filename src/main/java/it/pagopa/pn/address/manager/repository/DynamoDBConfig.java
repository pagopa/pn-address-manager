package it.pagopa.pn.address.manager.repository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@Configuration
public class DynamoDBConfig {
	@Bean
	public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient () {
		return DynamoDbEnhancedAsyncClient.builder().build();
	}
}
