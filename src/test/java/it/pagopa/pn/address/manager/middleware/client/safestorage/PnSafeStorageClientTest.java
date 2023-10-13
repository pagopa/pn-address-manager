package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

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
}