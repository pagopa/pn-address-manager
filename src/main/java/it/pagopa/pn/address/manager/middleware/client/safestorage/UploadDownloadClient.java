package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.msclient.pn.safe.storage.v1.dto.FileCreationResponseDto;
import lombok.CustomLog;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_SERVICE_UPLOAD_DOWNLOAD_FILE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
@CustomLog
public class UploadDownloadClient {

    private final WebClient webClient;

    public UploadDownloadClient() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector())
                .build();
    }

    public Mono<String> uploadContent(String content, FileCreationResponseDto fileCreationResponse, String sha256) {
        HttpMethod httpMethod = fileCreationResponse.getUploadMethod() == FileCreationResponseDto.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;
        log.logStartingProcess(PROCESS_SERVICE_UPLOAD_DOWNLOAD_FILE);
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
                    return Mono.error(new PnInternalAddressManagerException(ee.getMessage(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE));
                });
    }

    public Mono<byte[]> downloadContent(String downloadUrl) {
        log.logStartingProcess(PROCESS_SERVICE_UPLOAD_DOWNLOAD_FILE);
        log.info("start to download file to: {}", downloadUrl);
        try {
            Flux<DataBuffer> dataBufferFlux = WebClient.create()
                    .get()
                    .uri(new URI(downloadUrl))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .doOnError(ex -> log.error("Error in WebClient", ex));

            return DataBufferUtils.join(dataBufferFlux)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return bytes;
                    })
                    .onErrorMap(ex -> {
                        log.error("downloadContent Exception downloading content", ex);
                        return new PnInternalAddressManagerException(ex.getMessage(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE);
                    });
        } catch (URISyntaxException ex) {
            log.error("error in URI ", ex);
            return Mono.error(new PnInternalAddressManagerException(ex.getMessage(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE));
        }
    }
}
