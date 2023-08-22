package it.pagopa.pn.address.manager.client;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaRequest;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaResponse;
import it.pagopa.pn.address.manager.model.deduplica.InputDeduplica;
import it.pagopa.pn.address.manager.model.deduplica.RisultatoDeduplica;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith (SpringExtension.class)
@ContextConfiguration (classes = {PagoPaClient.class})
class PagoPaClientTest {
	@MockBean
	private WebClient webClient;
	@MockBean
	private PagoPaWebClient pagoPaWebClient;
	@Test
	void testDeduplicaOnline () {
		when(pagoPaWebClient.init()).thenReturn(webClient);
		PagoPaClient pagoPaClient = new PagoPaClient(pagoPaWebClient);

		InputDeduplica inputDeduplica = mock(InputDeduplica.class);
		RisultatoDeduplica risultatoDeduplica = mock(RisultatoDeduplica.class);

		WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
		WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri("/normalizzaRest")).thenReturn(requestBodySpec);
		when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
		when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(RisultatoDeduplica.class)).thenReturn(Mono.just(risultatoDeduplica));

		StepVerifier.create(pagoPaClient.deduplicaOnline(inputDeduplica))
				.expectNext(risultatoDeduplica)
				.verifyComplete();
	}
	@Test
	void testDeduplicaOnlineDoOnError() {
		when(pagoPaWebClient.init()).thenReturn(webClient);
		PagoPaClient pagoPaClient = new PagoPaClient(pagoPaWebClient);

		HttpHeaders headers = mock(HttpHeaders.class);
		byte[] testByteArray = new byte[0];
		String test = "test";
		WebClientResponseException webClientResponseException = new WebClientResponseException(test, HttpStatus.BAD_REQUEST.value(), test, headers, testByteArray, Charset.defaultCharset());

		WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
		WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
		InputDeduplica inputDeduplica = mock(InputDeduplica.class);

		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri("/normalizzaRest")).thenReturn(requestBodySpec);
		when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
		when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(RisultatoDeduplica.class)).thenReturn(Mono.error(webClientResponseException));

		StepVerifier.create(pagoPaClient.deduplicaOnline(inputDeduplica))
				.verifyError(PnAddressManagerException.class);
	}
	@Test
	void testActivateSINIComponent () {
		when(pagoPaWebClient.init()).thenReturn(webClient);
		PagoPaClient pagoPaClient = new PagoPaClient(pagoPaWebClient);

		String soapResp = "soapResp";
		WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
		WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri("/SINI/WcfSiniService.svc")).thenReturn(requestBodySpec);
		when(requestBodySpec.contentType(MediaType.TEXT_XML)).thenReturn(requestBodySpec);
		when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(soapResp));

		StepVerifier.create(pagoPaClient.activateSINIComponent(""))
				.expectNext(soapResp)
				.verifyComplete();
	}
	@Test
	void testActivateSINIComponentDoOnError() {
		when(pagoPaWebClient.init()).thenReturn(webClient);
		PagoPaClient pagoPaClient = new PagoPaClient(pagoPaWebClient);

		HttpHeaders headers = mock(HttpHeaders.class);
		byte[] testByteArray = new byte[0];
		String test = "test";
		WebClientResponseException webClientResponseException = new WebClientResponseException(test, HttpStatus.BAD_REQUEST.value(), test, headers, testByteArray, Charset.defaultCharset());

		WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
		WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
		WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

		when(webClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri("/SINI/WcfSiniService.svc")).thenReturn(requestBodySpec);
		when(requestBodySpec.contentType(MediaType.TEXT_XML)).thenReturn(requestBodySpec);
		when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(webClientResponseException));

		StepVerifier.create(pagoPaClient.activateSINIComponent(""))
				.verifyError(PnAddressManagerException.class);
	}
}

