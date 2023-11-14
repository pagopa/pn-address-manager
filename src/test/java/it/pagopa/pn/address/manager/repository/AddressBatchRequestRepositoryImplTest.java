package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
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
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
class AddressBatchRequestRepositoryImplTest {

    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @MockBean
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    private AddressBatchRequestRepositoryImpl addressBatchRequestRepository;


    @BeforeEach
    public void setUp() {
        PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
        dao.setBatchRequestTableName("table");
        PnAddressManagerConfig addressManagerConfig = new PnAddressManagerConfig();
        addressManagerConfig.setDao(dao);
        PnAddressManagerConfig.BatchRequest bR = new PnAddressManagerConfig.BatchRequest();
        bR.setMaxRetry(0);
        bR.setRecoveryAfter(1);
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(bR);
        addressManagerConfig.setNormalizer(normalizer);
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        addressBatchRequestRepository = new AddressBatchRequestRepositoryImpl(dynamoDbEnhancedAsyncClient, addressManagerConfig);
    }

    @Test
    void update(){
        BatchRequest batchRequest = getBatchRequest();
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> batchRequest);
        when(dynamoDbAsyncTable.updateItem((BatchRequest) any())).thenReturn(completableFuture);
        StepVerifier.create(addressBatchRequestRepository.update(batchRequest)).expectNext(batchRequest).verifyComplete();
    }

    @Test
    void create(){
        BatchRequest batchRequest = getBatchRequest();
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        when(dynamoDbAsyncTable.putItem((BatchRequest) any())).thenReturn(completableFuture);
        StepVerifier.create(addressBatchRequestRepository.create(batchRequest)).expectNext(batchRequest).verifyComplete();
    }

    @Test
    void getBatchRequestByNotBatchId(){
        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("key", AttributeValue.builder().s("key").build());
        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(index.query((QueryEnhancedRequest) any())).thenReturn(sdkPublisher);
        when(dynamoDbAsyncTable.index(any())).thenReturn(index);
        StepVerifier.create(addressBatchRequestRepository.getBatchRequestByNotBatchId(lastKey,0)).expectNextCount(0);
    }

    @Test
    void getBatchRequestToSend(){
        Map<String, AttributeValue> lastKey = new HashMap<>();
        lastKey.put("key", AttributeValue.builder().s("key").build());
        SdkPublisher<Page<Object>> sdkPublisher = mock(SdkPublisher.class);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(index.query((QueryEnhancedRequest) any())).thenReturn(sdkPublisher);
        when(dynamoDbAsyncTable.index(any())).thenReturn(index);
        StepVerifier.create(addressBatchRequestRepository.getBatchRequestToSend(lastKey,0)).expectNextCount(0);
    }

    @Test
    void getBatchRequestByBatchIdAndStatus(){
        BatchRequest batchRequest = getBatchRequest();
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchRequest)))));
        StepVerifier.create(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.NO_BATCH_ID)).expectNextCount(0);
    }
    @Test
    void testGetBatchRequestByBatchId() {
        Map<String, AttributeValue> lastKey = mock(Map.class);

        when(dynamoDbEnhancedAsyncClient.table(any(), any()))
                .thenReturn(dynamoDbAsyncTable);
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.empty()));


        StepVerifier.create(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(lastKey,"batchId", BatchStatus.NO_BATCH_ID))
                .expectNextCount(0)
                .verifyComplete();
    }


    @Test
    void setNewBatchIdToBatchRequest(){
        BatchRequest batchRequest = getBatchRequest();
        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest<Object>) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));
        StepVerifier.create(addressBatchRequestRepository.setNewBatchIdToBatchRequest(batchRequest)).expectNextCount(1);
    }

    @Test
    void setNewReservationIdToBatchRequest(){
        BatchRequest batchRequest = getBatchRequest();
        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest<Object>) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));
        StepVerifier.create(addressBatchRequestRepository.setNewReservationIdToBatchRequest(batchRequest)).expectNextCount(1);
    }

    @Test
    void resetBatchRequestForRecovery(){
        BatchRequest batchRequest = getBatchRequest();
        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest<Object>) any()))
                .thenReturn(CompletableFuture.completedFuture(batchRequest));
        StepVerifier.create(addressBatchRequestRepository.resetBatchRequestForRecovery(batchRequest)).expectNextCount(1);
    }

    @Test
    void getBatchRequestToRecovery(){
        BatchRequest batchRequest = getBatchRequest();
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(batchRequest)))));
        StepVerifier.create(addressBatchRequestRepository.getBatchRequestToRecovery()).expectNextCount(0);
    }

    BatchRequest getBatchRequest(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("yourCorrelationId");
        batchRequest.setAddresses("yourAddresses");
        batchRequest.setBatchId("yourBatchId");
        batchRequest.setRetry(1);
        batchRequest.setTtl(3600L); // Your TTL value in seconds
        batchRequest.setClientId("yourClientId");
        batchRequest.setStatus("yourStatus");
        batchRequest.setLastReserved(LocalDateTime.now()); // Your LocalDateTime value
        batchRequest.setCreatedAt(LocalDateTime.now()); // Your LocalDateTime value
        batchRequest.setSendStatus("yourSendStatus");
        batchRequest.setMessage("yourMessage");
        batchRequest.setXApiKey("yourXApiKey");
        batchRequest.setCxId("yourCxId");
        batchRequest.setAwsMessageId("yourAwsMessageId");
        return batchRequest;
    }
}
