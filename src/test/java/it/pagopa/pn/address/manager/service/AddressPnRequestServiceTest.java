package it.pagopa.pn.address.manager.service;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.normalizzatore.v1.dto.NormalizzazioneResponse;
import it.pagopa.pn.address.manager.generated.openapi.msclient.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.middleware.client.NormalizzatoreClient;
import it.pagopa.pn.address.manager.model.NormalizeRequestPostelInput;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.*;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AddressBatchRequestTest {

    @MockitoBean
    AddressBatchRequestRepository addressPnRequestRepository;

    @MockitoBean
    PostelBatchRepository postelBatchRepository;

    @MockitoBean
    AddressConverter addressConverter;

    @MockitoBean
    SqsService sqsService;

    @MockitoBean
    NormalizzatoreClient postelClient;

    @MockitoBean
    SafeStorageService safeStorageService;

    @MockitoBean
    PnAddressManagerConfig pnAddressManagerConfig;

    @MockitoBean
    EventService eventService;

    @MockitoBean
    CsvService csvService;

    @MockitoBean
    AddressUtils addressUtils;
    @MockitoBean
    Clock clock;

    private PnRequestService addressPnRequestService;
    @Test
    void batchAddressRequest(){
        Instant now = Instant.now();
        when(clock.instant()).thenReturn(now);
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer3();
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        long ttl = 3600L;
        postel.setWorkingTtl(Duration.ofSeconds(ttl));
        normalizer.setPostel(postel);
        pnAddressManagerConfig.setNormalizer(normalizer);

        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);


        PnRequest pnRequest1 = getPnRequest();
        Page<PnRequest> page1 = Page.create(List.of(pnRequest1), new HashMap<>());
        when(addressPnRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
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

        when(addressPnRequestRepository.setNewBatchIdToBatchRequest(same(pnRequest1)))
                .thenReturn(Mono.just(pnRequest1));

        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.setKey("key");
        fileCreationResponseDto.setSecret("secret");
        when(safeStorageService.callSelfStorageCreateFileAndUpload(anyString(),any())).thenReturn(Mono.just(fileCreationResponseDto));

        NormalizzatoreBatch normalizzatoreBatch = getPostelBatch();
        when(postelBatchRepository.create(any())).thenReturn(Mono.just(normalizzatoreBatch));
        when(addressPnRequestRepository.getBatchRequestByBatchIdAndStatus(anyMap(), any(),any())).thenReturn(Mono.just(Page.create(List.of(pnRequest1))));
        when(addressPnRequestRepository.update(pnRequest1)).thenReturn(Mono.just(pnRequest1));
        NormalizzazioneResponse responseActivatePostel = new NormalizzazioneResponse();
        when(postelClient.activatePostel(any())).thenReturn(Mono.just(responseActivatePostel));
        ArgumentCaptor<NormalizzatoreBatch> batchCaptor = ArgumentCaptor.forClass(NormalizzatoreBatch.class);
        when(postelBatchRepository.update(batchCaptor.capture())).thenReturn(Mono.just(normalizzatoreBatch));
        Assertions.assertDoesNotThrow(() -> addressPnRequestService.batchAddressRequest());

        // Verifica che il TTL sia stato impostato correttamente
        NormalizzatoreBatch capturedBatch = batchCaptor.getValue();
        assertNotNull(capturedBatch);
        int expectedTtl = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(ttl).getHour();
        assertEquals(expectedTtl, capturedBatch.getWorkingTtl().getHour());
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
        postel.setWorkingTtl(Duration.ofSeconds(12));
        normalizer.setPostel(postel);

        batchRequest.setQueryMaxSize(5);
        normalizer.setMaxCsvSize(0);
        normalizer.setBatchRequest(batchRequest);
        pnAddressManagerConfig.setNormalizer(normalizer);

        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);


        PnRequest batchRequest1 = getPnRequest();
        Page<PnRequest> page1 = Page.create(List.of(batchRequest1), new HashMap<>());
        when(addressPnRequestRepository.getBatchRequestByNotBatchId(anyMap(), anyInt()))
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

        when(addressPnRequestRepository.setNewBatchIdToBatchRequest(same(batchRequest1)))
                .thenReturn(Mono.just(batchRequest1));

        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.setKey("key");
        fileCreationResponseDto.setSecret("secret");
        when(safeStorageService.callSelfStorageCreateFileAndUpload(anyString(),any())).thenReturn(Mono.just(fileCreationResponseDto));

        NormalizzatoreBatch postelBatch = getPostelBatch();
        when(postelBatchRepository.create(postelBatch)).thenReturn(Mono.just(postelBatch));
        when(addressConverter.createPostelBatchByBatchIdAndFileKey(any(),any(), any())).thenReturn(postelBatch);
        when(addressPnRequestRepository.getBatchRequestByBatchIdAndStatus(anyMap(), any(),any())).thenReturn(Mono.just(Page.create(List.of(batchRequest1))));
        when(addressPnRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        NormalizzazioneResponse responseActivatePostel = new NormalizzazioneResponse();
        when(postelClient.activatePostel(any())).thenReturn(Mono.just(responseActivatePostel));
        when(postelBatchRepository.update(postelBatch)).thenReturn(Mono.just(postelBatch));
        Assertions.assertDoesNotThrow(() -> addressPnRequestService.batchAddressRequest());

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
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,config,eventService, csvService, addressUtils, clock);

        PnRequest pnRequest = getPnRequest();

        assertDoesNotThrow(() -> addressPnRequestService.incrementAndCheckRetry(List.of(pnRequest),new Throwable(), "batchId"));
    }

    @Test
    void incrementAndCheckRetry1(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer2();
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);

        assertDoesNotThrow(() -> addressPnRequestService.incrementAndCheckRetry(getPostelBatch(),new Throwable()));
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer2() {
        return getNormalizer3();
    }

    @Test
    void incrementAndCheckRetry2(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer1();
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);
        NormalizzatoreBatch normalizzatoreBatch = getPostelBatch();
        normalizzatoreBatch.setRetry(50);
        StepVerifier.create(addressPnRequestService.incrementAndCheckRetry(normalizzatoreBatch,new Throwable())).expectError().verify();
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
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,config,eventService, csvService, addressUtils, clock);
        PnRequest pnRequest1 = getPnRequest();
        when(addressPnRequestRepository.getBatchRequestByBatchIdAndStatus(anyMap(), any(),any())).thenReturn(Mono.just(Page.create(List.of(pnRequest1))));
        when(addressPnRequestRepository.update(pnRequest1)).thenReturn(Mono.just(pnRequest1));

        assertDoesNotThrow(() -> addressPnRequestService.retrieveAndUpdateBatchRequest("batchId", BatchStatus.WORKING, BatchStatus.TAKEN_CHARGE, true));
    }

    @Test
    void updateBatchRequest1(){
        PnAddressManagerConfig config = getPnAddressManagerConfig();
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,config,eventService, csvService, addressUtils, clock);
        PnRequest pnRequest1 = getPnRequest();
        when(addressPnRequestRepository.getBatchRequestByBatchIdAndStatus(anyMap(), any(),any())).thenReturn(Mono.just(Page.create(List.of(pnRequest1))));
        when(addressPnRequestRepository.update(pnRequest1)).thenReturn(Mono.just(pnRequest1));
        when(sqsService.sendToDlqQueue(pnRequest1)).thenReturn(Mono.empty());
        when(clock.instant()).thenReturn(Instant.now());
        StepVerifier.create(addressPnRequestService.retrieveAndUpdateBatchRequest("batchId", BatchStatus.TAKEN_CHARGE, BatchStatus.WORKING, false)).expectNextCount(0).verifyComplete();
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
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);
        PnRequest pnRequest1 = getPnRequest();
        when(addressPnRequestRepository.update(pnRequest1)).thenReturn(Mono.just(pnRequest1));
        when(sqsService.sendToDlqQueue(pnRequest1)).thenReturn(Mono.empty());
        StepVerifier.create(addressPnRequestService.updateBatchRequest(List.of(pnRequest1),"batchId")).expectNextCount(0).verifyComplete();
    }

    @Test
    void updateBatchRequestERROR(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer3();
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService,csvService,addressUtils, clock);
        PnRequest batchRequest1 = getPnRequest();
        batchRequest1.setStatus(BatchStatus.ERROR.name());
        when(addressPnRequestRepository.update(batchRequest1)).thenReturn(Mono.just(batchRequest1));
        when(sqsService.sendToDlqQueue(batchRequest1)).thenReturn(Mono.empty());
        StepVerifier.create(addressPnRequestService.updateBatchRequest(List.of(batchRequest1),"batchId")).expectNextCount(0).verifyComplete();
    }

    @Test
    void updateBatchRequest3(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = getNormalizer3();
        long ttl = 3600L;
        normalizer.getBatchRequest().setTtl(Duration.ofSeconds(ttl));
        pnAddressManagerConfig.setNormalizer(normalizer);
        addressPnRequestService = new PnRequestService(addressPnRequestRepository,postelBatchRepository,addressConverter,sqsService,
                postelClient,safeStorageService,pnAddressManagerConfig,eventService, csvService, addressUtils, clock);
        PnRequest pnRequest1 = getPnRequest();
        pnRequest1.setStatus(BatchStatus.WORKED.toString());
        ArgumentCaptor<PnRequest> batchCaptor = ArgumentCaptor.forClass(PnRequest.class);
        when(addressPnRequestRepository.update(batchCaptor.capture())).thenReturn(Mono.just(pnRequest1));
        when(sqsService.sendToDlqQueue(pnRequest1)).thenReturn(Mono.empty());
        when(eventService.sendEvent(any())).thenReturn(Mono.just(PutEventsResponse.builder().build()));
        StepVerifier.create(addressPnRequestService.updateBatchRequest(List.of(pnRequest1),"batchId")).expectNextCount(0).verifyComplete();

        // Verifica che il TTL sia stato impostato correttamente
        PnRequest capturedBatch = batchCaptor.getValue();
        assertNotNull(capturedBatch);
        long expectedTtlEpochSeconds = LocalDateTime.now().plusSeconds(ttl).toEpochSecond(ZoneOffset.UTC);
        assertEquals(expectedTtlEpochSeconds, capturedBatch.getTtl(), 5); // tolleranza di 5 secondi
    }


    NormalizzatoreBatch getPostelBatch(){
        NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
        normalizzatoreBatch.setFileKey("fileKey");
        normalizzatoreBatch.setStatus("status");
        normalizzatoreBatch.setRetry(0);
        normalizzatoreBatch.setBatchId("batchId");
        return normalizzatoreBatch;
    }

    PnRequest getPnRequest(){
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("yourCorrelationId");
        pnRequest.setAddresses("yourAddresses");
        pnRequest.setBatchId("NO_BATCH_ID");
        pnRequest.setRetry(1);
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
