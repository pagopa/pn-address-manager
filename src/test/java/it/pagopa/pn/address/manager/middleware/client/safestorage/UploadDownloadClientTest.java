package it.pagopa.pn.address.manager.middleware.client.safestorage;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import org.junit.jupiter.api.Assertions;
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

import java.util.Objects;

@ContextConfiguration (classes = {UploadDownloadClient.class})
@ExtendWith (SpringExtension.class)
class UploadDownloadClientTest {
	@Autowired
	private UploadDownloadClient uploadDownloadClient;
	@Test
	void testUploadContent () {
		FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
		when(fileCreationResponse.getSecret()).thenReturn("Secret");
		when(fileCreationResponse.getUploadUrl()).thenReturn("https://example.org/example");
		when(fileCreationResponse.getKey()).thenReturn("key");
		uploadDownloadClient.uploadContent("Not all who wander are lost", fileCreationResponse, "Sha256");
		verify(fileCreationResponse).getSecret();
	}
	@Test
	void testUploadContent2 () {
		FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
		when(fileCreationResponse.getSecret()).thenReturn("Secret");
		when(fileCreationResponse.getUploadUrl()).thenReturn("http://localhost:8080");
		when(fileCreationResponse.getKey()).thenReturn("key");
		uploadDownloadClient.uploadContent("Not all who wander are lost", fileCreationResponse, "Sha256");
		verify(fileCreationResponse).getSecret();
	}

	/**
	 * Method under test: {@link UploadDownloadClient#uploadContent(String, FileCreationResponseDto, String)}
	 */
	@Test
	void testUploadContent3 () {
		FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
		when(fileCreationResponse.getUploadUrl()).thenReturn("http://localhost:8080");
		when(fileCreationResponse.getSecret()).thenThrow(
				new PnAddressManagerException("", "The characteristics of someone or something", 2, "An error occurred"));
		Assertions.assertThrows(PnAddressManagerException.class, () -> uploadDownloadClient.uploadContent("Not all who wander are lost", fileCreationResponse, "Sha256"));
		verify(fileCreationResponse).getSecret();
	}

	@Test
	void testDownloadContent () {

		uploadDownloadClient.downloadContent("https://example.org/example");
	}
	@Test
	void testDownloadContentWithError () {
		UploadDownloadClient uploadDownloadClient = new UploadDownloadClient();
		WebClientResponseException webClientResponseException = mock(WebClientResponseException.class);
		when(webClientResponseException.getMessage()).thenReturn("Error message");
		when(webClientResponseException.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		Mono<byte[]> resultMono = uploadDownloadClient.downloadContent("http://localhost:8080");
		StepVerifier.create(resultMono)
				.expectError(PnAddressManagerException.class)
				.verify();
	}
	@Test
	void testUploadContentWithError () {
		UploadDownloadClient uploadDownloadClient = new UploadDownloadClient();
        FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
		when(fileCreationResponse.getKey()).thenReturn("key");
		when(fileCreationResponse.getUploadUrl()).thenReturn("http://localhost:8080");
		WebClientResponseException webClientResponseException = mock(WebClientResponseException.class);
		when(webClientResponseException.getMessage()).thenReturn("Error message");
		when(webClientResponseException.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		Mono<String> resultMono = uploadDownloadClient
				.uploadContent(Objects.requireNonNull
						(webClientResponseException.getMessage()), fileCreationResponse, null);
		StepVerifier.create(resultMono)
				.expectError(PnAddressManagerException.class)
				.verify();
	}
}

