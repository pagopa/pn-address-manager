package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
class AddressPnRequestRepositoryImplTest {

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
        PnRequest pnRequest = getBatchRequest();
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> pnRequest);
        when(dynamoDbAsyncTable.updateItem((PnRequest) any())).thenReturn(completableFuture);
        StepVerifier.create(addressBatchRequestRepository.update(pnRequest)).expectNext(pnRequest).verifyComplete();
    }

    @Test
    void create(){
        PnRequest pnRequest = getBatchRequest();
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> null);
        when(dynamoDbAsyncTable.putItem((PnRequest) any())).thenReturn(completableFuture);
        StepVerifier.create(addressBatchRequestRepository.create(pnRequest)).expectNext(pnRequest).verifyComplete();
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
        PnRequest pnRequest = getBatchRequest();
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(pnRequest)))));
        StepVerifier.create(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus("batchId", BatchStatus.NO_BATCH_ID)).expectNextCount(0);
    }

    @Test
    void setNewBatchIdToBatchRequest(){
        PnRequest pnRequest = getBatchRequest();
        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest<Object>) any()))
                .thenReturn(CompletableFuture.completedFuture(pnRequest));
        StepVerifier.create(addressBatchRequestRepository.setNewBatchIdToBatchRequest(pnRequest)).expectNextCount(1);
    }

    @Test
    void setNewReservationIdToBatchRequest(){
        PnRequest pnRequest = getBatchRequest();
        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest<Object>) any()))
                .thenReturn(CompletableFuture.completedFuture(pnRequest));
        StepVerifier.create(addressBatchRequestRepository.setNewReservationIdToBatchRequest(pnRequest)).expectNextCount(1);
    }

    @Test
    void resetBatchRequestForRecovery(){
        PnRequest pnRequest = getBatchRequest();
        when(dynamoDbAsyncTable.updateItem((UpdateItemEnhancedRequest<Object>) any()))
                .thenReturn(CompletableFuture.completedFuture(pnRequest));
        StepVerifier.create(addressBatchRequestRepository.resetBatchRequestForRecovery(pnRequest)).expectNextCount(1);
    }

    @Test
    void getBatchRequestToRecovery(){
        PnRequest pnRequest = getBatchRequest();
        DynamoDbAsyncIndex<Object> index = mock(DynamoDbAsyncIndex.class);
        when(dynamoDbAsyncTable.index(any()))
                .thenReturn(index);
        when(index.query((QueryEnhancedRequest) any()))
                .thenReturn(SdkPublisher.adapt(Mono.just(Page.create(List.of(pnRequest)))));
        StepVerifier.create(addressBatchRequestRepository.getBatchRequestToRecovery()).expectNextCount(0);
    }

    PnRequest getBatchRequest(){
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("yourCorrelationId");
        pnRequest.setAddresses("yourAddresses");
        pnRequest.setBatchId("yourBatchId");
        pnRequest.setRetry(1);
        pnRequest.setTtl(3600L); // Your TTL value in seconds
        pnRequest.setClientId("yourClientId");
        pnRequest.setStatus("yourStatus");
        pnRequest.setLastReserved(LocalDateTime.now()); // Your LocalDateTime value
        pnRequest.setCreatedAt(LocalDateTime.now()); // Your LocalDateTime value
        pnRequest.setSendStatus("yourSendStatus");
        pnRequest.setMessage("yourMessage");
        pnRequest.setXApiKey("yourXApiKey");
        pnRequest.setCxId("yourCxId");
        pnRequest.setAwsMessageId("yourAwsMessageId");
        return pnRequest;
    }
}
