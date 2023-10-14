package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.PostelBatch;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.*;
import static it.pagopa.pn.address.manager.constant.BatchStatus.WORKED;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Service
@CustomLog
public class NormalizzatoreService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final NormalizzatoreConverter normalizzatoreConverter;
    private final PostelBatchService postelBatchService;
    private final SqsService sqsService;
    private final SafeStorageService safeStorageService;
    private final PostelBatchRepository postelBatchRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AddressUtils addressUtils;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private static final String CALLBACK_ERROR_LOG = "callbackNormalizedAddress error:{}";

    public NormalizzatoreService(PnSafeStorageClient pnSafeStorageClient,
                                 NormalizzatoreConverter normalizzatoreConverter,
                                 PostelBatchService postelBatchService,
                                 SqsService sqsService,
                                 SafeStorageService safeStorageService,
                                 PostelBatchRepository postelBatchRepository,
                                 ApiKeyRepository apiKeyRepository,
                                 AddressUtils addressUtils,
                                 PnAddressManagerConfig pnAddressManagerConfig) {
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.normalizzatoreConverter = normalizzatoreConverter;
        this.postelBatchService = postelBatchService;
        this.sqsService = sqsService;
        this.safeStorageService = safeStorageService;
        this.postelBatchRepository = postelBatchRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.addressUtils = addressUtils;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    public Mono<PreLoadResponseData> presignedUploadRequest(PreLoadRequestData request, String pnAddressManagerCxId, String xApiKey) {
        return checkApiKey(pnAddressManagerCxId, xApiKey)
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
        return pnSafeStorageClient.createFile(fileCreationRequest, pnAddressManagerCxId, preLoadRequest.getSha256())
                .map(fileCreationResponseDto -> {
                    log.info(ADDRESS_NORMALIZER_ASYNC + "created file with fileKey: [{}]", fileCreationResponseDto.getKey());
                    return normalizzatoreConverter.fileDownloadResponseDtoToFileDownloadResponse(fileCreationResponseDto, preLoadRequest.getPreloadIdx());
                })
                .onErrorResume(WebClientResponseException.class, error -> {
                    log.error("Exception in call createFile - error={}", error);
                    if (error.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        log.error(ADDRESS_NORMALIZER_ASYNC + "createFile error:{}", error.getMessage(), error);
                        return Mono.error(new PnAddressManagerException(error.getMessage(), HttpStatus.BAD_REQUEST.value(),
                                SEMANTIC_ERROR_CODE));
                    }
                    log.error(ADDRESS_NORMALIZER_ASYNC + "failed to create file", error);
                    return Mono.error(error);
                });
    }

    public Mono<OperationResultCodeResponse> callbackNormalizedAddress(NormalizerCallbackRequest callbackRequestData, String pnAddressManagerCxId, String xApiKey) {
        return checkApiKey(pnAddressManagerCxId, xApiKey)
                .flatMap(apiKeyModel -> findPostelBatch(callbackRequestData.getRequestId()))
                .flatMap(postelBatch -> checkOutputFileOnFileStorage(callbackRequestData, postelBatch))
                .onErrorResume(throwable -> {
                    log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                    return Mono.error(throwable);
                });
    }

    private Mono<PostelBatch> findPostelBatch(String idLavorazione) {
        return postelBatchService.findPostelBatch(idLavorazione)
                .switchIfEmpty(Mono.error(new PnAddressManagerException(String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND, idLavorazione), HttpStatus.BAD_REQUEST.value(),
                        SEMANTIC_ERROR_CODE)));
    }

    private Mono<OperationResultCodeResponse> checkOutputFileOnFileStorage(NormalizerCallbackRequest normalizerCallbackRequest, PostelBatch postelBatch) {
        OperationResultCodeResponse response = getOperationResultCodeOK();
        if (!StringUtils.hasText(normalizerCallbackRequest.getError())) {
            return getFile(normalizerCallbackRequest.getUri())
                    .flatMap(fileDownloadResponse -> {
                        log.info(ADDRESS_NORMALIZER_ASYNC + "callbackNormalizedAddress fileDownloadResponse:{}", fileDownloadResponse);
                        return verifyCheckSumAndSendToInternalQueue(normalizerCallbackRequest, fileDownloadResponse, postelBatch)
                                .thenReturn(response);
                    })
                    .onErrorResume(throwable -> {
                        log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                        return Mono.error(throwable);
                    });
        }
        return sendToInternalQueueAndUpdatePostelBatchStatus(normalizerCallbackRequest, postelBatch, null)
                .thenReturn(response);
    }

    private OperationResultCodeResponse getOperationResultCodeOK() {
        OperationResultCodeResponse response = new OperationResultCodeResponse();
        response.setResultCode("202.00");
        response.setResultDescription("Accepted");
        response.setClientResponseTimeStamp(java.util.Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        return response;
    }

    private Mono<Void> verifyCheckSumAndSendToInternalQueue(NormalizerCallbackRequest callbackRequestData, FileDownloadResponse fileDownloadResponse, PostelBatch postelBatch) {
        /*if (!fileDownloadResponse.getChecksum().equalsIgnoreCase(callbackRequestData.getSha256())) {
            return Mono.error(new PnAddressManagerException(String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELINVALIDCHECKSUM, callbackRequestData.getUri()),
                    HttpStatus.BAD_REQUEST.value(), SEMANTIC_ERROR_CODE));
        }*/
        return sendToInternalQueueAndUpdatePostelBatchStatus(callbackRequestData, postelBatch, fileDownloadResponse.getDownload().getUrl());
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

    public Mono<ApiKeyModel> checkApiKey(String cxId, String xApiKey) {
        return apiKeyRepository.findById(cxId)
                .filter(apiKeyModel -> apiKeyModel.getApiKey().equalsIgnoreCase(xApiKey))
                .switchIfEmpty(Mono.error(new PnInternalAddressManagerException(APIKEY_DOES_NOT_EXISTS, APIKEY_DOES_NOT_EXISTS, HttpStatus.FORBIDDEN.value(), "Api Key not found")));

    }

    private Mono<Void> sendToInternalQueueAndUpdatePostelBatchStatus(NormalizerCallbackRequest callbackRequestData, PostelBatch postelBatch, String url) {
        LocalDateTime now = LocalDateTime.now();
        return sqsService.pushToInputQueue(addressUtils.getPostelCallbackSqsDto(callbackRequestData, url), AM_POSTEL_CALLBACK_EVENTTYPE)
                .map(sendMessageResponse -> {
                    postelBatch.setStatus(WORKED.name());
                    postelBatch.setTtl(now.plusSeconds(pnAddressManagerConfig.getNormalizer().getPostel().getTtl()).toEpochSecond(ZoneOffset.UTC));
                    return postelBatch;
                })
                .flatMap(postelBatchRepository::update)
                .map(batch -> {
                    log.debug("Normalize Address PostelBatch - batchId {} - set Status in {}", postelBatch.getBatchId(), postelBatch.getStatus());
                    return batch;
                })
                .onErrorResume(throwable -> {
                    log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                    return Mono.error(throwable);
                })
                .then();
    }
}
