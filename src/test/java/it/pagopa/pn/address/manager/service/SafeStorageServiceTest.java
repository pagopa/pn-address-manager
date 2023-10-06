package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PnSafeStorageException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.model.NormalizeRequestPostelInput;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SafeStorageServiceTest {

    SafeStorageService safeStorageService;

    @MockBean CsvService csvService;
    @MockBean PnSafeStorageClient pnSafeStorageClient;
    @MockBean UploadDownloadClient uploadDownloadClient;

    @MockBean AddressUtils addressUtils;

    @MockBean PnAddressManagerConfig pnAddressManagerConfig;
    @MockBean NormalizzatoreConverter normalizzatoreConverter;

    @Test
    void callSelfStorageCreateFileAndUpload(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setPagoPaCxId("cxId");
        safeStorageService = new SafeStorageService(csvService, pnSafeStorageClient, uploadDownloadClient, addressUtils, pnAddressManagerConfig, normalizzatoreConverter);
        BatchRequest batchRequest = getBatchRequest();
        when(addressUtils.getFileCreationRequest()).thenReturn(new FileCreationRequestDto());
        when(addressUtils.normalizeRequestToPostelCsvRequest(any())).thenReturn(List.of(new NormalizeRequestPostelInput()));
        when(csvService.writeItemsOnCsvToString(any())).thenReturn("csv");
        when(addressUtils.computeSha256(any())).thenReturn("csv");
        when(pnSafeStorageClient.createFile(any(),any())).thenReturn(Mono.just(new FileCreationResponseDto()));
        when(uploadDownloadClient.uploadContent(any(),any(),any())).thenReturn(Mono.just("csv"));

        StepVerifier.create(safeStorageService.callSelfStorageCreateFileAndUpload(List.of(batchRequest))).expectNext(new FileCreationResponseDto()).verifyComplete();
    }

    @Test
    void getFile(){
        safeStorageService = new SafeStorageService(csvService, pnSafeStorageClient, uploadDownloadClient, addressUtils, pnAddressManagerConfig, normalizzatoreConverter);
        when(pnSafeStorageClient.getFile(any(),any())).thenReturn(Mono.just(new FileDownloadResponseDto()));
        when(normalizzatoreConverter.fileDownloadResponseDtoToFileDownloadResponse(any())).thenReturn(new FileDownloadResponse());
        StepVerifier.create(safeStorageService.getFile("file","cx")).expectNext(new FileDownloadResponse()).verifyComplete();
    }

    @Test
    void callSelfStorageCreateFileAndUploadError(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setPagoPaCxId("cxId");
        safeStorageService = new SafeStorageService(csvService, pnSafeStorageClient, uploadDownloadClient, addressUtils, pnAddressManagerConfig, normalizzatoreConverter);
        BatchRequest batchRequest = getBatchRequest();
        when(addressUtils.getFileCreationRequest()).thenReturn(new FileCreationRequestDto());
        when(addressUtils.normalizeRequestToPostelCsvRequest(any())).thenReturn(List.of(new NormalizeRequestPostelInput()));
        when(csvService.writeItemsOnCsvToString(any())).thenReturn("csv");
        when(addressUtils.computeSha256(any())).thenReturn("csv");
        when(pnSafeStorageClient.createFile(any(),any())).thenReturn(Mono.just(new FileCreationResponseDto()));
        PnSafeStorageException exception = mock(PnSafeStorageException.class);
        when(uploadDownloadClient.uploadContent(any(),any(),any())).thenThrow(exception);

        StepVerifier.create(safeStorageService.callSelfStorageCreateFileAndUpload(List.of(batchRequest))).expectError().verify();
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