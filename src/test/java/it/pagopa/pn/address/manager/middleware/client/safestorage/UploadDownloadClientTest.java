package it.pagopa.pn.address.manager.middleware.client.safestorage;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {UploadDownloadClient.class})
@ExtendWith(SpringExtension.class)
class UploadDownloadClientTest {
    @Autowired
    private UploadDownloadClient uploadDownloadClient;

    /**
     * Method under test: {@link UploadDownloadClient#uploadContent(String, FileCreationResponseDto, String)}
     */
    @Test
    void testUploadContent() {
        FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
        when(fileCreationResponse.getSecret()).thenReturn("Secret");
        when(fileCreationResponse.getUploadUrl()).thenReturn("https://example.org/example");
        uploadDownloadClient.uploadContent("Not all who wander are lost", fileCreationResponse, "Sha256");
        verify(fileCreationResponse).getSecret();
        verify(fileCreationResponse).getUploadUrl();
    }

    /**
     * Method under test: {@link UploadDownloadClient#uploadContent(String, FileCreationResponseDto, String)}
     */
    @Test
    void testUploadContent2() {
        FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
        when(fileCreationResponse.getSecret()).thenReturn("Secret");
        when(fileCreationResponse.getUploadUrl()).thenReturn("U://U@[9U]:{UU?U#U");
        uploadDownloadClient.uploadContent("Not all who wander are lost", fileCreationResponse, "Sha256");
        verify(fileCreationResponse).getSecret();
        verify(fileCreationResponse).getUploadUrl();
    }

    /**
     * Method under test: {@link UploadDownloadClient#uploadContent(String, FileCreationResponseDto, String)}
     */
    @Test
    void testUploadContent3() {
        FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
        when(fileCreationResponse.getSecret()).thenThrow(
                new PnAddressManagerException("", "The characteristics of someone or something", 2, "An error occurred"));
        uploadDownloadClient.uploadContent("Not all who wander are lost", fileCreationResponse, "Sha256");
        verify(fileCreationResponse).getSecret();
    }

    @Test
    void testDownloadContent() {

        uploadDownloadClient.downloadContent("https://example.org/example");
    }

    @Test
    void testDownloadContentWithError() {
        UploadDownloadClient uploadDownloadClient = new UploadDownloadClient() ; // Crea un'istanza della tua classe

        // Simula un errore durante il download restituendo un errore WebClientResponseException
        WebClientResponseException errorResponse = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                null,
                null,
                null
        );

        // Passa l'URL di download al metodo downloadContent e simula un errore
        Mono<byte[]> resultMono = uploadDownloadClient.downloadContent(errorResponse.getMessage());

        // Verifica che il risultato sia un errore di tipo PnAddressManagerException
        StepVerifier.create(resultMono)
                .expectErrorMatches(ex -> {
                    return ex instanceof PnAddressManagerException &&
                            ex.getMessage().equals(ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION) &&
                            ((PnAddressManagerException)ex).getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value() &&
                            ((PnAddressManagerException)ex).getMessage().equals(ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE);
                })
                .verify();
    }
}

