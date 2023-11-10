package it.pagopa.pn.address.manager.service;

import com.amazonaws.services.eventbridge.model.PutEventsResult;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class RecoveryServiceTest {
    RecoveryService recoveryService;

    @MockBean AddressBatchRequestRepository addressBatchRequestRepository;
    @MockBean AddressBatchRequestService addressBatchRequestService;
    @MockBean SqsService sqsService;
    @MockBean EventService eventService;
    @MockBean PnAddressManagerConfig pnAddressManagerConfig;

    @MockBean PostelBatchRepository postelBatchRepository;


    @MockBean
    AddressUtils addressUtils;

    @Test
    void recoveryBatchRequest(){
        recoveryService = new RecoveryService(addressBatchRequestRepository, addressBatchRequestService, sqsService, eventService, pnAddressManagerConfig, addressUtils, postelBatchRepository);

        BatchRequest batchRequest = getBatchRequest();
        when( addressBatchRequestRepository.getBatchRequestToRecovery()).thenReturn(Mono.just(List.of(batchRequest)));
        when(addressBatchRequestRepository.resetBatchRequestForRecovery(any())).thenReturn(Mono.just(batchRequest));

        Assertions.assertDoesNotThrow(() -> recoveryService.recoveryBatchRequest());
    }

    @Test
    void recoveryPostelActivation(){
        recoveryService = new RecoveryService(addressBatchRequestRepository, addressBatchRequestService, sqsService, eventService, pnAddressManagerConfig, addressUtils, postelBatchRepository);

        PostelBatch postelBatch = new PostelBatch();
        postelBatch.setBatchId("id");
        when(postelBatchRepository.getPostelBatchToRecover()).thenReturn(Mono.just(List.of(postelBatch)));
        when(postelBatchRepository.resetPostelBatchForRecovery(any())).thenReturn(Mono.just(postelBatch));
        when(addressBatchRequestService.callPostelActivationApi(any())).thenReturn(Mono.empty());

        Assertions.assertDoesNotThrow(() -> recoveryService.recoveryPostelActivation());
    }

    @Test
    void recoveryBatchSendToEventbridge(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setQueryMaxSize(100);
        batchRequest.setLockAtMost(100);
        batchRequest.setLockAtLeast(100);
        batchRequest.setRecoveryAfter(3);
        batchRequest.setMaxRetry(3);
        batchRequest.setRecoveryDelay(3);
        batchRequest.setDelay(3);
        batchRequest.setEventBridgeRecoveryDelay(3);
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer();
        normalizer.setMaxCsvSize(100);
        normalizer.setBatchRequest(batchRequest);
        pnAddressManagerConfig.setNormalizer(normalizer);
        recoveryService = new RecoveryService(addressBatchRequestRepository, addressBatchRequestService, sqsService, eventService, pnAddressManagerConfig, addressUtils, postelBatchRepository);

        BatchRequest batchRequest1 = getBatchRequest();
        BatchRequest batchRequest2 = getBatchRequest();
        Page<BatchRequest> page1 = Page.create(List.of(batchRequest1), Map.of("key", AttributeValue.builder().s("value").build()));
        Page<BatchRequest> page2 = Page.create(List.of(batchRequest2));
        when(addressBatchRequestRepository.getBatchRequestToSend(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);
        when(addressBatchRequestRepository.resetBatchRequestForRecovery(any())).thenReturn(Mono.just(batchRequest1));
        when(addressBatchRequestRepository.resetBatchRequestForRecovery(any())).thenReturn(Mono.just(batchRequest2));

        when(addressBatchRequestRepository.setNewReservationIdToBatchRequest(any())).thenReturn(Mono.just(batchRequest1));
        when(addressBatchRequestRepository.setNewReservationIdToBatchRequest(any())).thenReturn(Mono.just(batchRequest2));


        when(sqsService.sendToDlqQueue(any())).thenReturn(Mono.empty());
        PutEventsResult putEventsResult = new PutEventsResult();
        when(eventService.sendEvent(anyString())).thenReturn(Mono.just(putEventsResult));
        Assertions.assertDoesNotThrow(() -> recoveryService.recoveryBatchSendToEventbridge());
    }
    @Test
    void testCleanStoppedRequest () {
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setQueryMaxSize(100);
        batchRequest.setLockAtMost(100);
        batchRequest.setLockAtLeast(100);
        batchRequest.setRecoveryAfter(3);
        batchRequest.setMaxRetry(3);
        batchRequest.setRecoveryDelay(3);
        batchRequest.setDelay(3);
        batchRequest.setEventBridgeRecoveryDelay(3);
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer();
        normalizer.setMaxCsvSize(100);
        normalizer.setBatchRequest(batchRequest);
        pnAddressManagerConfig.setNormalizer(normalizer);
        recoveryService = new RecoveryService(addressBatchRequestRepository,
                addressBatchRequestService, sqsService, eventService, pnAddressManagerConfig, addressUtils, postelBatchRepository);
        BatchRequest batchRequest1= getBatchRequest();
        PostelBatch postelBatch1 = new PostelBatch();
        postelBatch1.setBatchId("id1");
        PostelBatch postelBatch2 = new PostelBatch();
        postelBatch2.setBatchId("id2");
        Page<PostelBatch> page1 = Page.create(List.of(postelBatch1), Map.of("key", AttributeValue.builder().s("value").build()));
        Page<PostelBatch> page2 = Page.create(List.of(postelBatch2));
        when(postelBatchRepository.getPostelBatchToClean())
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(anyString(), any()))
                .thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestService.incrementAndCheckRetry(any(),any(),anyString()))
                .thenReturn(Mono.empty());
        when(addressBatchRequestRepository.update(any()))
                .thenReturn(Mono.just(batchRequest1));
        when(postelBatchRepository.deleteItem(anyString()))
                .thenReturn(Mono.empty());
        when(addressBatchRequestRepository.update(any()))
                .thenReturn(Mono.just(batchRequest1));
        when(postelBatchRepository.deleteItem(anyString()))
                .thenReturn(Mono.empty());
        Assertions.assertDoesNotThrow(() -> recoveryService.cleanStoppedRequest());

    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer() {
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setRecoveryAfter(3);
        batchRequest.setMaxRetry(3);
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setMaxRetry(3);
        postel.setRecoveryAfter(3);
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        return normalizer;
    }

    BatchRequest getBatchRequest(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("yourCorrelationId");
        batchRequest.setAddresses("yourAddresses");
        batchRequest.setBatchId("NO_BATCH_ID");
        batchRequest.setRetry(1);
        batchRequest.setTtl(3600L); // Your TTL value in seconds
        batchRequest.setClientId("yourClientId");
        batchRequest.setStatus(BatchStatus.NO_BATCH_ID.toString());
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