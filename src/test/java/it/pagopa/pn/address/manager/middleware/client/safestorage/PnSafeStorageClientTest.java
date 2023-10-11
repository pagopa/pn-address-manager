package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileDownloadApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensaml.xmlsec.signature.P;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class PnSafeStorageClientTest {
    @Test
    void testGetFile() {

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setSafeStorageBasePath("http://localhost:8080");
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        (new PnSafeStorageClient(new PnSafeStorageWebClient(responseExchangeFilter, pnAddressManagerConfig)))
                .getFile("File Key", "42");
        verify(responseExchangeFilter, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction, atLeast(1)).apply(Mockito.<ExchangeFunction>any());
    }

    @Test
    void testCreateFile() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setSafeStorageBasePath("http://localhost:8080");
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        PnSafeStorageClient pnSafeStorageClient = new PnSafeStorageClient(
                new PnSafeStorageWebClient(responseExchangeFilter, pnAddressManagerConfig));
        pnSafeStorageClient.createFile(new FileCreationRequestDto(), "42", "sha256");
        verify(responseExchangeFilter, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction, atLeast(1)).apply(Mockito.<ExchangeFunction>any());
    }
    @Test
    @Disabled
    void testGetFileWithError () {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setSafeStorageBasePath("http://localhost:8080");
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        Mono<FileDownloadResponseDto> resultMono;
        resultMono=(new PnSafeStorageClient(new PnSafeStorageWebClient(responseExchangeFilter, pnAddressManagerConfig)))
                .getFile("File Key", "42");
        WebClientResponseException webClientResponseException = mock(WebClientResponseException.class);
        when(webClientResponseException.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        StepVerifier.create(resultMono)
                .expectError(PnAddressManagerException.class)
                .verify();
    }
}