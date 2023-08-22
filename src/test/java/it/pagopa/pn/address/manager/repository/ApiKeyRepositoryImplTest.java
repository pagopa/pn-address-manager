package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith (SpringExtension.class)
@ContextConfiguration (classes = {ApiKeyRepositoryImpl.class,ApiKeyRepository.class,DynamoDBConfig.class})

class ApiKeyRepositoryImplTest {
	@MockBean
	private ApiKeyRepositoryImpl apiKeyRepositoryImpl;
	@Autowired
	private ApiKeyRepository apiKeyRepository;
	@Mock
	private DynamoDbAsyncTable<Object> table;
	@Mock
	private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

	@Test
	void testFindById() {
		String id = "id";
		ApiKeyModel apiKeyModel = new ApiKeyModel();
		apiKeyModel.setApiKeyId(id);

		when(dynamoDbEnhancedAsyncClient.table(any(), any()))
				.thenReturn(table);
		ApiKeyRepositoryImpl apiKeyRepository = new ApiKeyRepositoryImpl(dynamoDbEnhancedAsyncClient, "");
		when(table.getItem(any(Key.class)))
				.thenReturn(CompletableFuture.completedFuture(apiKeyModel));
		StepVerifier.create(apiKeyRepository.findById(id))
				.expectNextCount(1)
				.verifyComplete();
	}
}

