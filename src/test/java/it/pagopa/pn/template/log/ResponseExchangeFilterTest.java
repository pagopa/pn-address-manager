package it.pagopa.pn.template.log;

import it.pagopa.pn.template.exception.PnAddressManagerException;

import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.UnknownHttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {ResponseExchangeFilter.class})
@ExtendWith(MockitoExtension.class)
class ResponseExchangeFilterTest {

    @InjectMocks
    ResponseExchangeFilter responseExchangeFilter;

    @Test
    @DisplayName("Should log the response status code")
    void logResponseBodyShouldLogTheResponseStatusCode() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost:8080/test")).build();
        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        long startTime = System.currentTimeMillis();

        responseExchangeFilter.logResponseBody(startTime, "test", response, request);

        Assertions.assertEquals(HttpStatus.OK, response.statusCode());
    }

    /**
     * Method under test: {@link ResponseExchangeFilter#logResponseBody(long, WebClientResponseException, ClientRequest)}
     */
    @Test
    void testLogResponseBody4() {
        UnknownHttpStatusCodeException unknownHttpStatusCodeException = mock(UnknownHttpStatusCodeException.class);
        when(unknownHttpStatusCodeException.getResponseBodyAsString()).thenReturn("Not all who wander are lost");
        when(unknownHttpStatusCodeException.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
        ClientRequest clientRequest = mock(ClientRequest.class);
        when(clientRequest.url()).thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        responseExchangeFilter.logResponseBody(1L, unknownHttpStatusCodeException, clientRequest);
        verify(unknownHttpStatusCodeException).getResponseBodyAsString();
        verify(unknownHttpStatusCodeException, atLeast(1)).getStatusCode();
        verify(clientRequest).url();
    }

    /**
     * Method under test: {@link ResponseExchangeFilter#logResponseBody(long, WebClientResponseException, ClientRequest)}
     */
    @Test
    void testLogResponseBody5() {
        UnknownHttpStatusCodeException unknownHttpStatusCodeException = mock(UnknownHttpStatusCodeException.class);
        when(unknownHttpStatusCodeException.getResponseBodyAsString())
                .thenThrow(new WebClientResponseException(5, "Response HTTP from {} {} {} - body: {} - timelapse: {}ms",
                        new HttpHeaders(), new byte[]{'A', 5, 'A', 5, 'A', 5, 'A', 5}, null));
        when(unknownHttpStatusCodeException.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
        ClientRequest clientRequest = mock(ClientRequest.class);
        when(clientRequest.url()).thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        assertThrows(WebClientResponseException.class,
                () -> responseExchangeFilter.logResponseBody(1L, unknownHttpStatusCodeException, clientRequest));
        verify(unknownHttpStatusCodeException).getResponseBodyAsString();
        verify(unknownHttpStatusCodeException, atLeast(1)).getStatusCode();
        verify(clientRequest).url();
    }

    @Test
    @DisplayName("Should log the response body")
    void logResponseBodyShouldLogTheResponseBody() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost:8080/test")).build();
        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        ExchangeFunction exchangeFunction = clientRequest -> Mono.just(response);

        Mono<ClientResponse> clientResponseMono = responseExchangeFilter.filter(request, exchangeFunction);

        StepVerifier.create(clientResponseMono)
                .expectNextMatches(clientResponse -> clientResponse.statusCode().equals(HttpStatus.OK))
                .verifyComplete();
    }

    @Test
    void logResponseInCaseOfError() {
        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost:8080/test")).build();
        ExchangeFunction exchangeFunction = clientRequest -> Mono.error(new PnAddressManagerException("", HttpStatus.CREATED));

        Mono<ClientResponse> clientResponseMono = responseExchangeFilter.filter(request, exchangeFunction);

        StepVerifier.create(clientResponseMono)
                .expectError(PnAddressManagerException.class)
                .verify();
    }

    @Test
    @DisplayName("Should log the request body")
    void logRequestBodyShouldLogTheRequestBody() {
        ClientRequest clientRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost:8080/test")).build();
        ExchangeFunction exchangeFunction = cr -> Mono.just(ClientResponse.create(HttpStatus.OK).build());

        DataBuffer dataBuffer = mock(DataBuffer.class);
        when(dataBuffer.toString(StandardCharsets.UTF_8)).thenReturn("test");
        responseExchangeFilter.logRequestBody(dataBuffer, clientRequest);

        StepVerifier.create(responseExchangeFilter.filter(clientRequest, exchangeFunction))
                .expectNextMatches(clientResponse -> clientResponse.statusCode().equals(HttpStatus.OK))
                .verifyComplete();
    }

    @Test
    void filter() {
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK).body("response").build();
        ExchangeFunction exchangeFunction = clientRequest -> Mono.just(clientResponse);
        ClientRequest clientRequest = ClientRequest.create(HttpMethod.POST, URI.create("test")).build();
        StepVerifier.create(responseExchangeFilter.filter(clientRequest, exchangeFunction))
                .expectNextMatches(response -> clientResponse.statusCode().is2xxSuccessful()).verifyComplete();
    }
}
