package it.pagopa.pn.address.manager.service;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.NormalizzazioneResponse;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.PostelClient;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.OperationResultCodeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AddressBatchRequestServiceTest {

    @MockBean
    AddressBatchRequestRepository addressBatchRequestRepository;

    @MockBean
    PostelBatchRepository postelBatchRepository;

    @MockBean
    AddressConverter addressConverter;

    @MockBean
    SqsService sqsService;

    @MockBean
    PostelClient postelClient;

    @MockBean
    SafeStorageService safeStorageService;

    @MockBean
    PnAddressManagerConfig pnAddressManagerConfig;

    @MockBean
    EventService eventService;

    @MockBean
    CsvService csvService;

    @MockBean
    AddressUtils addressUtils;

    private AddressBatchRequestService addressBatchRequestService;

    @Test
    void incrementAndCheckRetry(){
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils);

        BatchRequest batchRequest = getBatchRequest();

        assertDoesNotThrow(() -> addressBatchRequestService.incrementAndCheckRetry(List.of(batchRequest),new Throwable(), "batchId"));
    }

    @Test
    void incrementAndCheckRetry1(){
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils);

        assertDoesNotThrow(() -> addressBatchRequestService.incrementAndCheckRetry(getPostelBatch(),new Throwable()));
    }

    @Test
    void incrementAndCheckRetry2(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setRecoveryAfter(3);
        batchRequest.setMaxRetry(3);
        batchRequest.setMaxSize(3);
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setMaxRetry(3);
        postel.setMaxSize(3);
        postel.setRecoveryAfter(3);
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils);
        PostelBatch postelBatch = getPostelBatch();
        postelBatch.setRetry(50);
        StepVerifier.create(addressBatchRequestService.incrementAndCheckRetry(postelBatch,new Throwable())).expectError().verify();
    }

    @Test
    void updateBatchRequest(){
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository, postelBatchRepository, addressConverter, sqsService,
                postelClient, safeStorageService, pnAddressManagerConfig, eventService, csvService, addressUtils);
        BatchRequest batchRequest1 = getBatchRequest();
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));

        assertDoesNotThrow(() -> addressBatchRequestService.updateBatchRequest("batchId", BatchStatus.NO_BATCH_ID));
    }

    @Test
    void updateBatchRequest1(){
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils);
        BatchRequest batchRequest1 = getBatchRequest();
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        when(sqsService.sendToDlqQueue(batchRequest1)).thenReturn(Mono.empty());
        StepVerifier.create(addressBatchRequestService.updateBatchRequest("batchId", BatchStatus.NO_BATCH_ID)).expectNextCount(0).verifyComplete();
    }

    @Test
    void updateBatchRequest2(){
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService,csvService,addressUtils);
        BatchRequest batchRequest1 = getBatchRequest();
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        when(sqsService.sendToDlqQueue(batchRequest1)).thenReturn(Mono.empty());
        StepVerifier.create(addressBatchRequestService.updateBatchRequest(List.of(batchRequest1),"batchId")).expectNextCount(0).verifyComplete();
    }

    @Test
    void updateBatchRequest3(){
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService,csvService,addressUtils);
        BatchRequest batchRequest1 = getBatchRequest();
        batchRequest1.setStatus(BatchStatus.WORKED.toString());
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        when(sqsService.sendToDlqQueue(batchRequest1)).thenReturn(Mono.empty());
        when(eventService.sendEvent(any(),any())).thenReturn(Mono.just(new PutEventsResult()));
        StepVerifier.create(addressBatchRequestService.updateBatchRequest(List.of(batchRequest1),"batchId")).expectNextCount(0).verifyComplete();
    }


    PostelBatch getPostelBatch(){
        PostelBatch postelBatch = new PostelBatch();
        postelBatch.setFileKey("fileKey");
        postelBatch.setStatus("status");
        postelBatch.setRetry(0);
        postelBatch.setBatchId("batchId");
        return postelBatch;
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
        batchRequest.setReservationId("yourReservationId");
        batchRequest.setCreatedAt(LocalDateTime.now()); // Your LocalDateTime value
        batchRequest.setSendStatus("yourSendStatus");
        batchRequest.setMessage("yourMessage");
        batchRequest.setXApiKey("yourXApiKey");
        batchRequest.setCxId("yourCxId");
        batchRequest.setAwsMessageId("yourAwsMessageId");
        return batchRequest;
    }

}