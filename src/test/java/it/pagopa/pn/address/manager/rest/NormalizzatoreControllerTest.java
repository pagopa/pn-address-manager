package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.service.NormalizzatoreService;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadRequestData;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadResponseData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith (SpringExtension.class)
@ContextConfiguration (classes = {NormalizzatoreController.class, SchedulerConfig.class})
class NormalizzatoreControllerTest {
	@Autowired
	private NormalizzatoreController normalizzatoreController;
	@MockBean
	private NormalizzatoreService normalizzatoreService;
	@Test
	void presignedUploadRequestTest() {
		PreLoadRequestData preLoadRequestData = mock(PreLoadRequestData.class);
		PreLoadResponseData preLoadResponseData = mock(PreLoadResponseData.class);
		when(normalizzatoreService.presignedUploadRequest(preLoadRequestData,"cxId")).thenReturn(Mono.just(preLoadResponseData));
		StepVerifier.create(normalizzatoreController.presignedUploadRequest("cxId", "ApiKey", Mono.just(preLoadRequestData), mock(ServerWebExchange.class)))
				.expectNext(ResponseEntity.ok().body(preLoadResponseData))
				.verifyComplete();
	}
	@Test
	void getFileTest() {
		FileDownloadResponse fileDownloadResponse = mock(FileDownloadResponse.class);
		when(normalizzatoreService.getFile(anyString(),anyString())).thenReturn(Mono.just(fileDownloadResponse));
		StepVerifier.create(normalizzatoreController.getFile("cxId", "ApiKey", "fileId", mock(ServerWebExchange.class)))
				.expectNext(ResponseEntity.ok().body(fileDownloadResponse))
				.verifyComplete();
	}
}

