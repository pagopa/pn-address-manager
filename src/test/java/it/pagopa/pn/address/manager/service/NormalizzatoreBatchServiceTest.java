package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadInfo;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PostelBatchService.class})
class NormalizzatoreBatchServiceTest {

    @Autowired
    PostelBatchService postelBatchService;

    @MockBean
    AddressBatchRequestRepository addressBatchRequestRepository;
    @MockBean
    PostelBatchRepository postelBatchRepository;
    @MockBean
    CsvService csvService;
    @MockBean
    AddressUtils addressUtils;
    @MockBean
    UploadDownloadClient uploadDownloadClient;

    @MockBean
    AddressBatchRequestService addressBatchRequestService;
    @MockBean
    CapAndCountryService capAndCountryService;

    @MockBean
    PnAddressManagerConfig pnAddressManagerConfig;

    @MockBean
    SafeStorageService safeStorageService;

    @MockBean
    Clock clock;

    @Test
    void resetRelatedBatchRequestForRetry(){
        NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
        normalizzatoreBatch.setBatchId("id");
        when(postelBatchRepository.update(any())).thenReturn(Mono.just(normalizzatoreBatch));
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(anyMap(), any(), any())).thenReturn(Mono.just(Page.create(List.of(new PnRequest()))));
        StepVerifier.create(postelBatchService.resetRelatedBatchRequestForRetry(normalizzatoreBatch)).expectError().verify();
    }
    @Test
    void findPostelBatch(){
        when(postelBatchRepository.findByBatchId(anyString())).thenReturn(Mono.just(new NormalizzatoreBatch()));
        StepVerifier.create(postelBatchService.findPostelBatch("fileKey")).expectNext(new NormalizzatoreBatch()).verifyComplete();
    }
    @Test
    void getResponse(){

        when(uploadDownloadClient.downloadContent(anyString())).thenReturn(Mono.just("url".getBytes()));
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("id");
        when(csvService.readItemsFromCsv(NormalizedAddress.class,"url".getBytes(),0)).thenReturn(List.of(normalizedAddress));
        when(addressUtils.getCorrelationIdCreatedAt(anyString())).thenReturn("id");
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("id");
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
        pnRequest.setCxId("yourCxId");
        pnRequest.setAwsMessageId("yourAwsMessageId");
        when(clock.instant()).thenReturn(Instant.now());
        when(addressUtils.getNormalizeRequestFromBatchRequest(any())).thenReturn(List.of(new NormalizeRequest()));
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(Map.of(), "id", BatchStatus.WORKING)).thenReturn(Mono.just(Page.create(List.of(pnRequest))));
        when(addressBatchRequestService.updateBatchRequest(anyList(),anyString())).thenReturn(Mono.empty());
        NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
        normalizzatoreBatch.setBatchId("id");
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        FileDownloadInfo info = new FileDownloadInfo();
        info.setUrl("http://url.it");
        fileDownloadResponse.setDownload(info);
        when(safeStorageService.getFile(any(), any())).thenReturn(Mono.just(fileDownloadResponse));
        StepVerifier.create(postelBatchService.getResponse("url", normalizzatoreBatch)).verifyComplete();
    }

    @Test
    void getResponse1(){

        when(uploadDownloadClient.downloadContent(anyString())).thenReturn(Mono.just("url".getBytes()));
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("id");
        when(csvService.readItemsFromCsv(NormalizedAddress.class,"url".getBytes(),1)).thenReturn(List.of(normalizedAddress));
        when(addressUtils.getCorrelationIdCreatedAt(anyString())).thenReturn("id");
        PnRequest pnRequest = new PnRequest();
        pnRequest.setBatchId("id");
        pnRequest.setCorrelationId("id");
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(Map.of(), "id", BatchStatus.WORKING)).thenReturn(Mono.just(Page.create(List.of(pnRequest))));
        when(addressBatchRequestService.updateBatchRequest(anyList(),anyString())).thenReturn(Mono.empty());
        when(addressUtils.getNormalizeRequestFromBatchRequest(any())).thenReturn(List.of(new NormalizeRequest()));
        NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
        normalizzatoreBatch.setBatchId("id");
        AnalogAddress base = new AnalogAddress();
        base.setCity("Roma");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("RM");
        base.setCap("00010");
        base.setCountry("ITALIA");
        NormalizeResult normalizeResult = new NormalizeResult();
        normalizeResult.setNormalizedAddress(base);
        when(addressUtils.toResultItem(any(), any())).thenReturn(List.of(normalizeResult));
        when(addressUtils.toJson(anyString())).thenReturn("json");
        when(clock.instant()).thenReturn(Instant.now());
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        FileDownloadInfo info = new FileDownloadInfo();
        info.setUrl("http://url.it");
        fileDownloadResponse.setDownload(info);
        when(safeStorageService.getFile(any(), any())).thenReturn(Mono.just(fileDownloadResponse));
        CapModel capModel = new CapModel();
        capModel.setCap("00010");
        capModel.setEndValidity(LocalDateTime.now());
        capModel.setStartValidity(LocalDateTime.now());
        StepVerifier.create(postelBatchService.getResponse("url", normalizzatoreBatch)).verifyComplete();
    }


    @Test
    void getResponse2(){

        when(uploadDownloadClient.downloadContent(anyString())).thenReturn(Mono.just("url".getBytes()));
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("id");
        when(csvService.readItemsFromCsv(NormalizedAddress.class,"url".getBytes(),1)).thenReturn(List.of(normalizedAddress));
        when(addressUtils.getCorrelationIdCreatedAt(anyString())).thenReturn("id");
        PnRequest pnRequest = new PnRequest();
        pnRequest.setBatchId("id");
        pnRequest.setCorrelationId("id");
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(Map.of(), "id", BatchStatus.WORKING)).thenReturn(Mono.just(Page.create(List.of(pnRequest))));
        when(addressBatchRequestService.updateBatchRequest(anyList(),anyString())).thenReturn(Mono.empty());
        when(addressUtils.getNormalizeRequestFromBatchRequest(any())).thenReturn(List.of(new NormalizeRequest()));
        NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
        normalizzatoreBatch.setBatchId("id");
        AnalogAddress base = new AnalogAddress();
        base.setCity("Roma");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("RM");
        base.setCap("00010");
        base.setCountry("COUNTRY");
        NormalizeResult normalizeResult = new NormalizeResult();
        normalizeResult.setNormalizedAddress(base);
        when(clock.instant()).thenReturn(Instant.now());
        when(addressUtils.toResultItem(any(), any())).thenReturn(List.of(normalizeResult));
        when(addressUtils.toJson(anyString())).thenReturn("json");
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        FileDownloadInfo info = new FileDownloadInfo();
        info.setUrl("http://url.it");
        fileDownloadResponse.setDownload(info);
        when(safeStorageService.getFile(any(), any())).thenReturn(Mono.just(fileDownloadResponse));
        StepVerifier.create(postelBatchService.getResponse("url", normalizzatoreBatch)).verifyComplete();
    }
}