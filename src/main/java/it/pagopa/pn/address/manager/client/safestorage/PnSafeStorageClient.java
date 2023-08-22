package it.pagopa.pn.address.manager.client.safestorage;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
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

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_NOT_FOUND;

@CustomLog
@Component
public class PnSafeStorageClient {
	private final FileUploadApi fileUploadApi;
	private final FileDownloadApi fileDownloadApi;

	public PnSafeStorageClient (FileUploadApi fileUploadApi, FileDownloadApi fileDownloadApi) {
		this.fileUploadApi = fileUploadApi;
		this.fileDownloadApi = fileDownloadApi;
	}
	public Mono<FileDownloadResponseDto> getFile (String fileKey, String cxId) {
		boolean metadataOnly = true;
		log.debug("Req params : {}", fileKey);
		log.trace("GET FILE TICK {}", new Date().getTime());
		return fileDownloadApi.getFile(fileKey, cxId, metadataOnly)
				.retryWhen(
						Retry.backoff(2, Duration.ofMillis(500))
								.filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
				).map(item -> {
					log.trace("GET FILE TOCK {}", new Date().getTime());
					return item;
				}).onErrorResume(WebClientResponseException.class, ex -> {
					log.trace("GET FILE TOCK {}", new Date().getTime());
					log.error(ex.getResponseBodyAsString());
					if (ex.getStatusCode() == HttpStatus.NOT_FOUND){
						return Mono.error(new PnAddressManagerException(ex.getMessage(), ex.getStatusText()
								, ex.getStatusCode().value(),ERROR_ADDRESS_MANAGER_NOT_FOUND));
					}
					return Mono.error(new PnSafeStorageException(ex));
				});
	}
	public Mono<FileCreationResponseDto> createFile (FileCreationRequestDto fileCreationRequest,String cxId) {
		log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, "createFile");
		log.info("POST LOG:  cxId{} fileCreationRequest{}",cxId,fileCreationRequest.toString());
		return this.fileUploadApi.createFile(cxId,fileCreationRequest)
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
