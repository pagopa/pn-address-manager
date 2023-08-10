package it.pagopa.pn.address.manager.converter;

import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class NormalizzatoreConverter {
	public static final String PN_ADDRESS_TO_NORMALIZE_ATTACHMENTS = "PN_ADDRESS_TO_NORMALIZE_ATTACHMENTS";
	public static final String PRELOADED = "PRELOADED";

	public FileCreationRequestDto preLoadRequestToFileCreationRequestDto (PreLoadRequest preLoadRequest){
		FileCreationRequestDto fileCreationRequestDto = new FileCreationRequestDto();
		fileCreationRequestDto.setContentType(preLoadRequest.getContentType());
		fileCreationRequestDto.setDocumentType(PN_ADDRESS_TO_NORMALIZE_ATTACHMENTS);
		fileCreationRequestDto.setStatus(PRELOADED);
		return fileCreationRequestDto;
	}
	public PreLoadResponse fileDownloadResponseDtoToFileDownloadResponse (FileCreationResponseDto fileCreationResponseDto, String preLoadIdx) {
		PreLoadResponse preLoadResponse = new PreLoadResponse();
		preLoadResponse.setKey(fileCreationResponseDto.getKey());
		preLoadResponse.setUrl(fileCreationResponseDto.getUploadUrl());
		preLoadResponse.setHttpMethod(PreLoadResponse.HttpMethodEnum.fromValue(fileCreationResponseDto.getUploadMethod().getValue()));
		preLoadResponse.setSecret(fileCreationResponseDto.getSecret());
		preLoadResponse.setPreloadIdx(preLoadIdx);
		return preLoadResponse;
	}
	public PreLoadResponseData collectPreLoadRequestToPreLoadRequestData(List<PreLoadResponse> preLoadResponses) {
		PreLoadResponseData preLoadResponseData = new PreLoadResponseData();
		preLoadResponseData.setPreloads(preLoadResponses);
		return preLoadResponseData;
	}
	public FileDownloadResponse fileDownloadResponseDtoToFileDownloadResponse (FileDownloadResponseDto fileDownloadResponseDto) {
		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		FileDownloadInfo fileDownloadInfo = new FileDownloadInfo();
		fileDownloadInfo.setRetryAfter(fileDownloadResponseDto.getDownload().getRetryAfter());
		fileDownloadInfo.setUrl(fileDownloadResponseDto.getDownload().getUrl());
		fileDownloadResponse.setDownload(fileDownloadInfo);
		fileDownloadResponse.setChecksum(fileDownloadResponseDto.getChecksum());
		fileDownloadResponse.setContentLength(fileDownloadResponseDto.getContentLength());
		fileDownloadResponse.setKey(fileDownloadResponseDto.getKey());
		fileDownloadResponse.setDocumentType(fileDownloadResponseDto.getDocumentType());
		fileDownloadResponse.setDocumentStatus(fileDownloadResponseDto.getDocumentStatus());
		fileDownloadResponse.setRetentionUntil(Date.from(fileDownloadResponseDto.getRetentionUntil().toInstant()));
		fileDownloadResponse.setVersionId(fileDownloadResponseDto.getVersionId());
		fileDownloadResponse.setContentType(fileDownloadResponseDto.getContentType());
		return fileDownloadResponse;
	}
}
