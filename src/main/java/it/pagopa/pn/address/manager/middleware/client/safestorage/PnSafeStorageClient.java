package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.exception.PnFileNotFoundException;
import it.pagopa.pn.address.manager.exception.PnSafeStorageException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileDownloadApi;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi;
import it.pagopa.pn.commons.log.PnLogger;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.SHA256;

@CustomLog
@Component
public class PnSafeStorageClient {

    private final FileUploadApi fileUploadApi;
    private final FileDownloadApi fileDownloadApi;
    public PnSafeStorageClient(PnSafeStorageWebClient pnSafeStorageWebClient) {

        this.fileUploadApi = new FileUploadApi(pnSafeStorageWebClient.init());
        this.fileDownloadApi = new FileDownloadApi(pnSafeStorageWebClient.init());
    }

    public Mono<FileDownloadResponseDto> getFile(String fileKey, String cxId) {
        log.debug("Req params : {}", fileKey);
        log.trace("GET FILE TICK {}", new Date().getTime());

        return fileDownloadApi.getFile(fileKey, cxId, false)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(item -> {
                    log.trace("GET FILE TOCK {}", new Date().getTime());
                    return item;
                }).onErrorResume(WebClientResponseException.class, error -> {
                    log.error("Exception in call getFile fileKey={} error={}", fileKey, error);

                    if (error.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                        log.error("File not found from safeStorage fileKey={} error={}", fileKey, error);
                        String errorDetail = "Allegato non trovato. fileKey=" + fileKey;
                        return Mono.error(new PnFileNotFoundException(errorDetail, error));
                    }
                    return Mono.error(error);
                });
    }

    public Mono<FileCreationResponseDto> createFile(FileCreationRequestDto fileCreationRequest, String cxId, String sha256) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, "createFile");
        log.info("POST LOG:  cxId{} fileCreationRequest{}", cxId, fileCreationRequest.toString());
        return this.fileUploadApi.createFile(cxId, SHA256, sha256, fileCreationRequest)
                .map(item -> {
                    log.trace("CREATE FILE TOCK {}", new Date().getTime());
                    return item;
                })
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).onErrorResume(WebClientResponseException.class, ex -> {
                    log.trace("CREATE FILE TOCK {}", new Date().getTime());
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(new PnSafeStorageException(ex));
                });
    }
}
