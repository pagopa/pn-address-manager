package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
@Slf4j
public class UploadDownloadClient {
    public final static MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");
    private final WebClient webClient;

    public UploadDownloadClient() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector())
                .build();
    }

    public Mono<String> uploadContent(String content, FileCreationResponseDto fileCreationResponse, String sha256) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TEXT_CSV_TYPE);
        headers.add("x-amz-checksum-sha256", sha256);
        headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());


        ByteArrayResource resource = new ByteArrayResource(content.getBytes());

        return webClient.method(HttpMethod.PUT)
                .uri(fileCreationResponse.getUploadUrl())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(BodyInserters.fromResource(resource))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ee -> {
                    log.error("uploadContent Exception uploading file", ee);
                    return Mono.error(new PnAddressManagerException(ee.getMessage(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE));
                });
    }

    public Mono<byte[]> downloadContent(String downloadUrl) {
        return webClient.get()
                .uri(downloadUrl)
                .retrieve()
                .bodyToMono(byte[].class)
                .onErrorMap(ex -> {
                    log.error("downloadContent Exception downloading content", ex);
                    return new PnAddressManagerException(ex.getMessage(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE);
                });
    }
}
