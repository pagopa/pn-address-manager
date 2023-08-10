package it.pagopa.pn.address.manager.converter;

import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadInfoDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadInfo;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadRequest;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration (classes = {NormalizzatoreConverter.class})
@ExtendWith (SpringExtension.class)
class NormalizzatoreConverterTest {
	@Autowired
	private NormalizzatoreConverter normalizzatoreConverter;

	/**
	 * Method under test: {@link NormalizzatoreConverter#preLoadRequestToFileCreationRequestDto(PreLoadRequest)}
	 */
	@Test
	void testPreLoadRequestToFileCreationRequestDto () {
		// Arrange and Act
		FileCreationRequestDto actualPreLoadRequestToFileCreationRequestDtoResult = normalizzatoreConverter
				.preLoadRequestToFileCreationRequestDto(new PreLoadRequest());

		// Assert
		assertNull(actualPreLoadRequestToFileCreationRequestDtoResult.getContentType());
		assertEquals(NormalizzatoreConverter.PRELOADED, actualPreLoadRequestToFileCreationRequestDtoResult.getStatus());
		assertEquals(NormalizzatoreConverter.PN_ADDRESS_TO_NORMALIZE_ATTACHMENTS,
				actualPreLoadRequestToFileCreationRequestDtoResult.getDocumentType());
	}
	@Test
	void testFileDownloadResponseDtoToFileDownloadResponse2 () {
		// Arrange
		FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
		fileCreationResponseDto.uploadMethod(FileCreationResponseDto.UploadMethodEnum.PUT);

		// Act
		PreLoadResponse actualFileDownloadResponseDtoToFileDownloadResponseResult = normalizzatoreConverter
				.fileDownloadResponseDtoToFileDownloadResponse(fileCreationResponseDto, "Pre Load Idx");

		// Assert
		assertEquals(PreLoadResponse.HttpMethodEnum.PUT,
				actualFileDownloadResponseDtoToFileDownloadResponseResult.getHttpMethod());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getUrl());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getSecret());
		assertEquals("Pre Load Idx", actualFileDownloadResponseDtoToFileDownloadResponseResult.getPreloadIdx());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getKey());
	}
	@Test
	void testFileDownloadResponseDtoToFileDownloadResponse6 () {
		// Arrange
		FileDownloadInfoDto download = new FileDownloadInfoDto();
		download.url("https://example.org/example");

		FileDownloadResponseDto fileDownloadResponseDto = new FileDownloadResponseDto();
		fileDownloadResponseDto
				.retentionUntil(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		fileDownloadResponseDto.download(download);

		// Act
		FileDownloadResponse actualFileDownloadResponseDtoToFileDownloadResponseResult = normalizzatoreConverter
				.fileDownloadResponseDtoToFileDownloadResponse(fileDownloadResponseDto);

		// Assert
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getChecksum());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getVersionId());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getKey());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getDocumentType());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getContentLength());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getDocumentStatus());
		assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getContentType());
		FileDownloadInfo download2 = actualFileDownloadResponseDtoToFileDownloadResponseResult.getDownload();
		assertEquals("https://example.org/example", download2.getUrl());
		assertNull(download2.getRetryAfter());
	}

	/**
	 * Method under test: {@link NormalizzatoreConverter#collectPreLoadRequestToPreLoadRequestData(List)}
	 */
	@Test
	void testCollectPreLoadRequestToPreLoadRequestData () {
		// Arrange, Act and Assert
		assertTrue(
				normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(new ArrayList<>()).getPreloads().isEmpty());
	}

	/**
	 * Method under test: {@link NormalizzatoreConverter#collectPreLoadRequestToPreLoadRequestData(List)}
	 */
	@Test
	void testCollectPreLoadRequestToPreLoadRequestData2 () {
		// Arrange
		ArrayList<PreLoadResponse> preLoadResponses = new ArrayList<>();
		preLoadResponses.add(new PreLoadResponse());

		// Act and Assert
		assertEquals(1,
				normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(preLoadResponses).getPreloads().size());
	}

	/**
	 * Method under test: {@link NormalizzatoreConverter#collectPreLoadRequestToPreLoadRequestData(List)}
	 */
	@Test
	void testCollectPreLoadRequestToPreLoadRequestData3 () {
		// Arrange
		ArrayList<PreLoadResponse> preLoadResponses = new ArrayList<>();
		preLoadResponses.add(new PreLoadResponse());
		preLoadResponses.add(new PreLoadResponse());

		// Act and Assert
		assertEquals(2,
				normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(preLoadResponses).getPreloads().size());
	}
}

