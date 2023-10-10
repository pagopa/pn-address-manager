package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.BatchStatus.WORKED;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.APIKEY_DOES_NOT_EXISTS;

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

    private static final String AM_POSTEL_CALLBACK_EVENTTYPE = "AM_POSTEL_CALLBACK";

    private static final String CALLBACK_ERROR_LOG = "callbackNormalizedAddress error:{}";

    public NormalizzatoreService(PnSafeStorageClient pnSafeStorageClient,
                                 NormalizzatoreConverter normalizzatoreConverter,
                                 PostelBatchService postelBatchService,
                                 SqsService sqsService,
                                 SafeStorageService safeStorageService,
                                 PostelBatchRepository postelBatchRepository,
                                 ApiKeyRepository apiKeyRepository,
                                 AddressUtils addressUtils) {
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.normalizzatoreConverter = normalizzatoreConverter;
        this.postelBatchService = postelBatchService;
        this.sqsService = sqsService;
        this.safeStorageService = safeStorageService;
        this.postelBatchRepository = postelBatchRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.addressUtils = addressUtils;
    }

    public Mono<PreLoadResponseData> presignedUploadRequest(PreLoadRequestData request, String pnAddressManagerCxId, String xApiKey) {
        return checkApiKey(pnAddressManagerCxId, xApiKey)
                .flatMapIterable(apiKeyModel -> request.getPreloads())
                .flatMap(preLoadRequest -> {
                    log.info("preloadDocuments contentType:{} preloadIdx:{}", preLoadRequest.getContentType(), preLoadRequest.getPreloadIdx());
                    return createFile(pnAddressManagerCxId, preLoadRequest);
                })
                .collectList()
                .map(normalizzatoreConverter::collectPreLoadRequestToPreLoadRequestData);
    }

    private Mono<PreLoadResponse> createFile(String pnAddressManagerCxId, PreLoadRequest preLoadRequest) {
        FileCreationRequestDto fileCreationRequest = normalizzatoreConverter.preLoadRequestToFileCreationRequestDto(preLoadRequest);
        return pnSafeStorageClient.createFile(fileCreationRequest, pnAddressManagerCxId)
                .map(fileCreationResponseDto -> {
                    log.info(ADDRESS_NORMALIZER_ASYNC + "created file with fileKey: [{}]", fileCreationResponseDto.getKey());
                    return normalizzatoreConverter.fileDownloadResponseDtoToFileDownloadResponse(fileCreationResponseDto, preLoadRequest.getPreloadIdx());
                })
                .onErrorResume(e -> {
                    log.error(ADDRESS_NORMALIZER_ASYNC + "failed to create file", e);
                    return Mono.error(e);
                });
    }

    public Mono<OperationResultCodeResponse> callbackNormalizedAddress(NormalizerCallbackRequest callbackRequestData, String pnAddressManagerCxId, String xApiKey) {
        return checkApiKey(pnAddressManagerCxId, xApiKey)
                .flatMap(apiKeyModel -> findPostelBatch(callbackRequestData.getRequestId()))
                .flatMap(postelBatch -> checkOutputFileOnFileStorage(callbackRequestData, pnAddressManagerCxId, postelBatch))
                .onErrorResume(throwable -> {
                    log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                    return Mono.error(throwable);
                });
    }

    private Mono<PostelBatch> findPostelBatch(String idLavorazione) {
        return postelBatchService.findPostelBatch(idLavorazione)
                .switchIfEmpty(Mono.error(new PnInternalException(String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND, idLavorazione),
                        ERROR_CODE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND)));
    }

    private Mono<OperationResultCodeResponse> checkOutputFileOnFileStorage(NormalizerCallbackRequest normalizerCallbackRequest, String pnAddressManagerCxId, PostelBatch postelBatch) {
        OperationResultCodeResponse response = getOperationResultCodeOK();
        if (!StringUtils.hasText(normalizerCallbackRequest.getError())) {
            return getFile(normalizerCallbackRequest.getUri(), pnAddressManagerCxId)
                    .flatMap(fileDownloadResponse -> {
                        log.info(ADDRESS_NORMALIZER_ASYNC + "callbackNormalizedAddress fileDownloadResponse:{}", fileDownloadResponse);
                        return verifyCheckSumAndSendToInternalQueue(normalizerCallbackRequest, fileDownloadResponse, postelBatch);
                    })
                    .onErrorResume(throwable -> {
                        log.error(CALLBACK_ERROR_LOG, throwable.getMessage(), throwable);
                        return Mono.error(throwable);
                    })
                    .thenReturn(response);
        }
        return sendToInternalQueueAndUpdatePostelBatchStatus(normalizerCallbackRequest, postelBatch, null)
                .thenReturn(response);
    }

    private OperationResultCodeResponse getOperationResultCodeOK() {
        OperationResultCodeResponse response = new OperationResultCodeResponse();
        response.setResultCode("202.00");
        response.setResultDescription("Richiesta presa in carico");
        response.setClientResponseTimeStamp(java.util.Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        return response;
    }

    private Mono<Void> verifyCheckSumAndSendToInternalQueue(NormalizerCallbackRequest callbackRequestData, FileDownloadResponse fileDownloadResponse, PostelBatch postelBatch) {
        if (!fileDownloadResponse.getChecksum().equalsIgnoreCase(callbackRequestData.getSha256())) {
            return Mono.error(new PnAddressManagerException(ERROR_CODE_ADDRESS_MANAGER_POSTELINVALIDCHECKSUM,
                    String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELINVALIDCHECKSUM, callbackRequestData.getUri()),
                    HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_POSTELINVALIDCHECKSUM));
        }
        return sendToInternalQueueAndUpdatePostelBatchStatus(callbackRequestData, postelBatch, fileDownloadResponse.getDownload().getUrl());
    }

    public Mono<FileDownloadResponse> getFile(String fileKey, String pnAddressManagerCxId) {
        return safeStorageService.getFile(fileKey, pnAddressManagerCxId);
    }

    public Mono<ApiKeyModel> checkApiKey(String cxId, String xApiKey) {
        return apiKeyRepository.findById(cxId)
                .filter(apiKeyModel -> apiKeyModel.getApiKey().equalsIgnoreCase(xApiKey))
                .switchIfEmpty(Mono.error(new PnAddressManagerException(APIKEY_DOES_NOT_EXISTS, APIKEY_DOES_NOT_EXISTS, HttpStatus.FORBIDDEN.value(), "Api Key not found")));

    }

    private Mono<Void> sendToInternalQueueAndUpdatePostelBatchStatus(NormalizerCallbackRequest callbackRequestData, PostelBatch postelBatch, String url) {
        return sqsService.pushToInputQueue(addressUtils.getPostelCallbackSqsDto(callbackRequestData, url), AM_POSTEL_CALLBACK_EVENTTYPE)
                .map(sendMessageResponse -> {
                    postelBatch.setStatus(WORKED.name());
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
