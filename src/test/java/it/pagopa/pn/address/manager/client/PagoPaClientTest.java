package it.pagopa.pn.address.manager.client;

import it.pagopa.pn.address.manager.model.deduplica.DeduplicaRequest;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith (SpringExtension.class)
class PagoPaClientTest {
	@MockBean
	private WebClient webClient;
	@MockBean
	private PagoPaWebClient pagoPaWebClient;
	/*@Test
	void testDeduplicaOnline () {
		when(pagoPaWebClient.init()).thenReturn(webClient);
		PagoPaClient pagoPaClient = new PagoPaClient(pagoPaWebClient);

		DeduplicaRequest deduplicaRequest = mock(DeduplicaRequest.class);
		DeduplicaResponse deduplicaResponseMock = mock(DeduplicaResponse.class);
		when(deduplicaRequest.getConfigurazioneDeduplica()).thenReturn("configurazioneDeduplica");

		WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
		WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri("/PagoPaDeduplica/rest/")).thenReturn(requestBodySpec);
		when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
		when(requestBodySpec.bodyValue(any(BodyInserter.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(DeduplicaResponse.class)).thenReturn(Mono.just(deduplicaResponseMock));

		StepVerifier.create(pagoPaClient.deduplicaOnline(deduplicaRequest))
				.expectNext(deduplicaResponseMock)
				.verifyComplete();
	}*/

}

