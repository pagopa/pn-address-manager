package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileDownloadApi;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {PnSafeStorageClient.class})
@ExtendWith(SpringExtension.class)
class PnSafeStorageClientTest {

    @MockBean
    private FileDownloadApi fileDownloadApi;

    @MockBean
    private FileUploadApi fileUploadApi;

    @Autowired
    private PnSafeStorageClient pnSafeStorageClient;

    @Test
    void testGetFile() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setSafeStorageBasePath("http://localhost:8080");

        PnSafeStorageWebClient pnSafeStorageWebClient = new PnSafeStorageWebClient();
        FileUploadApi fileUploadApi = pnSafeStorageWebClient.fileUploadApi(pnAddressManagerConfig);
        FileDownloadApi fileDownloadApi = pnSafeStorageWebClient.fileDownloadApi(pnAddressManagerConfig);
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        (new PnSafeStorageClient(fileUploadApi, fileDownloadApi))
                .getFile("File Key", "42");
        verify(responseExchangeFilter, atLeast(0)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2, atLeast(0)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction, atLeast(0)).apply(Mockito.<ExchangeFunction>any());
    }

    /**
     * Method under test: {@link PnSafeStorageClient#getFile(String, String)}
     */
    @Test
    void testGetFile2() throws WebClientResponseException {
        when(fileDownloadApi.getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any()))
                .thenReturn(Mono.just(new FileDownloadResponseDto()));
        StepVerifier.create(pnSafeStorageClient.getFile("File Key", "42"))
                .expectError();
    }

    /**
     * Method under test: {@link PnSafeStorageClient#getFile(String, String)}
     */
    @Test
    void testGetFile3() throws WebClientResponseException {
        when(fileDownloadApi.getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any()))
                .thenThrow(new WebClientResponseException(3, "Req params : {}", new HttpHeaders(),
                        new byte[]{'A', 3, 'A', 3, 'A', 3, 'A', 3}, null));
        assertThrows(WebClientResponseException.class, () -> pnSafeStorageClient.getFile("File Key", "42"));
        verify(fileDownloadApi).getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any());
    }

    /**
     * Method under test: {@link PnSafeStorageClient#getFile(String, String)}
     */
    @Test
    void testGetFile4() throws WebClientResponseException {
        when(fileDownloadApi.getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any()))
                .thenReturn(mock(Mono.class));
        pnSafeStorageClient.getFile("File Key", "42");
        verify(fileDownloadApi).getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any());
    }

    @Test
    void testCreateFile() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setSafeStorageBasePath("http://localhost:8080");

        PnSafeStorageWebClient pnSafeStorageWebClient = new PnSafeStorageWebClient();
        FileUploadApi fileUploadApi = pnSafeStorageWebClient.fileUploadApi(pnAddressManagerConfig);
        FileDownloadApi fileDownloadApi = pnSafeStorageWebClient.fileDownloadApi(pnAddressManagerConfig);
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        PnSafeStorageClient pnSafeStorageClient = new PnSafeStorageClient(fileUploadApi, fileDownloadApi);
        pnSafeStorageClient.createFile(new FileCreationRequestDto(), "42", "sha256");
        verify(responseExchangeFilter, atLeast(0)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2, atLeast(0)).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction, atLeast(0)).apply(Mockito.<ExchangeFunction>any());
    }

    /**
     * Method under test: {@link PnSafeStorageClient#createFile(FileCreationRequestDto, String, String)}
     */
    @Test
    void testCreateFile2() throws WebClientResponseException {

        when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequestDto>any())).thenReturn(Mono.just(new FileCreationResponseDto()));
        StepVerifier.create(pnSafeStorageClient.createFile(mock(FileCreationRequestDto.class), "42", "Sha256"))
                .expectError();
    }

    /**
     * Method under test: {@link PnSafeStorageClient#createFile(FileCreationRequestDto, String, String)}
     */
    @Test
    void testCreateFile3() throws WebClientResponseException {
        when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequestDto>any())).thenReturn(Mono.just(new FileCreationResponseDto()));

        FileCreationRequestDto fileCreationRequest = new FileCreationRequestDto();
        fileCreationRequest.contentType("text/plain");
        StepVerifier.create(pnSafeStorageClient.createFile(fileCreationRequest, "42", "Sha256"))
                .expectNextCount(1);
    }

    /**
     * Method under test: {@link PnSafeStorageClient#createFile(FileCreationRequestDto, String, String)}
     */
    @Test
    void testCreateFile4() throws WebClientResponseException {
        when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequestDto>any())).thenReturn(Mono.just(new FileCreationResponseDto()));

        FileCreationRequestDto fileCreationRequest = new FileCreationRequestDto();
        fileCreationRequest.contentType("Not all who wander are lost");
        pnSafeStorageClient.createFile(fileCreationRequest, "42", "Sha256");
        verify(fileUploadApi).createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequestDto>any());
    }

    /**
     * Method under test: {@link PnSafeStorageClient#createFile(FileCreationRequestDto, String, String)}
     */
    @Test
    void testCreateFile5() throws WebClientResponseException {
        when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequestDto>any())).thenReturn(mock(Mono.class));
        FileCreationRequestDto fileCreationRequest = mock(FileCreationRequestDto.class);
        when(fileCreationRequest.contentType(Mockito.<String>any())).thenReturn(new FileCreationRequestDto());
        fileCreationRequest.contentType("Not all who wander are lost");
        pnSafeStorageClient.createFile(fileCreationRequest, "42", "Sha256");
        verify(fileUploadApi).createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<FileCreationRequestDto>any());
        verify(fileCreationRequest).contentType(Mockito.<String>any());
    }
}