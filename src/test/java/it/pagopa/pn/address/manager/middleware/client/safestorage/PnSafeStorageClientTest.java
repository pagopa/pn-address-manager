package it.pagopa.pn.address.manager.middleware.client.safestorage;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

class PnSafeStorageClientTest {
    /**
     * Method under test: {@link PnSafeStorageClient#getFile(String, String)}
     */
    @Test
    void testGetFile() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.NullPointerException: Cannot invoke "it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.ApiClient.parameterToMultiValueMap(it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.ApiClient$CollectionFormat, String, Object)" because "this.apiClient" is null
        //       at it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileDownloadApi.getFileRequestCreation(FileDownloadApi.java:80)
        //       at it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileDownloadApi.getFile(FileDownloadApi.java:110)
        //       at it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient.getFile(PnSafeStorageClient.java:41)
        //   See https://diff.blue/R013 to resolve this issue.

        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        (new PnSafeStorageClient(new PnSafeStorageWebClient(responseExchangeFilter, new PnAddressManagerConfig())))
                .getFile("File Key", "42");
        verify(responseExchangeFilter, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction, atLeast(1)).apply(Mockito.<ExchangeFunction>any());
    }

    /**
     * Method under test: {@link PnSafeStorageClient#createFile(FileCreationRequestDto, String)}
     */
    @Test
    void testCreateFile() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.NullPointerException: Cannot invoke "it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.ApiClient.parameterToString(Object)" because "this.apiClient" is null
        //       at it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi.createFileRequestCreation(FileUploadApi.java:75)
        //       at it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi.createFile(FileUploadApi.java:103)
        //       at it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient.createFile(PnSafeStorageClient.java:61)
        //   See https://diff.blue/R013 to resolve this issue.

        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        PnSafeStorageClient pnSafeStorageClient = new PnSafeStorageClient(
                new PnSafeStorageWebClient(responseExchangeFilter, new PnAddressManagerConfig()));
        pnSafeStorageClient.createFile(new FileCreationRequestDto(), "42");
        verify(responseExchangeFilter, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2, atLeast(1)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction, atLeast(1)).apply(Mockito.<ExchangeFunction>any());
    }
}

