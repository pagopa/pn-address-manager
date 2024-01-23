package it.pagopa.pn.address.manager.middleware.client.safestorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.address.manager.generated.openapi.msclient.pn.safe.storage.v1.api.FileDownloadApi;
import it.pagopa.pn.address.manager.generated.openapi.msclient.pn.safe.storage.v1.api.FileUploadApi;
import it.pagopa.pn.address.manager.generated.openapi.msclient.pn.safe.storage.v1.dto.FileCreationRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@ContextConfiguration (classes = {PnSafeStorageClient.class})
@ExtendWith (SpringExtension.class)
class PnSafeStorageClientTest {
	@MockBean
	private FileDownloadApi fileDownloadApi;

	@MockBean
	private FileUploadApi fileUploadApi;

	@Autowired
	private PnSafeStorageClient pnSafeStorageClient;

	/**
	 * Method under test: {@link PnSafeStorageClient#getFile(String, String)}
	 */
	@Test
	void testGetFile () throws WebClientResponseException {
		// Arrange
		when(fileDownloadApi.getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any()))
				.thenThrow(new WebClientResponseException(2, "Req params : {}", new HttpHeaders(),
						new byte[]{'A', 2, 'A', 2, 'A', 2, 'A', 2}, null));

		// Act and Assert
		assertThrows(WebClientResponseException.class, () -> pnSafeStorageClient.getFile("File Key", "42"));
		verify(fileDownloadApi).getFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any());
	}

	/**
	 * Method under test: {@link PnSafeStorageClient#createFile(FileCreationRequestDto, String, String)}
	 */
	@Test
	void testCreateFile () throws WebClientResponseException {
		// Arrange
		when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
				Mockito.<FileCreationRequestDto>any())).thenReturn(mock(Mono.class));

		FileCreationRequestDto fileCreationRequest = new FileCreationRequestDto();
		fileCreationRequest.contentType("Not all who wander are lost");

		// Act
		pnSafeStorageClient.createFile(fileCreationRequest, "42", "Sha256");

		// Assert
		verify(fileUploadApi).createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
				Mockito.<FileCreationRequestDto>any());
	}

	/**
	 * Method under test: {@link PnSafeStorageClient#createFile(FileCreationRequestDto, String, String)}
	 */
	@Test
	void testCreateFile2 () throws WebClientResponseException {
		// Arrange
		when(fileUploadApi.createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
				Mockito.<FileCreationRequestDto>any())).thenReturn(mock(Mono.class));
		FileCreationRequestDto fileCreationRequest = mock(FileCreationRequestDto.class);
		when(fileCreationRequest.contentType(Mockito.<String>any())).thenReturn(new FileCreationRequestDto());
		fileCreationRequest.contentType("Not all who wander are lost");

		// Act
		pnSafeStorageClient.createFile(fileCreationRequest, "42", "Sha256");

		// Assert
		verify(fileCreationRequest).contentType(Mockito.<String>any());
		verify(fileUploadApi).createFile(Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(),
				Mockito.<FileCreationRequestDto>any());
	}
}
