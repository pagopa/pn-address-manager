package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class PostelBatchServiceTest {

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

    @Test
    void resetRelatedBatchRequestForRetry(){
        postelBatchService = new PostelBatchService(addressBatchRequestRepository, postelBatchRepository, csvService, addressUtils, uploadDownloadClient, addressBatchRequestService, capAndCountryService);
        PostelBatch postelBatch = new PostelBatch();
        postelBatch.setBatchId("id");
        when(postelBatchRepository.update(any())).thenReturn(Mono.just(postelBatch));
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(anyString(),any())).thenReturn(Mono.just(List.of(new BatchRequest())));
        StepVerifier.create(postelBatchService.resetRelatedBatchRequestForRetry(postelBatch)).expectError().verify();
    }
    @Test
    void findPostelBatch(){
        postelBatchService = new PostelBatchService(addressBatchRequestRepository, postelBatchRepository, csvService, addressUtils, uploadDownloadClient, addressBatchRequestService, capAndCountryService);
        when(postelBatchRepository.findByBatchId(anyString())).thenReturn(Mono.just(new PostelBatch()));
        StepVerifier.create(postelBatchService.findPostelBatch("fileKey")).expectNext(new PostelBatch()).verifyComplete();
    }
    @Test
    void getResponse(){
        postelBatchService = new PostelBatchService(addressBatchRequestRepository, postelBatchRepository, csvService, addressUtils, uploadDownloadClient, addressBatchRequestService, capAndCountryService);

        when(uploadDownloadClient.downloadContent(anyString())).thenReturn(Mono.just("url".getBytes()));
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("id");
        when(csvService.readItemsFromCsv(NormalizedAddress.class,"url".getBytes(),0)).thenReturn(List.of(normalizedAddress));
        when(addressUtils.getCorrelationId(anyString())).thenReturn("id");
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("id");
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
        when(addressUtils.getNormalizeRequestFromBatchRequest(any())).thenReturn(List.of(new NormalizeRequest()));
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus("id", BatchStatus.WORKING)).thenReturn(Mono.just(List.of(batchRequest)));
        when(addressBatchRequestService.updateBatchRequest(anyString(),any())).thenReturn(Mono.empty());
        PostelBatch postelBatch = new PostelBatch();
        postelBatch.setBatchId("id");
        StepVerifier.create(postelBatchService.getResponse("url", postelBatch)).expectError().verify();
    }

    @Test
    void getResponse1(){
        postelBatchService = new PostelBatchService(addressBatchRequestRepository, postelBatchRepository, csvService, addressUtils, uploadDownloadClient, addressBatchRequestService, capAndCountryService);

        when(uploadDownloadClient.downloadContent(anyString())).thenReturn(Mono.just("url".getBytes()));
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("id");
        when(csvService.readItemsFromCsv(NormalizedAddress.class,"url".getBytes(),1)).thenReturn(List.of(normalizedAddress));
        when(addressUtils.getCorrelationId(anyString())).thenReturn("id");
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId("id");
        batchRequest.setCorrelationId("id");
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus("id", BatchStatus.WORKING)).thenReturn(Mono.just(List.of(batchRequest)));
        when(addressBatchRequestService.updateBatchRequest(anyString(),any())).thenReturn(Mono.empty());
        when(addressUtils.getNormalizeRequestFromBatchRequest(any())).thenReturn(List.of(new NormalizeRequest()));
        PostelBatch postelBatch = new PostelBatch();
        postelBatch.setBatchId("id");
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
        when(addressUtils.toResultItem(any())).thenReturn(List.of(normalizeResult));
        when(addressUtils.toJson(anyString())).thenReturn("json");
        CapModel capModel = new CapModel();
        capModel.setCap("00010");
        capModel.setEndValidity(LocalDateTime.now());
        capModel.setStartValidity(LocalDateTime.now());
        StepVerifier.create(postelBatchService.getResponse("url", postelBatch)).expectError().verify();
    }


    @Test
    void getResponse2(){
        postelBatchService = new PostelBatchService(addressBatchRequestRepository, postelBatchRepository, csvService, addressUtils, uploadDownloadClient, addressBatchRequestService, capAndCountryService);

        when(uploadDownloadClient.downloadContent(anyString())).thenReturn(Mono.just("url".getBytes()));
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("id");
        when(csvService.readItemsFromCsv(NormalizedAddress.class,"url".getBytes(),1)).thenReturn(List.of(normalizedAddress));
        when(addressUtils.getCorrelationId(anyString())).thenReturn("id");
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setBatchId("id");
        batchRequest.setCorrelationId("id");
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus("id", BatchStatus.WORKING)).thenReturn(Mono.just(List.of(batchRequest)));
        when(addressBatchRequestService.updateBatchRequest(anyString(),any())).thenReturn(Mono.empty());
        when(addressUtils.getNormalizeRequestFromBatchRequest(any())).thenReturn(List.of(new NormalizeRequest()));
        PostelBatch postelBatch = new PostelBatch();
        postelBatch.setBatchId("id");
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
        when(addressUtils.toResultItem(any())).thenReturn(List.of(normalizeResult));
        when(addressUtils.toJson(anyString())).thenReturn("json");
        StepVerifier.create(postelBatchService.getResponse("url", postelBatch)).expectError().verify();
    }
}