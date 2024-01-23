package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.exception.PnFileNotFoundException;
import it.pagopa.pn.address.manager.generated.openapi.msclient.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.generated.openapi.msclient.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.model.PostelCallbackSqsDto;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class NormalizzatoreServiceTest {
    @MockBean PnSafeStorageClient pnSafeStorageClient;
    @MockBean NormalizzatoreConverter normalizzatoreConverter;
    @MockBean
    NormalizzatoreBatchService normalizzatoreBatchService;
    @MockBean SqsService sqsService;
    @MockBean SafeStorageService safeStorageService;
    @MockBean PostelBatchRepository postelBatchRepository;
    @MockBean ApiKeyUtils apiKeyUtils;
    @MockBean AddressUtils addressUtils;
    @MockBean PnAddressManagerConfig pnAddressManagerConfig;

    NormalizzatoreService normalizzatoreService;

    @BeforeEach
    void setUp(){
        normalizzatoreService = new NormalizzatoreService(pnSafeStorageClient, normalizzatoreConverter, normalizzatoreBatchService,sqsService, safeStorageService, postelBatchRepository,  apiKeyUtils, addressUtils, pnAddressManagerConfig);
    }

    @Test
    void presignedUploadRequest(){
        PreLoadRequestData preLoadRequestData = new PreLoadRequestData();
        PreLoadRequest preLoadRequest = new PreLoadRequest();
        preLoadRequest.setSha256("sha256");
        preLoadRequest.setContentType("application/pdf");
        preLoadRequest.setPreloadIdx("id");
        preLoadRequestData.setPreloads(List.of(preLoadRequest));
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setCxId("id");
        apiKeyModel.setApiKey("id");
        when(apiKeyUtils.checkPostelApiKey(anyString(), anyString())).thenReturn(Mono.just(apiKeyModel));
        when(normalizzatoreConverter.preLoadRequestToFileCreationRequestDto(any())).thenReturn(new FileCreationRequestDto());
        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.setSecret("secret");
        fileCreationResponseDto.setKey("key");
        fileCreationResponseDto.setUploadUrl("url");
        fileCreationResponseDto.setUploadMethod(FileCreationResponseDto.UploadMethodEnum.PUT);
        when(pnSafeStorageClient.createFile(any(),anyString(), any())).thenReturn(Mono.just(fileCreationResponseDto));
        StepVerifier.create(normalizzatoreService.presignedUploadRequest(preLoadRequestData, "id","id")).expectError().verify();
    }

    @Test
    void callbackNormalizedAddress(){
        NormalizerCallbackRequest normalizerCallbackRequest = new NormalizerCallbackRequest();
        normalizerCallbackRequest.setRequestId("id");
        normalizerCallbackRequest.setSha256("sha256");
        normalizerCallbackRequest.setUri("uri");
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setCxId("id");
        apiKeyModel.setApiKey("id");
        when(apiKeyUtils.checkPostelApiKey(anyString(), anyString())).thenReturn(Mono.just(apiKeyModel));
        when(normalizzatoreBatchService.findPostelBatch(anyString())).thenReturn(Mono.just(new NormalizzatoreBatch()));
        FileDownloadResponse fileDownloadResponse = mock(FileDownloadResponse.class);
        when(safeStorageService.getFile(anyString(),anyString())).thenReturn(Mono.just(fileDownloadResponse));
        when(postelBatchRepository.update(any()))
                .thenReturn(Mono.just(new NormalizzatoreBatch()));
        when(safeStorageService.getFile("fileKey",pnAddressManagerConfig.getPagoPaCxId()))
                .thenReturn(Mono.just(fileDownloadResponse));
        when(sqsService.pushToCallbackQueue(any()))
                .thenReturn(Mono.just(SendMessageResponse.builder().build()));
        when(postelBatchRepository.update(any()))
                .thenReturn(Mono.just(new NormalizzatoreBatch()));
        StepVerifier.create(normalizzatoreService.callbackNormalizedAddress(normalizerCallbackRequest,"id","id")).expectError().verify();
    }

    @Test
    void callbackNormalizedAddress1(){
        NormalizerCallbackRequest normalizerCallbackRequest = new NormalizerCallbackRequest();
        normalizerCallbackRequest.setRequestId("id");
        normalizerCallbackRequest.setSha256("sha256");
        normalizerCallbackRequest.setUri("uri");
        normalizerCallbackRequest.setError("error");
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setCxId("id");
        apiKeyModel.setApiKey("id");
        when(apiKeyUtils.checkPostelApiKey(anyString(), anyString())).thenReturn(Mono.just(apiKeyModel));
        when(normalizzatoreBatchService.findPostelBatch(anyString())).thenReturn(Mono.just(new NormalizzatoreBatch()));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        when(safeStorageService.getFile(anyString(),anyString())).thenReturn(Mono.just(fileDownloadResponse));
        PostelCallbackSqsDto postelCallbackSqsDto = mock(PostelCallbackSqsDto.class);
        when(addressUtils.getPostelCallbackSqsDto(any(),anyString())).thenReturn(postelCallbackSqsDto);
        when(sqsService.pushToCallbackQueue(any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        StepVerifier.create(normalizzatoreService.callbackNormalizedAddress(normalizerCallbackRequest,"id","id")).expectError().verify();
    }
    @Test
    void getFileTest(){
        FileDownloadResponse fileDownloadResponse=mock(FileDownloadResponse.class);
        when(safeStorageService.getFile("fileKey",pnAddressManagerConfig.getPagoPaCxId()))
                .thenReturn(Mono.just(fileDownloadResponse));
        StepVerifier.create(normalizzatoreService.getFile("fileKey"))
                .expectNext(fileDownloadResponse)
                .verifyComplete();
    }
    @Test
    void getFileErrorTest() {
        when(safeStorageService.getFile("fileKey", pnAddressManagerConfig.getPagoPaCxId()))
                .thenReturn(Mono.error(new PnFileNotFoundException("", new RuntimeException())));
        StepVerifier.create(normalizzatoreService.getFile("fileKey"))
                .expectError()
                .verify();
    }

}
