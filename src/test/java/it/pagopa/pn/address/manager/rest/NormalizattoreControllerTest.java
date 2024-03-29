package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.service.ApiKeyUtils;
import it.pagopa.pn.address.manager.service.NormalizzatoreService;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NormalizzatoreController.class, SchedulerConfig.class})
class NormalizattoreControllerTest {

    @Autowired
    NormalizzatoreController normalizeAddressController;

    @MockBean
    private NormalizzatoreService normalizzatoreService;

    @MockBean
    private Scheduler scheduler;

    @MockBean
    ServerWebExchange serverWebExchange;

    @MockBean
    ApiKeyUtils apiKeyUtils;

    @Test
    void presignedUploadRequest() {
        PreLoadRequestData requestData = new PreLoadRequestData();
        PreLoadResponseData preLoadResponseData = new PreLoadResponseData();
        when(normalizzatoreService.presignedUploadRequest(any(), any(), any())).
                thenReturn(Mono.just(preLoadResponseData));
        StepVerifier.create(normalizeAddressController.presignedUploadRequest("cxId", "ApiKey",Mono.just(requestData), serverWebExchange))
              .expectNext(ResponseEntity.ok().body(preLoadResponseData));
    }

    @Test
    void getFile() {
        FileDownloadResponse fileDownloadResponse = mock(FileDownloadResponse.class);
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey("ApiKey");
        apiKeyModel.setCxId("cxId");
        when(apiKeyUtils.checkPostelApiKey(any(), any())).thenReturn(Mono.just(apiKeyModel));
        when(normalizzatoreService.getFile(any())).thenReturn(Mono.just(fileDownloadResponse));
        StepVerifier.create(normalizeAddressController.getFile("fileKey","cxId","ApiKey",serverWebExchange))
                .expectNext(ResponseEntity.ok().body(fileDownloadResponse));
    }

    @Test
    void normalizerCallback(){
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        when(normalizzatoreService.callbackNormalizedAddress(any(),any(),any())).thenReturn(Mono.just(operationResultCodeResponse));
        StepVerifier.create(normalizeAddressController.normalizerCallback("fileKey","cxId", Mono.just(new NormalizerCallbackRequest()), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(operationResultCodeResponse));
    }
}
