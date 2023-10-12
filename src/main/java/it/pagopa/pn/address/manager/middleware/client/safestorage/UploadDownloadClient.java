package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
@Slf4j
public class UploadDownloadClient {

    private final WebClient webClient;

    public UploadDownloadClient() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector())
                .build();
    }

    public Mono<String> uploadContent(String content, FileCreationResponseDto fileCreationResponse, String sha256) {
        HttpMethod httpMethod = fileCreationResponse.getUploadMethod() == FileCreationResponseDto.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;
        log.info("start to upload file to: {}", fileCreationResponse.getUploadUrl());
        return webClient.method(httpMethod)
                .uri(URI.create(fileCreationResponse.getUploadUrl()))
                .header("Content-Type", "text/csv")
                .header("x-amz-meta-secret", fileCreationResponse.getSecret())
                .header("x-amz-checksum-sha256",sha256)
                .bodyValue(content.getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .toBodilessEntity()
                .thenReturn(fileCreationResponse.getKey())
                .onErrorResume(ee -> {
                    log.error("Normalize Address - uploadContent Exception uploading file", ee);
                    return Mono.error(new PnAddressManagerException(ee.getMessage(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE));
                });
    }

    public Mono<byte[]> downloadContent(String downloadUrl) {
        return webClient.get()
                .uri(URI.create(downloadUrl))
                .retrieve()
                .bodyToMono(byte[].class)
                .onErrorMap(ex -> {
                    log.error("downloadContent Exception downloading content", ex);
                    return new PnAddressManagerException(ex.getMessage(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE);
                });
    }
}
