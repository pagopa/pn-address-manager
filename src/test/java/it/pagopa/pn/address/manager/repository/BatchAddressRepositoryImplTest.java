package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.BatchAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchAddressRepositoryImplTest {

    @Mock
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Mock
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    @Test
    void testUpdate() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        BatchAddressRepository batchAddressRepository = new BatchAddressRepositoryImpl(dynamoDbEnhancedAsyncClient);

        BatchAddress batchAddress = new BatchAddress();

        when(dynamoDbAsyncTable.updateItem(same(batchAddress)))
                .thenReturn(CompletableFuture.completedFuture(batchAddress));

        StepVerifier.create(batchAddressRepository.update(batchAddress))
                .expectNext(batchAddress)
                .verifyComplete();
    }

    @Test
    void testCreate() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        BatchAddressRepository batchAddressRepository = new BatchAddressRepositoryImpl(dynamoDbEnhancedAsyncClient);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        BatchAddress batchAddress = new BatchAddress();
        when(dynamoDbAsyncTable.putItem(batchAddress))
                .thenReturn(completableFuture);

        StepVerifier.create(batchAddressRepository.create(batchAddress))
                .expectNext(batchAddress)
                .verifyComplete();
    }

    @Test
    void testGetBatchAddressByNotBatchId() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        BatchAddressRepository batchAddressRepository = new BatchAddressRepositoryImpl(dynamoDbEnhancedAsyncClient);

        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("chiave", AttributeValue.builder().s("valore").build());

        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(sdkPublisher);

        StepVerifier.create(batchAddressRepository.getBatchAddressByNotBatchId(lastKey, 100))
                .expectNextCount(0);
    }


    @Test
    void testSetNewBatchIdToBatchRequests() {
        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        BatchAddressRepository batchAddressRepository = new BatchAddressRepositoryImpl(dynamoDbEnhancedAsyncClient);

        BatchAddress batchRequest = new BatchAddress();

        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));

        StepVerifier.create(batchAddressRepository.setNewBatchIdToBatchAddress(batchRequest))
                .expectNext(batchRequest)
                .verifyComplete();
    }

}
