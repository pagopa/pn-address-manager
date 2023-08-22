package it.pagopa.pn.address.manager.client.safestorage;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION;

@Component
@Slf4j
public class UploadDownloadClient {
    private final WebClient webClient;
    public UploadDownloadClient() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector())
                .build();    }

    public Mono<String> uploadContent(String content, FileCreationResponseDto fileCreationResponse, String sha256) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf(MediaType.TEXT_PLAIN_VALUE));
            headers.add("x-amz-checksum-sha256", sha256);
            headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

            ByteArrayResource resource = new ByteArrayResource(content.getBytes());

            return webClient.method(HttpMethod.POST)
                    .uri(fileCreationResponse.getUploadUrl())
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(BodyInserters.fromResource(resource))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(res -> {
                        if (!res.equals("OK")) {
                            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION, ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE);
                        }
                    })
                    .doOnError(PnInternalException.class, ee -> log.error("uploadContent PnInternalException uploading file", ee))
                    .onErrorResume(ee -> {
                        log.error("uploadContent Exception uploading file", ee);
                        return Mono.error(new PnAddressManagerException(ee.getMessage(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE));
                    });
        } catch (PnInternalException ee) {
            log.error("uploadContent PnInternalException uploading file", ee);
            return Mono.error(ee);
        } catch (Exception ee) {
            log.error("uploadContent Exception uploading file", ee);
            return Mono.error(new PnAddressManagerException(ee.getMessage(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE));
        }
    }
}
