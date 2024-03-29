package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.entity.PnRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class RecoveryServiceTest {
    RecoveryService recoveryService;
    PendingRequestService pendingRequestService;

    @MockBean AddressBatchRequestRepository addressBatchRequestRepository;
    @MockBean
    PnRequestService pnRequestService;
    @MockBean SqsService sqsService;
    @MockBean EventService eventService;
    @MockBean PnAddressManagerConfig pnAddressManagerConfig;

    @MockBean PostelBatchRepository postelBatchRepository;
    @MockBean Clock clock;


    @MockBean
    AddressUtils addressUtils;

    @Test
    void recoveryBatchRequest(){
        recoveryService = new RecoveryService(addressBatchRequestRepository, pnRequestService, sqsService, eventService, pnAddressManagerConfig, addressUtils, postelBatchRepository);

        PnRequest pnRequest = getBatchRequest();
        when( addressBatchRequestRepository.getBatchRequestToRecovery()).thenReturn(Mono.just(List.of(pnRequest)));
        when(addressBatchRequestRepository.resetBatchRequestForRecovery(any())).thenReturn(Mono.just(pnRequest));

        Assertions.assertDoesNotThrow(() -> recoveryService.recoveryBatchRequest());
    }

    @Test
    void recoveryPostelActivation(){
        recoveryService = new RecoveryService(addressBatchRequestRepository, pnRequestService, sqsService, eventService, pnAddressManagerConfig, addressUtils, postelBatchRepository);

        NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
        normalizzatoreBatch.setBatchId("id");
        when(postelBatchRepository.getPostelBatchToRecover()).thenReturn(Mono.just(List.of(normalizzatoreBatch)));
        when(postelBatchRepository.resetPostelBatchForRecovery(any())).thenReturn(Mono.just(normalizzatoreBatch));
        when(pnRequestService.callPostelActivationApi(any())).thenReturn(Mono.empty());

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
        recoveryService = new RecoveryService(addressBatchRequestRepository, pnRequestService, sqsService, eventService, pnAddressManagerConfig, addressUtils, postelBatchRepository);

        PnRequest pnRequest1 = getBatchRequest();
        PnRequest pnRequest2 = getBatchRequest();
        Page<PnRequest> page1 = Page.create(List.of(pnRequest1), Map.of("key", AttributeValue.builder().s("value").build()));
        Page<PnRequest> page2 = Page.create(List.of(pnRequest2));
        when(addressBatchRequestRepository.getBatchRequestToSend(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);
        when(addressBatchRequestRepository.resetBatchRequestForRecovery(any())).thenReturn(Mono.just(pnRequest1));
        when(addressBatchRequestRepository.resetBatchRequestForRecovery(any())).thenReturn(Mono.just(pnRequest2));

        when(addressBatchRequestRepository.setNewReservationIdToBatchRequest(any())).thenReturn(Mono.just(pnRequest1));
        when(addressBatchRequestRepository.setNewReservationIdToBatchRequest(any())).thenReturn(Mono.just(pnRequest2));


        when(sqsService.sendToDlqQueue((PnRequest) any())).thenReturn(Mono.empty());
        when(eventService.sendEvent(anyString())).thenReturn(Mono.just(PutEventsResponse.builder().build()));
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
        pendingRequestService = new PendingRequestService(addressBatchRequestRepository,
                pnRequestService,postelBatchRepository, clock);
        PnRequest pnRequest1 = getBatchRequest();
        NormalizzatoreBatch normalizzatoreBatch1 = new NormalizzatoreBatch();
        normalizzatoreBatch1.setBatchId("id1");
        NormalizzatoreBatch normalizzatoreBatch2 = new NormalizzatoreBatch();
        normalizzatoreBatch2.setBatchId("id2");
        Page<NormalizzatoreBatch> page1 = Page.create(List.of(normalizzatoreBatch1), Map.of("key", AttributeValue.builder().s("value").build()));
        Page<NormalizzatoreBatch> page2 = Page.create(List.of(normalizzatoreBatch2));
        when(postelBatchRepository.getPostelBatchToClean(anyMap()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(anyMap(), anyString(), any()))
                .thenReturn(Mono.just(Page.create(List.of(pnRequest1))));
        when(pnRequestService.incrementAndCheckRetry(any(),any(),anyString()))
                .thenReturn(Mono.empty());
        when(addressBatchRequestRepository.update(any()))
                .thenReturn(Mono.just(pnRequest1));
        when(postelBatchRepository.deleteItem(anyString()))
                .thenReturn(Mono.empty());
        when(addressBatchRequestRepository.update(any()))
                .thenReturn(Mono.just(pnRequest1));
        when(postelBatchRepository.deleteItem(anyString()))
                .thenReturn(Mono.empty());
        Assertions.assertDoesNotThrow(() -> pendingRequestService.cleanPendingPostelBatch());

    }

    @Test
    void testCleanStoppedRequestWithRelatedBatchRequest () {
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
        pendingRequestService = new PendingRequestService(addressBatchRequestRepository,
                pnRequestService,postelBatchRepository, clock);
        PnRequest pnRequest1 = getBatchRequest();
        PnRequest pnRequest2 = getBatchRequest();
        NormalizzatoreBatch normalizzatoreBatch1 = new NormalizzatoreBatch();
        normalizzatoreBatch1.setBatchId("id1");
        Page<NormalizzatoreBatch> page1 = Page.create(List.of(normalizzatoreBatch1), Map.of("key", AttributeValue.builder().s("value").build()));
        when(postelBatchRepository.getPostelBatchToClean(anyMap()))
                .thenReturn(Mono.just(page1))
                .thenThrow(RuntimeException.class);
        List<PnRequest> requests = List.of(pnRequest1, pnRequest2);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(anyMap(), anyString(), any()))
                .thenReturn(Mono.just(Page.create(requests)));
        when(pnRequestService.incrementAndCheckRetry(requests, null, "id1"))
                .thenReturn(Mono.empty());

        when(addressBatchRequestRepository.update(pnRequest1))
                .thenReturn(Mono.just(pnRequest1));

        pnRequest1.setStatus(BatchStatus.TAKEN_CHARGE.getValue());
        when(addressBatchRequestRepository.update(pnRequest1))
                .thenReturn(Mono.just(pnRequest1));
        pnRequest1.setStatus(BatchStatus.NO_BATCH_ID.getValue());

        when(addressBatchRequestRepository.update(pnRequest2))
                .thenReturn(Mono.just(pnRequest2));

        pnRequest2.setStatus(BatchStatus.TAKEN_CHARGE.getValue());
        when(addressBatchRequestRepository.update(pnRequest2))
                .thenReturn(Mono.just(pnRequest2));
        pnRequest2.setStatus(BatchStatus.NO_BATCH_ID.getValue());

        when(postelBatchRepository.deleteItem("id1"))
                .thenReturn(Mono.empty());
        when(clock.instant()).thenReturn(Instant.now());
        Assertions.assertDoesNotThrow(() -> pendingRequestService.cleanPendingPostelBatch());

        verify(pnRequestService, times(1)).incrementAndCheckRetry(requests, null, "id1");

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

    PnRequest getBatchRequest(){
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("yourCorrelationId");
        pnRequest.setAddresses("yourAddresses");
        pnRequest.setBatchId("NO_BATCH_ID");
        pnRequest.setRetry(0);
        pnRequest.setTtl(3600L); // Your TTL value in seconds
        pnRequest.setClientId("yourClientId");
        pnRequest.setStatus(BatchStatus.NO_BATCH_ID.toString());
        pnRequest.setLastReserved(LocalDateTime.now()); // Your LocalDateTime value
        pnRequest.setCreatedAt(LocalDateTime.now()); // Your LocalDateTime value
        pnRequest.setSendStatus("yourSendStatus");
        pnRequest.setMessage("yourMessage");
        pnRequest.setXApiKey("yourXApiKey");
        pnRequest.setAwsMessageId("yourAwsMessageId");
        return pnRequest;
    }

}