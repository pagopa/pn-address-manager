package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.CAPModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@ContextConfiguration (classes = {CapRepository.class,CapRepositoryImpl.class,DynamoDBConfig.class})
class CapRepositoryImplTest {
	@Autowired
	private CapRepositoryImpl capRepositoryImpl;
	@Autowired
	private CapRepository capRepository;
	@Mock
	private DynamoDbAsyncTable<Object> table;
	@Mock
	private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
	@Test
	void testFindByCap () {
		String id = "id";
		CAPModel capModel = new CAPModel();
		capModel.setCap(id);

		when(dynamoDbEnhancedAsyncClient.table(any(), any()))
				.thenReturn(table);
		CapRepositoryImpl capRepository = new CapRepositoryImpl(dynamoDbEnhancedAsyncClient, "");
		when(table.getItem(any(Key.class)))
				.thenReturn(CompletableFuture.completedFuture(capModel));
		StepVerifier.create(capRepository.findByCap(id))
				.expectNextCount(1)
				.verifyComplete();
	}
}

