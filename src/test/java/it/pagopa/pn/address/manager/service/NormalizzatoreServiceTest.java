package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.model.PostelCallbackSqsDto;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.NormalizerCallbackRequest;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadRequest;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadRequestData;
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
    @MockBean PostelBatchService postelBatchService;
    @MockBean SqsService sqsService;
    @MockBean SafeStorageService safeStorageService;
    @MockBean PostelBatchRepository postelBatchRepository;
    @MockBean ApiKeyRepository apiKeyRepository;
    @MockBean AddressUtils addressUtils;
    @MockBean PnAddressManagerConfig pnAddressManagerConfig;

    NormalizzatoreService normalizzatoreService;

    @BeforeEach
    void setUp(){
        normalizzatoreService = new NormalizzatoreService(pnSafeStorageClient, normalizzatoreConverter, postelBatchService,sqsService, safeStorageService, postelBatchRepository, apiKeyRepository, addressUtils, pnAddressManagerConfig);
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
        when(apiKeyRepository.findById(anyString())).thenReturn(Mono.just(apiKeyModel));
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
        when(apiKeyRepository.findById(anyString())).thenReturn(Mono.just(apiKeyModel));
        when(postelBatchService.findPostelBatch(anyString())).thenReturn(Mono.just(new PostelBatch()));
        FileDownloadResponse fileDownloadResponse = mock(FileDownloadResponse.class);
        when(safeStorageService.getFile(anyString(),anyString())).thenReturn(Mono.just(fileDownloadResponse));
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
        when(apiKeyRepository.findById(anyString())).thenReturn(Mono.just(apiKeyModel));
        when(postelBatchService.findPostelBatch(anyString())).thenReturn(Mono.just(new PostelBatch()));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        when(safeStorageService.getFile(anyString(),anyString())).thenReturn(Mono.just(fileDownloadResponse));
        PostelCallbackSqsDto postelCallbackSqsDto = mock(PostelCallbackSqsDto.class);
        when(addressUtils.getPostelCallbackSqsDto(any(),anyString(), anyString())).thenReturn(postelCallbackSqsDto);
        when(sqsService.pushToCallbackQueue(any(),anyString(), any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        StepVerifier.create(normalizzatoreService.callbackNormalizedAddress(normalizerCallbackRequest,"id","id")).expectError().verify();
    }
}
