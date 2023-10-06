package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.config.SchedulerConfig;
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

    @Test
    void presignedUploadRequest() {
        PreLoadRequestData requestData = new PreLoadRequestData();
        PreLoadResponseData preLoadResponseData = new PreLoadResponseData();
        when(normalizzatoreService.presignedUploadRequest(any(), any())).
                thenReturn(Mono.just(preLoadResponseData));
        StepVerifier.create(normalizeAddressController.presignedUploadRequest("cxId", "ApiKey",Mono.just(requestData), serverWebExchange))
              .expectNext(ResponseEntity.ok().body(preLoadResponseData));
    }

    @Test
    void getFile() {
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        when(normalizzatoreService.getFile(any(),any())).thenReturn(Mono.just(fileDownloadResponse));
        StepVerifier.create(normalizeAddressController.getFile("fileKey","cxId", "ApiKey", serverWebExchange))
                .expectNext(ResponseEntity.ok().body(fileDownloadResponse));
    }


    @Test
    void callbackNormalizedAddress() {
        CallbackResponseData response = new CallbackResponseData();
        when(normalizzatoreService.callbackNormalizedAddress(any(),any())).thenReturn(Mono.just(response));
        CallbackRequestData callbackRequestData = new CallbackRequestData();
        StepVerifier.create(normalizeAddressController.callbackNormalizedAddress("cxId", "ApiKey", Mono.just(callbackRequestData), serverWebExchange))
                .expectNext(ResponseEntity.ok().body(response));
    }


}
