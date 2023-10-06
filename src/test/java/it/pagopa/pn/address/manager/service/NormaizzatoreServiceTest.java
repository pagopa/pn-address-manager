package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class NormaizzatoreServiceTest {

    private NormalizzatoreService normalizzatoreService;

    @MockBean
    PnSafeStorageClient pnSafeStorageClient;
    @MockBean
    NormalizzatoreConverter normalizzatoreConverter;
    @MockBean
    PostelBatchService postelBatchService;
    @MockBean
    SqsService sqsService;
    @MockBean
    SafeStorageService safeStorageService;
    @MockBean
    PostelBatchRepository postelBatchRepository;

    @Test
    void presignedUploadRequest(){
        normalizzatoreService = new NormalizzatoreService(pnSafeStorageClient,normalizzatoreConverter,postelBatchService,sqsService,safeStorageService,postelBatchRepository);

        PreLoadRequestData preLoadRequest = new PreLoadRequestData();
        PreLoadRequest preLoadRequest1 = new PreLoadRequest();
        preLoadRequest1.setContentType("application/pdf");
        preLoadRequest1.setPreloadIdx("pre");
        preLoadRequest1.setSha256("sha256");
        preLoadRequest.setPreloads(List.of(preLoadRequest1));
        when(normalizzatoreConverter.preLoadRequestToFileCreationRequestDto(any())).thenReturn(new FileCreationRequestDto());
        when(pnSafeStorageClient.createFile(any(),any())).thenReturn(Mono.just(new FileCreationResponseDto()));
        when(normalizzatoreConverter.fileDownloadResponseDtoToFileDownloadResponse(any(), any())).thenReturn(new PreLoadResponse());
        when(normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(any())).thenReturn(new PreLoadResponseData());
        StepVerifier.create(normalizzatoreService.presignedUploadRequest(preLoadRequest,"cxId")).expectNext(new PreLoadResponseData()).verifyComplete();
    }

    @Test
    void callbackNormalizedAddress(){
        normalizzatoreService = new NormalizzatoreService(pnSafeStorageClient,normalizzatoreConverter,postelBatchService,sqsService,safeStorageService,postelBatchRepository);

        when(postelBatchService.findPostelBatch(any())).thenReturn(Mono.just(new PostelBatch()));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("check");
        when(safeStorageService.getFile(any(),any())).thenReturn(Mono.just(fileDownloadResponse));

        CallbackResponseData callbackResponseData = new CallbackResponseData();
        callbackResponseData.setResponse(CallbackResponseData.ResponseEnum.KO);
        StepVerifier.create(normalizzatoreService.callbackNormalizedAddress(new CallbackRequestData(),"cxId")).expectNext(callbackResponseData).verifyComplete();
    }

    @Test
    void callbackNormalizedAddress1(){
        normalizzatoreService = new NormalizzatoreService(pnSafeStorageClient,normalizzatoreConverter,postelBatchService,sqsService,safeStorageService,postelBatchRepository);

        when(postelBatchService.findPostelBatch(any())).thenReturn(Mono.just(new PostelBatch()));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        when(safeStorageService.getFile(any(),any())).thenReturn(Mono.just(fileDownloadResponse));

        CallbackRequestData callbackRequestData = new CallbackRequestData();
        callbackRequestData.setFileKeyInput("file");
        callbackRequestData.setFileKeyOutput("file");
        when(postelBatchRepository.update(any())).thenReturn(Mono.just(new PostelBatch()));
        when(sqsService.pushToInputQueue(any(),any())).thenReturn(Mono.just(SendMessageResponse.builder().messageId("message").build()));
        StepVerifier.create(normalizzatoreService.callbackNormalizedAddress(callbackRequestData,"cxId")).expectNext(new CallbackResponseData()).verifyComplete();
    }
}