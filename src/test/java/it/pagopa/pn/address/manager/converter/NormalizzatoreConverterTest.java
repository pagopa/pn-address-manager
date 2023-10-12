package it.pagopa.pn.address.manager.converter;

import it.pagopa.pn.address.manager.constant.AddressmanagerConstant;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {NormalizzatoreConverter.class})
@ExtendWith(SpringExtension.class)
class NormalizzatoreConverterTest {
    @Autowired
    private NormalizzatoreConverter normalizzatoreConverter;


    /**
     * Method under test: {@link NormalizzatoreConverter#preLoadRequestToFileCreationRequestDto(PreLoadRequest)}
     */
    @Test
    void testPreLoadRequestToFileCreationRequestDto() {
        FileCreationRequestDto actualPreLoadRequestToFileCreationRequestDtoResult = normalizzatoreConverter
                .preLoadRequestToFileCreationRequestDto(new PreLoadRequest());
        assertNull(actualPreLoadRequestToFileCreationRequestDtoResult.getContentType());
        assertEquals(AddressmanagerConstant.SAVED, actualPreLoadRequestToFileCreationRequestDtoResult.getStatus());
        assertEquals(AddressmanagerConstant.PN_ADDRESSES_NORMALIZED,
                actualPreLoadRequestToFileCreationRequestDtoResult.getDocumentType());
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#preLoadRequestToFileCreationRequestDto(PreLoadRequest)}
     */
    @Test
    void testPreLoadRequestToFileCreationRequestDto2() {
        PreLoadRequest preLoadRequest = mock(PreLoadRequest.class);
        when(preLoadRequest.getContentType()).thenReturn("text/plain");
        FileCreationRequestDto actualPreLoadRequestToFileCreationRequestDtoResult = normalizzatoreConverter
                .preLoadRequestToFileCreationRequestDto(preLoadRequest);
        assertEquals("text/plain", actualPreLoadRequestToFileCreationRequestDtoResult.getContentType());
        assertEquals(AddressmanagerConstant.SAVED, actualPreLoadRequestToFileCreationRequestDtoResult.getStatus());
        assertEquals(AddressmanagerConstant.PN_ADDRESSES_NORMALIZED,
                actualPreLoadRequestToFileCreationRequestDtoResult.getDocumentType());
        verify(preLoadRequest).getContentType();
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#fileDownloadResponseDtoToFileDownloadResponse(FileCreationResponseDto, String)}
     */
    @Test
    void testFileDownloadResponseDtoToFileDownloadResponse() {
        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.uploadMethod(FileCreationResponseDto.UploadMethodEnum.PUT);
        PreLoadResponse actualFileDownloadResponseDtoToFileDownloadResponseResult = normalizzatoreConverter
                .fileDownloadResponseDtoToFileDownloadResponse(fileCreationResponseDto, "Pre Load Idx");
        assertEquals(PreLoadResponse.HttpMethodEnum.PUT,
                actualFileDownloadResponseDtoToFileDownloadResponseResult.getHttpMethod());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getUrl());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getSecret());
        assertEquals("Pre Load Idx", actualFileDownloadResponseDtoToFileDownloadResponseResult.getPreloadIdx());
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#fileDownloadResponseDtoToFileDownloadResponse(FileCreationResponseDto, String)}
     */
    @Test
    void testFileDownloadResponseDtoToFileDownloadResponse2() {
        FileCreationResponseDto fileCreationResponseDto = mock(FileCreationResponseDto.class);
        when(fileCreationResponseDto.getSecret()).thenReturn("Secret");
        when(fileCreationResponseDto.getUploadMethod()).thenReturn(FileCreationResponseDto.UploadMethodEnum.PUT);
        when(fileCreationResponseDto.getKey()).thenReturn("Key");
        when(fileCreationResponseDto.getUploadUrl()).thenReturn("https://example.org/example");
        PreLoadResponse actualFileDownloadResponseDtoToFileDownloadResponseResult = normalizzatoreConverter
                .fileDownloadResponseDtoToFileDownloadResponse(fileCreationResponseDto, "Pre Load Idx");
        assertEquals(PreLoadResponse.HttpMethodEnum.PUT,
                actualFileDownloadResponseDtoToFileDownloadResponseResult.getHttpMethod());
        assertEquals("https://example.org/example", actualFileDownloadResponseDtoToFileDownloadResponseResult.getUrl());
        assertEquals("Secret", actualFileDownloadResponseDtoToFileDownloadResponseResult.getSecret());
        assertEquals("Pre Load Idx", actualFileDownloadResponseDtoToFileDownloadResponseResult.getPreloadIdx());
        assertEquals("safestorage://Key", actualFileDownloadResponseDtoToFileDownloadResponseResult.getKey());
        verify(fileCreationResponseDto).getUploadMethod();
        verify(fileCreationResponseDto).getKey();
        verify(fileCreationResponseDto).getSecret();
        verify(fileCreationResponseDto).getUploadUrl();
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#fileDownloadResponseDtoToFileDownloadResponse(FileDownloadResponseDto)}
     */
    @Test
    void testFileDownloadResponseDtoToFileDownloadResponse3() {
        FileDownloadResponseDto fileDownloadResponseDto = new FileDownloadResponseDto();
        fileDownloadResponseDto
                .retentionUntil(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
        FileDownloadResponse actualFileDownloadResponseDtoToFileDownloadResponseResult = normalizzatoreConverter
                .fileDownloadResponseDtoToFileDownloadResponse(fileDownloadResponseDto);
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getChecksum());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getVersionId());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getKey());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getDocumentType());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getContentLength());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getDocumentStatus());
        assertNull(actualFileDownloadResponseDtoToFileDownloadResponseResult.getContentType());
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#fileDownloadResponseDtoToFileDownloadResponse(FileDownloadResponseDto)}
     */
    @Test
    void testFileDownloadResponseDtoToFileDownloadResponse4() {
        FileDownloadResponseDto fileDownloadResponseDto = mock(FileDownloadResponseDto.class);
        when(fileDownloadResponseDto.getContentType()).thenReturn("text/plain");
        when(fileDownloadResponseDto.getVersionId()).thenReturn("42");
        when(fileDownloadResponseDto.getDownload()).thenReturn(new FileDownloadInfoDto());
        when(fileDownloadResponseDto.getChecksum()).thenReturn("Checksum");
        when(fileDownloadResponseDto.getDocumentStatus()).thenReturn("Document Status");
        when(fileDownloadResponseDto.getDocumentType()).thenReturn("Document Type");
        when(fileDownloadResponseDto.getKey()).thenReturn("Key");
        BigDecimal valueOfResult = BigDecimal.valueOf(1L);
        when(fileDownloadResponseDto.getContentLength()).thenReturn(valueOfResult);
        when(fileDownloadResponseDto.getRetentionUntil())
                .thenReturn(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
        FileDownloadResponse actualFileDownloadResponseDtoToFileDownloadResponseResult = normalizzatoreConverter
                .fileDownloadResponseDtoToFileDownloadResponse(fileDownloadResponseDto);
        assertEquals("Checksum", actualFileDownloadResponseDtoToFileDownloadResponseResult.getChecksum());
        assertEquals("42", actualFileDownloadResponseDtoToFileDownloadResponseResult.getVersionId());
        assertEquals("Key", actualFileDownloadResponseDtoToFileDownloadResponseResult.getKey());
        assertEquals("Document Type", actualFileDownloadResponseDtoToFileDownloadResponseResult.getDocumentType());
        BigDecimal expectedContentLength = valueOfResult.ONE;
        BigDecimal contentLength = actualFileDownloadResponseDtoToFileDownloadResponseResult.getContentLength();
        assertSame(expectedContentLength, contentLength);
        assertEquals("Document Status", actualFileDownloadResponseDtoToFileDownloadResponseResult.getDocumentStatus());
        assertEquals("text/plain", actualFileDownloadResponseDtoToFileDownloadResponseResult.getContentType());
        FileDownloadInfo download = actualFileDownloadResponseDtoToFileDownloadResponseResult.getDownload();
        assertNull(download.getUrl());
        assertEquals("1", contentLength.toString());
        assertNull(download.getRetryAfter());
        verify(fileDownloadResponseDto, atLeast(1)).getDownload();
        verify(fileDownloadResponseDto).getChecksum();
        verify(fileDownloadResponseDto).getContentType();
        verify(fileDownloadResponseDto).getDocumentStatus();
        verify(fileDownloadResponseDto).getDocumentType();
        verify(fileDownloadResponseDto).getKey();
        verify(fileDownloadResponseDto).getVersionId();
        verify(fileDownloadResponseDto).getContentLength();
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#collectPreLoadRequestToPreLoadRequestData(List)}
     */
    @Test
    void testCollectPreLoadRequestToPreLoadRequestData() {
        assertTrue(
                normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(new ArrayList<>()).getPreloads().isEmpty());
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#collectPreLoadRequestToPreLoadRequestData(List)}
     */
    @Test
    void testCollectPreLoadRequestToPreLoadRequestData2() {
        ArrayList<PreLoadResponse> preLoadResponses = new ArrayList<>();
        preLoadResponses.add(new PreLoadResponse());
        assertEquals(1,
                normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(preLoadResponses).getPreloads().size());
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#collectPreLoadRequestToPreLoadRequestData(List)}
     */
    @Test
    void testCollectPreLoadRequestToPreLoadRequestData3() {
        ArrayList<PreLoadResponse> preLoadResponses = new ArrayList<>();
        preLoadResponses.add(new PreLoadResponse());
        preLoadResponses.add(new PreLoadResponse());
        assertEquals(2,
                normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(preLoadResponses).getPreloads().size());
    }

    /**
     * Method under test: {@link NormalizzatoreConverter#collectPreLoadRequestToPreLoadRequestData(List)}
     */
    @Test
    void testCollectPreLoadRequestToPreLoadRequestData4() {
        ArrayList<PreLoadResponse> preLoadResponses = new ArrayList<>();
        preLoadResponses.add(mock(PreLoadResponse.class));
        assertEquals(1,
                normalizzatoreConverter.collectPreLoadRequestToPreLoadRequestData(preLoadResponses).getPreloads().size());
    }
}

