package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnFileNotFoundException;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.*;
import static it.pagopa.pn.address.manager.constant.BatchStatus.WORKED;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_CHECKING_APIKEY;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_SERVICE_SAFE_STORAGE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Service
@CustomLog
@RequiredArgsConstructor
public class NormalizzatoreService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final NormalizzatoreConverter normalizzatoreConverter;
    private final NormalizzatoreBatchService normalizzatoreBatchService;
    private final SqsService sqsService;
    private final SafeStorageService safeStorageService;
    private final PostelBatchRepository postelBatchRepository;
    private final ApiKeyUtils apiKeyUtils;
    private final AddressUtils addressUtils;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private static final String CALLBACK_ERROR_LOG = "callbackNormalizedAddress error:{}";


    public Mono<PreLoadResponseData> presignedUploadRequest(PreLoadRequestData request, String pnAddressManagerCxId, String xApiKey) {
        return apiKeyUtils.checkPostelApiKey(pnAddressManagerCxId, xApiKey)
                .doOnNext(apiKeyModel -> {
                    log.logCheckingOutcome(PROCESS_CHECKING_APIKEY, true);
                    log.info(ADDRESS_NORMALIZER_SYNC + "Founded apikey for safeStorage presignedUploadRequest.");
                })
                .flatMapIterable(apiKeyModel -> request.getPreloads())
                .flatMap(preLoadRequest -> {
                    log.info("preloadDocuments contentType:{} preloadIdx:{}", preLoadRequest.getContentType(), preLoadRequest.getPreloadIdx());
                    return createFile(pnAddressManagerConfig.getPagoPaCxId(), preLoadRequest);
                })
                .collectList()
                .map(normalizzatoreConverter::collectPreLoadRequestToPreLoadRequestData);
    }

    private Mono<PreLoadResponse> createFile(String pnAddressManagerCxId, PreLoadRequest preLoadRequest) {
        FileCreationRequestDto fileCreationRequest = normalizzatoreConverter.preLoadRequestToFileCreationRequestDto(preLoadRequest);
        log.logStartingProcess(PROCESS_SERVICE_SAFE_STORAGE + ": create file");
        return pnSafeStorageClient.createFile(fileCreationRequest, pnAddressManagerCxId, preLoadRequest.getSha256())
                .map(fileCreationResponseDto -> {
                    log.info(ADDRESS_NORMALIZER_ASYNC + "created file with fileKey: [{}]", fileCreationResponseDto.getKey());
                    return normalizzatoreConverter.fileDownloadResponseDtoToFileDownloadResponse(fileCreationResponseDto, preLoadRequest.getPreloadIdx());
                })
                .onErrorResume(WebClientResponseException.class, error -> {
                    log.error("Exception in call createFile - error={}", error.getMessage(), error);
                    if (error.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        log.error(ADDRESS_NORMALIZER_ASYNC + "createFile error:{}", error.getMessage(), error);
                        return Mono.error(new PnAddressManagerException(error.getMessage(), HttpStatus.BAD_REQUEST.value(),
                                SEMANTIC_ERROR_CODE));
                    }
                    log.error(ADDRESS_NORMALIZER_ASYNC + "failed to create file", error);
                    return Mono.error(error);
                });
    }

    /**
     * The callbackNormalizedAddress function is called by the PN Address Manager service when it has completed normalizing a batch of addresses.
     * The function checks that the API key provided in the request matches one stored in our database, and then finds the PostelBatch object
     * associated with this callback request. It then checks that the output fileKey received exists in safeStorage and verify given checksum
     * with sha256 retrieved from safeStorage, and if so, send to internalQueue the normalized callback request,
     * Finally send synchronous response to the caller with successfully or error response.
     */
    public Mono<OperationResultCodeResponse> callbackNormalizedAddress(NormalizerCallbackRequest callbackRequestData, String pnAddressManagerCxId, String xApiKey) {
        return apiKeyUtils.checkPostelApiKey(pnAddressManagerCxId, xApiKey)
                .flatMap(apiKeyModel -> findPostelBatch(callbackRequestData.getRequestId().split(RETRY_SUFFIX)[0]))
                .flatMap(postelBatch -> updateWithFileKeyTimestampAndError(postelBatch, callbackRequestData))
                .flatMap(postelBatch -> checkOutputFileOnFileStorage(callbackRequestData, postelBatch, pnAddressManagerCxId))
                .onErrorResume(throwable -> {
                    log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                    return Mono.error(throwable);
                });
    }

    private Mono<NormalizzatoreBatch> updateWithFileKeyTimestampAndError(NormalizzatoreBatch normalizzatoreBatch, NormalizerCallbackRequest callbackRequestData) {
        if(StringUtils.hasText(callbackRequestData.getUri())) {
            normalizzatoreBatch.setOutputFileKey(callbackRequestData.getUri().replace(SAFE_STORAGE_URL_PREFIX, ""));
        }
        normalizzatoreBatch.setCallbackTimeStamp(LocalDateTime.now());
        normalizzatoreBatch.setError(callbackRequestData.getError());

        return postelBatchRepository.update(normalizzatoreBatch);
    }

    private Mono<NormalizzatoreBatch> findPostelBatch(String idLavorazione) {
        return normalizzatoreBatchService.findPostelBatch(idLavorazione)
                .switchIfEmpty(Mono.error(new PnAddressManagerException(String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND, idLavorazione), HttpStatus.BAD_REQUEST.value(),
                        SEMANTIC_ERROR_CODE)));
    }

    private Mono<OperationResultCodeResponse> checkOutputFileOnFileStorage(NormalizerCallbackRequest normalizerCallbackRequest, NormalizzatoreBatch normalizzatoreBatch, String pnAddressManagerCxId) {
        OperationResultCodeResponse response = getOperationResultCodeOK();
        if (!StringUtils.hasText(normalizerCallbackRequest.getError())) {
            return getFile(normalizerCallbackRequest.getUri())
                    .flatMap(fileDownloadResponse -> {
                        log.info(ADDRESS_NORMALIZER_ASYNC + "callbackNormalizedAddress fileDownloadResponse:{}", fileDownloadResponse);
                        return verifyCheckSum(normalizerCallbackRequest, fileDownloadResponse)
                                .thenReturn(response);
                    })
                    .flatMap(operationResultCodeResponse -> sendToInternalQueueAndUpdatePostelBatchStatus(normalizerCallbackRequest, normalizzatoreBatch).thenReturn(response))
                    .onErrorResume(throwable -> {
                        log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                        return Mono.error(throwable);
                    });
        }
        else {
            return sendToInternalQueueAndUpdatePostelBatchStatus(normalizerCallbackRequest, normalizzatoreBatch)
                    .thenReturn(response);
        }
    }

    private OperationResultCodeResponse getOperationResultCodeOK() {
        OperationResultCodeResponse response = new OperationResultCodeResponse();
        response.setResultCode("202.00");
        response.setResultDescription("Accepted");
        response.setClientResponseTimeStamp(java.util.Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        return response;
    }

    private Mono<Void> verifyCheckSum(NormalizerCallbackRequest callbackRequestData, FileDownloadResponse fileDownloadResponse) {
        if (!fileDownloadResponse.getChecksum().equalsIgnoreCase(callbackRequestData.getSha256())) {
            return Mono.error(new PnAddressManagerException(String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELINVALIDCHECKSUM, callbackRequestData.getUri()),
                    HttpStatus.BAD_REQUEST.value(), SEMANTIC_ERROR_CODE));
        }
        return Mono.empty();
    }

    public Mono<FileDownloadResponse> getFile(String fileKey) {
        return safeStorageService.getFile(fileKey, pnAddressManagerConfig.getPagoPaCxId())
                .onErrorResume(PnFileNotFoundException.class, error -> {
                    log.error("Exception in call getFile fileKey={}}", fileKey);
                    log.error(ADDRESS_NORMALIZER_ASYNC + "getFile error:{}", error.getMessage(), error);
                    return Mono.error(new PnAddressManagerException(String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELOUTPUTFILEKEYNOTFOUND, fileKey), HttpStatus.BAD_REQUEST.value(),
                                SEMANTIC_ERROR_CODE));
                });
    }

    private Mono<Void> sendToInternalQueueAndUpdatePostelBatchStatus(NormalizerCallbackRequest callbackRequestData, NormalizzatoreBatch normalizzatoreBatch) {
        LocalDateTime now = LocalDateTime.now();
        return sqsService.pushToCallbackQueue(addressUtils.getPostelCallbackSqsDto(callbackRequestData, normalizzatoreBatch.getBatchId()))
                .map(sendMessageResponse -> {
                    normalizzatoreBatch.setStatus(WORKED.name());
                    normalizzatoreBatch.setTtl(now.plusSeconds(pnAddressManagerConfig.getNormalizer().getPostel().getTtl()).toEpochSecond(ZoneOffset.UTC));
                    return normalizzatoreBatch;
                })
                .flatMap(postelBatchRepository::update)
                .map(batch -> {
                    log.debug("Normalize Address PostelBatch - batchId {} - set Status in {}", normalizzatoreBatch.getBatchId(), normalizzatoreBatch.getStatus());
                    return batch;
                })
                .onErrorResume(throwable -> {
                    log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                    return Mono.error(throwable);
                })
                .then();
    }
}
