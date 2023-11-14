package it.pagopa.pn.address.manager.service;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneResponse;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.NormalizzatoreClient;
import it.pagopa.pn.address.manager.model.NormalizeRequestPostelInput;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

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
    NormalizzatoreClient postelClient;

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
    @MockBean
    Clock clock;

    private AddressBatchRequestService addressBatchRequestService;
    @Test
    void batchAddressRequest(){
        Instant now = Instant.now();
        when(clock.instant()).thenReturn(now);
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer3();
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setWorkingTtl(12);
        normalizer.setPostel(postel);
        pnAddressManagerConfig.setNormalizer(normalizer);

        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);


        BatchRequest batchRequest1 = getBatchRequest();
        Page<BatchRequest> page1 = Page.create(List.of(batchRequest1), new HashMap<>());
        when(addressBatchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1));

        NormalizeRequestPostelInput request = new NormalizeRequestPostelInput();
        request.setIdCodiceCliente("12345");
        request.setProvincia("TO");
        request.setCap("12345");
        request.setLocalita("Sample Località");
        request.setLocalitaAggiuntiva("Sample Località Aggiuntiva");
        request.setIndirizzo("123 Main St");
        request.setStato("IT");
        when(addressUtils.normalizeRequestToPostelCsvRequest(any())).thenReturn(List.of(request));
        when(csvService.writeItemsOnCsvToString(any())).thenReturn("csvContent");
        when(addressUtils.computeSha256(any())).thenReturn("sha256");

        when(addressBatchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));

        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.setKey("key");
        fileCreationResponseDto.setSecret("secret");
        when(safeStorageService.callSelfStorageCreateFileAndUpload(anyString(),any())).thenReturn(Mono.just(fileCreationResponseDto));

        PostelBatch postelBatch = getPostelBatch();
        when(postelBatchRepository.create(postelBatch)).thenReturn(Mono.just(postelBatch));
        when(addressConverter.createPostelBatchByBatchIdAndFileKey(any(),any(), any())).thenReturn(postelBatch);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        NormalizzazioneResponse responseActivatePostel = new NormalizzazioneResponse();
        when(postelClient.activatePostel(any())).thenReturn(responseActivatePostel);
        when(postelBatchRepository.update(postelBatch)).thenReturn(Mono.just(postelBatch));
        Assertions.assertDoesNotThrow(() -> addressBatchRequestService.batchAddressRequest());

    }
    @Test
    void batchAddressRequest2(){
        Instant now = Instant.now();
        when(clock.instant()).thenReturn(now);
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer();
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setLockAtMost(100);
        batchRequest.setLockAtLeast(100);
        batchRequest.setRecoveryAfter(3);
        batchRequest.setMaxRetry(3);
        batchRequest.setRecoveryDelay(3);
        batchRequest.setDelay(3);
        batchRequest.setEventBridgeRecoveryDelay(3);
        normalizer.setMaxFileNumber(2);
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setWorkingTtl(12);
        normalizer.setPostel(postel);

        batchRequest.setQueryMaxSize(5);
        normalizer.setMaxCsvSize(0);
        normalizer.setBatchRequest(batchRequest);
        pnAddressManagerConfig.setNormalizer(normalizer);

        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);


        BatchRequest batchRequest1 = getBatchRequest();
        Page<BatchRequest> page1 = Page.create(List.of(batchRequest1), new HashMap<>());
        when(addressBatchRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1));

        NormalizeRequestPostelInput request = new NormalizeRequestPostelInput();
        request.setIdCodiceCliente("12345");
        request.setProvincia("TO");
        request.setCap("12345");
        request.setLocalita("Sample Località");
        request.setLocalitaAggiuntiva("Sample Località Aggiuntiva");
        request.setIndirizzo("123 Main St");
        request.setStato("IT");
        when(addressUtils.normalizeRequestToPostelCsvRequest(any())).thenReturn(List.of(request));
        when(csvService.writeItemsOnCsvToString(any())).thenReturn("csvContent");
        when(addressUtils.computeSha256(any())).thenReturn("sha256");

        when(addressBatchRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));

        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.setKey("key");
        fileCreationResponseDto.setSecret("secret");
        when(safeStorageService.callSelfStorageCreateFileAndUpload(anyString(),any())).thenReturn(Mono.just(fileCreationResponseDto));

        PostelBatch postelBatch = getPostelBatch();
        when(postelBatchRepository.create(postelBatch)).thenReturn(Mono.just(postelBatch));
        when(addressConverter.createPostelBatchByBatchIdAndFileKey(any(),any(), any())).thenReturn(postelBatch);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        NormalizzazioneResponse responseActivatePostel = new NormalizzazioneResponse();
        when(postelClient.activatePostel(any())).thenReturn(responseActivatePostel);
        when(postelBatchRepository.update(postelBatch)).thenReturn(Mono.just(postelBatch));
        Assertions.assertDoesNotThrow(() -> addressBatchRequestService.batchAddressRequest());

    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer3() {
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
        normalizer.setMaxFileNumber(2);
        normalizer.setBatchRequest(batchRequest);
        return normalizer;
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer() {
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setRecoveryAfter(3);
        batchRequest.setMaxRetry(3);
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setMaxRetry(3);
        postel.setRecoveryAfter(3);
        postel.setRequestPrefix("prefix");
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        return normalizer;
    }

    @Test
    void incrementAndCheckRetry(){
        PnAddressManagerConfig config = getPnAddressManagerConfig();
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,config,eventService, csvService, addressUtils, clock);

        BatchRequest batchRequest = getBatchRequest();
        when(sqsService.sendToDlqQueue(batchRequest)).thenReturn(Mono.empty());
        when(addressBatchRequestRepository.update(batchRequest)).thenReturn(Mono.just(batchRequest));

        assertDoesNotThrow(() -> addressBatchRequestService.incrementAndCheckRetry(List.of(batchRequest),new Throwable(), "batchId"));
    }

    @Test
    void incrementAndCheckRetry1(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer2();
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);

        assertDoesNotThrow(() -> addressBatchRequestService.incrementAndCheckRetry(getPostelBatch(),new Throwable()));
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer2() {
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer3();
        return normalizer;
    }

    @Test
    void incrementAndCheckRetry2(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer1();
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);
        PostelBatch postelBatch = getPostelBatch();
        postelBatch.setRetry(50);
        StepVerifier.create(addressBatchRequestService.incrementAndCheckRetry(postelBatch,new Throwable())).expectError().verify();
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer1() {
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

    @Test
    void updateBatchRequest(){
        PnAddressManagerConfig config = getPnAddressManagerConfig();
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository, postelBatchRepository, addressConverter, sqsService,
                postelClient, safeStorageService, config, eventService, csvService, addressUtils, clock);
        BatchRequest batchRequest1 = getBatchRequest();
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));

        assertDoesNotThrow(() -> addressBatchRequestService.updateBatchRequest("batchId", BatchStatus.NO_BATCH_ID));
    }

    @Test
    void updateBatchRequest1(){
        PnAddressManagerConfig config = getPnAddressManagerConfig();
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,config,eventService, csvService, addressUtils, clock);
        BatchRequest batchRequest1 = getBatchRequest();
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        when(sqsService.sendToDlqQueue(batchRequest1)).thenReturn(Mono.empty());
        StepVerifier.create(addressBatchRequestService.updateBatchRequest("batchId", BatchStatus.NO_BATCH_ID)).expectNextCount(0).verifyComplete();
    }

    @NotNull
    private static PnAddressManagerConfig getPnAddressManagerConfig() {
        PnAddressManagerConfig config = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setRequestPrefix("");
        normalizer.setPostel(postel);
        normalizer.setMaxFileNumber(2);
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setMaxRetry(3);
        normalizer.setBatchRequest(batchRequest);
        config.setNormalizer(normalizer);
        return config;
    }

    @Test
    void updateBatchRequest2(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer3();
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService,csvService,addressUtils, clock);
        BatchRequest batchRequest1 = getBatchRequest();
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        when(sqsService.sendToDlqQueue(batchRequest1)).thenReturn(Mono.empty());
        StepVerifier.create(addressBatchRequestService.updateBatchRequest(List.of(batchRequest1),"batchId")).expectNextCount(0).verifyComplete();
    }

    @Test
    void updateBatchRequest3(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer3();
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressBatchRequestService = new AddressBatchRequestService(addressBatchRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService,csvService,addressUtils, clock);
        BatchRequest batchRequest1 = getBatchRequest();
        batchRequest1.setStatus(BatchStatus.WORKED.toString());
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(),any())).thenReturn(Mono.just(List.of(batchRequest1)));
        when(addressBatchRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        when(sqsService.sendToDlqQueue(batchRequest1)).thenReturn(Mono.empty());
        when(eventService.sendEvent(any())).thenReturn(Mono.just(new PutEventsResult()));
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
        batchRequest.setCreatedAt(LocalDateTime.now()); // Your LocalDateTime value
        batchRequest.setSendStatus("yourSendStatus");
        batchRequest.setMessage("yourMessage");
        batchRequest.setXApiKey("yourXApiKey");
        batchRequest.setCxId("yourCxId");
        batchRequest.setAwsMessageId("yourAwsMessageId");
        return batchRequest;
    }

}
