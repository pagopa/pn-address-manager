package it.pagopa.pn.address.manager.client;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnSafeStorageException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileDownloadApi;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.mail.internet.AddressException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith (SpringExtension.class)
class PnSafeStorageClientTest {
	@Autowired
	private PnSafeStorageClient pnSafeStorageClient;
	@MockBean
	private FileUploadApi fileUploadApi;
	@MockBean
	private FileDownloadApi fileDownloadApi;

	@Test
	void testCreateFile () {
		FileCreationRequestDto fileCreationRequestDto = new FileCreationRequestDto();
		fileCreationRequestDto.setStatus("status");
		fileCreationRequestDto.setContentType("contentType");
		fileCreationRequestDto.setDocumentType("documentType");
		String cxId = "cxId";
		FileCreationResponseDto fileCreationResponseDto = mock(FileCreationResponseDto.class);
		when(fileUploadApi.createFile(cxId,fileCreationRequestDto)).thenReturn(Mono.just(fileCreationResponseDto));
		StepVerifier.create(pnSafeStorageClient.createFile(fileCreationRequestDto,cxId))
				.expectNext(fileCreationResponseDto)
				.verifyComplete();
	}
	@Test
	void testCreateFileCode404() {
		FileCreationRequestDto fileCreationRequestDto = new FileCreationRequestDto();
		fileCreationRequestDto.setStatus("");
		fileCreationRequestDto.setContentType("");
		fileCreationRequestDto.setDocumentType("");
		when(fileUploadApi.createFile("cxId",fileCreationRequestDto)).thenReturn(Mono.error(new WebClientResponseException(404, "Not Found", null, null, null)));
		StepVerifier.create(pnSafeStorageClient.createFile(fileCreationRequestDto,"cxId"))
				.expectError(PnSafeStorageException.class)
				.verify();
	}
	@Test
	void testGetFile () {
		String fileKey = "fileKey";
		FileDownloadResponseDto fileDownloadResponseDto = mock(FileDownloadResponseDto.class);
		when(fileDownloadApi.getFile(fileKey,"cxId", true)).thenReturn(Mono.just(fileDownloadResponseDto));
		StepVerifier.create(pnSafeStorageClient.getFile(fileKey,  "cxId"))
				.expectNext(fileDownloadResponseDto)
				.verifyComplete();
	}
	@Test
	void testGetFileCode404() {
		String fileKey = "fileKey";
		when(fileDownloadApi.getFile(fileKey, "cxId", true))
				.thenReturn(Mono.error(new WebClientResponseException(404, "Not Found", null, null, null)));
		StepVerifier.create(pnSafeStorageClient.getFile(fileKey, "cxId"))
				.expectError(PnAddressManagerException.class)
				.verify();
	}
	@Test
	void testGetFileCodeError() {
		String fileKey = "fileKey";
		when(fileDownloadApi.getFile(fileKey, "cxId", true))
				.thenReturn(Mono.error(new WebClientResponseException(500,"", null, null, null)));
		StepVerifier.create(pnSafeStorageClient.getFile(fileKey, "cxId"))
				.expectError(PnSafeStorageException.class)
				.verify();
	}

}

