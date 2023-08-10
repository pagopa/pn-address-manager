package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.client.PnSafeStorageClient;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadRequestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import static org.mockito.Mockito.*;

@ContextConfiguration (classes = {NormalizzatoreService.class})
@ExtendWith (SpringExtension.class)
class NormalizzatoreServiceTest {
	@MockBean
	private NormalizzatoreConverter normalizzatoreConverter;

	@Autowired
	private NormalizzatoreService normalizzatoreService;

	@MockBean
	private PnSafeStorageClient pnSafeStorageClient;

	@Test
	void testPresignedUploadRequest3 () {
		// Arrange
		PreLoadRequestData request = mock(PreLoadRequestData.class);
		when(request.getPreloads()).thenReturn(new ArrayList<>());

		// Act
		normalizzatoreService.presignedUploadRequest(request, "42 Main St");

		// Assert
		verify(request).getPreloads();
	}

	/**
	 * Method under test: {@link NormalizzatoreService#getFile(String, String)}
	 */
	@Test
	void testGetFile2 () {
		// Arrange
		when(pnSafeStorageClient.getFile(Mockito.<String>any(), Mockito.<String>any())).thenReturn(mock(Mono.class));

		// Act
		normalizzatoreService.getFile("File Key", "42 Main St");

		// Assert
		verify(pnSafeStorageClient).getFile(Mockito.<String>any(), Mockito.<String>any());
	}
}