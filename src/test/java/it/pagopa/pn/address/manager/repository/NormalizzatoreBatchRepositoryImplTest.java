package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith (SpringExtension.class)
class NormalizzatoreBatchRepositoryImplTest {

	@MockBean
	private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

	@MockBean
	private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

	private PostelBatchRepositoryImpl postelBatchRepository;


	@BeforeEach
	public void setUp () {
		PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
		dao.setBatchRequestTableName("table");
		PnAddressManagerConfig addressManagerConfig = new PnAddressManagerConfig();
		addressManagerConfig.setDao(dao);
		PnAddressManagerConfig.Postel bR = new PnAddressManagerConfig.Postel();
		bR.setMaxRetry(0);
		bR.setRecoveryAfter(1);
		PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
		normalizer.setPostel(bR);
		addressManagerConfig.setNormalizer(normalizer);
		PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
		batchRequest.setMaxRetry(3);
		when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
		postelBatchRepository = new PostelBatchRepositoryImpl(dynamoDbEnhancedAsyncClient, addressManagerConfig);
	}

	@Test
	void create () {
		NormalizzatoreBatch normalizzatoreBatch = getBatchRequest();
		CompletableFuture<Void> completableFuture = new CompletableFuture<>();
		completableFuture.completeAsync(() -> null);
		when(dynamoDbAsyncTable.putItem((NormalizzatoreBatch) any())).thenReturn(completableFuture);
		StepVerifier.create(postelBatchRepository.create(normalizzatoreBatch)).expectNext(normalizzatoreBatch).verifyComplete();
	}


	@Test
	void update () {
		NormalizzatoreBatch normalizzatoreBatch = getBatchRequest();
		CompletableFuture<Object> completableFuture = new CompletableFuture<>();
		completableFuture.completeAsync(() -> normalizzatoreBatch);
		when(dynamoDbAsyncTable.updateItem((PnRequest) any())).thenReturn(completableFuture);
		StepVerifier.create(postelBatchRepository.update(normalizzatoreBatch)).expectNext(normalizzatoreBatch).verifyComplete();
	}
	@Test
	void findByBatchIdTest (){
		NormalizzatoreBatch normalizzatoreBatch = getBatchRequest();
		CompletableFuture<Object> completableFuture = new CompletableFuture<>();
		completableFuture.completeAsync(() -> normalizzatoreBatch);
		when(dynamoDbAsyncTable.getItem((Consumer<GetItemEnhancedRequest.Builder>) any()))
				.thenReturn(completableFuture);
		StepVerifier.create(postelBatchRepository.findByBatchId("batchId"))
                .expectNext(normalizzatoreBatch)
                .verifyComplete();
	}
	@Test
	void deleteItemTest(){
		NormalizzatoreBatch normalizzatoreBatch = getBatchRequest();
		CompletableFuture<Object> completableFuture = new CompletableFuture<>();
		completableFuture.completeAsync(() -> normalizzatoreBatch);
		when(dynamoDbAsyncTable.deleteItem((Key) any()))
				.thenReturn(completableFuture);
		StepVerifier.create(postelBatchRepository.deleteItem("batchId"))
				.expectNext(normalizzatoreBatch)
				.verifyComplete();
	}
	@Test
	void resetPostelBatchForRecovery () {
		NormalizzatoreBatch normalizzatoreBatch = getBatchRequest();
		when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest<Object>) any()))
				.thenReturn(CompletableFuture.completedFuture(normalizzatoreBatch));
		StepVerifier.create(postelBatchRepository.resetPostelBatchForRecovery(normalizzatoreBatch)).expectNextCount(1);
	}

	@Test
	void getBatchRequestToRecovery () {
		NormalizzatoreBatch batchRequest = getBatchRequest();
		PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
		PnAddressManagerConfig.BatchRequest batchRequest1 = new PnAddressManagerConfig.BatchRequest();
		batchRequest1.setMaxRetry(3);
		batchRequest1.setRecoveryAfter(1);
		normalizer.setBatchRequest(batchRequest1);
		PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
		PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
		dao.setPostelBatchTableName("tabelName");
		pnAddressManagerConfig.setDao(dao);
		pnAddressManagerConfig.setNormalizer(normalizer);
		DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
		when(dynamoDbAsyncTable.index(any()))
				.thenReturn(index);
		when(index.query((QueryEnhancedRequest) any()))
				.thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchRequest)))));
		PostelBatchRepository batchRepository = new PostelBatchRepositoryImpl(dynamoDbEnhancedAsyncClient, pnAddressManagerConfig);
		Assertions.assertNotNull(batchRepository);
	}

	@Test
	void testGetBatchRequestToRecovery () {
		NormalizzatoreBatch batchRequest = getBatchRequest();
		PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
		postel.setMaxRetry(3);
		postel.setRecoveryAfter(1);
		PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
		PnAddressManagerConfig.BatchRequest batchRequest1 = new PnAddressManagerConfig.BatchRequest();
		batchRequest1.setMaxRetry(3);
		batchRequest1.setRecoveryAfter(1);
		normalizer.setBatchRequest(batchRequest1);
		PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
		PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
		dao.setPostelBatchTableName("tabelName");
		pnAddressManagerConfig.setDao(dao);
		pnAddressManagerConfig.setNormalizer(normalizer);
		pnAddressManagerConfig.getNormalizer().setPostel(postel);
		pnAddressManagerConfig.getNormalizer().getPostel().setMaxRetry(3);
		when(dynamoDbEnhancedAsyncClient.table(any(), any()))
				.thenReturn(dynamoDbAsyncTable);
		PostelBatchRepository batchRequestRepository = new PostelBatchRepositoryImpl(dynamoDbEnhancedAsyncClient, pnAddressManagerConfig);

		DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
		when(dynamoDbAsyncTable.index(any()))
				.thenReturn(index);
		when(index.query((QueryEnhancedRequest) any()))
				.thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchRequest)))));

		StepVerifier.create(batchRequestRepository.getPostelBatchToRecover())
				.expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void testGetPostelBatchToClean () {
		NormalizzatoreBatch batchRequest = getBatchRequest();
		PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
		postel.setMaxRetry(3);
		postel.setRecoveryAfter(1);
		PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
		PnAddressManagerConfig.BatchRequest batchRequest1 = new PnAddressManagerConfig.BatchRequest();
		batchRequest1.setMaxRetry(3);
		batchRequest1.setRecoveryAfter(1);
		normalizer.setBatchRequest(batchRequest1);
		PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
		PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
		dao.setPostelBatchTableName("tabelName");
		pnAddressManagerConfig.setDao(dao);
		pnAddressManagerConfig.setNormalizer(normalizer);
		pnAddressManagerConfig.getNormalizer().setPostel(postel);
		pnAddressManagerConfig.getNormalizer().getPostel().setMaxRetry(3);
		// Mocking current time
		LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
		Clock clock = Clock.fixed(currentTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

		Key expectedKey = Key.builder()
				.partitionValue(BatchStatus.WORKING.getValue())
				.sortValue(AttributeValue.builder()
						.s(currentTime.toString())
						.build())
				.build();

		QueryConditional expectedQueryConditional = QueryConditional.sortLessThan(expectedKey);
		QueryEnhancedRequest expectedQueryEnhancedRequest = QueryEnhancedRequest.builder()
				.queryConditional(expectedQueryConditional)
				.build();
		when(dynamoDbEnhancedAsyncClient.table(any(), any()))
				.thenReturn(dynamoDbAsyncTable);
		PostelBatchRepository batchRequestRepository = new PostelBatchRepositoryImpl(dynamoDbEnhancedAsyncClient, pnAddressManagerConfig);

		DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
		when(dynamoDbAsyncTable.index(any()))
				.thenReturn(index);
		when(index.query((QueryEnhancedRequest) any()))
				.thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchRequest)))));

		// Invoking the method
		Mono<Page<NormalizzatoreBatch>> resultMono = postelBatchRepository.getPostelBatchToClean();
		StepVerifier.create(resultMono)
				.expectNextCount(1)
				.verifyComplete();
	}

	NormalizzatoreBatch getBatchRequest () {
		NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
		normalizzatoreBatch.setBatchId("batchId");
		normalizzatoreBatch.setRetry(0);
		normalizzatoreBatch.setFileKey("fileKey");
		return normalizzatoreBatch;
	}

	Key keyBuilder (String key) {
		return Key.builder().partitionValue(key).build();
	}
}
