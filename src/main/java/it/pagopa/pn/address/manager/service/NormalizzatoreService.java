package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.model.PostelCallbackSqsDto;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND;

@Service
@CustomLog
public class NormalizzatoreService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final NormalizzatoreConverter normalizzatoreConverter;
    private final PostelBatchService postelBatchService;
    private final SqsService sqsService;
    private final SafeStorageService safeStorageService;
    private final PostelBatchRepository postelBatchRepository;
    private static final String AM_POSTEL_CALLBACK_EVENTTYPE = "AM_POSTEL_CALLBACK";

    public NormalizzatoreService(PnSafeStorageClient pnSafeStorageClient,
                                 NormalizzatoreConverter normalizzatoreConverter,
                                 PostelBatchService postelBatchService,
                                 SqsService sqsService,
                                 SafeStorageService safeStorageService, PostelBatchRepository postelBatchRepository) {
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.normalizzatoreConverter = normalizzatoreConverter;
        this.postelBatchService = postelBatchService;
        this.sqsService = sqsService;
        this.safeStorageService = safeStorageService;
        this.postelBatchRepository = postelBatchRepository;
    }

    public Mono<PreLoadResponseData> presignedUploadRequest(PreLoadRequestData request, String pnAddressManagerCxId) {
        return Flux.fromStream(request.getPreloads().stream())
                .flatMap(preload -> {
                    log.info("preloadDocuments contentType:{} preloadIdx:{}", preload.getContentType(), preload.getPreloadIdx());
                    FileCreationRequestDto fileCreationRequest = normalizzatoreConverter.preLoadRequestToFileCreationRequestDto(preload);
                    return pnSafeStorageClient.createFile(fileCreationRequest, pnAddressManagerCxId)
                            .map(v -> normalizzatoreConverter.fileDownloadResponseDtoToFileDownloadResponse(v, preload.getPreloadIdx()));
                }).collectList()
                .map(normalizzatoreConverter::collectPreLoadRequestToPreLoadRequestData);
    }

    public Mono<CallbackResponseData> callbackNormalizedAddress(CallbackRequestData callbackRequestData, String pnAddressManagerCxId) {
        CallbackResponseData callbackResponseData = new CallbackResponseData();
        return postelBatchService.findPostelBatch(callbackRequestData.getFileKeyInput())
                .switchIfEmpty(Mono.error(new PnInternalException(String.format(ERROR_MESSAGE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND, callbackRequestData.getFileKeyInput()),
                        ERROR_CODE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND)))
                .flatMap(postelBatch -> checkOutputFileOnFileStorage(callbackRequestData, pnAddressManagerCxId, callbackResponseData, postelBatch))
                .doOnError(throwable -> {
                    log.error("callbackNormalizedAddress error:{}", throwable.getMessage(), throwable);
                    callbackResponseData.setResponse(CallbackResponseData.ResponseEnum.KO);
                })
                .thenReturn(callbackResponseData);
    }

    private Mono<CallbackResponseData> checkOutputFileOnFileStorage(CallbackRequestData callbackRequestData, String pnAddressManagerCxId,
                                                                    CallbackResponseData callbackResponseData, PostelBatch postelBatch) {
        return getFile(callbackRequestData.getFileKeyOutput(), pnAddressManagerCxId)
                .map(fileDownloadResponse -> {
                    log.info("callbackNormalizedAddress fileDownloadResponse:{}", fileDownloadResponse);
                    if (!fileDownloadResponse.getChecksum().equalsIgnoreCase(""/*callbackRequestData.getCheckSum()*/)) {
                        callbackResponseData.setResponse(CallbackResponseData.ResponseEnum.KO);
                        return callbackResponseData;
                    }
                    return sendToInternalQueueAndUpdatePostelBatchStatus(callbackRequestData, fileDownloadResponse.getDownload(), postelBatch)
                            .map(sendMessageResponse -> {
                                callbackResponseData.setResponse(CallbackResponseData.ResponseEnum.OK);
                                return callbackResponseData;
                            });
                })
                .thenReturn(callbackResponseData);
    }

    private Mono<PostelBatch> sendToInternalQueueAndUpdatePostelBatchStatus(CallbackRequestData callbackRequestData, FileDownloadInfo download, PostelBatch postelBatch) {
        return sendToInputQueue(callbackRequestData, download)
                .flatMap(sendMessageResponse -> {
                    postelBatch.setStatus(BatchStatus.WORKED.name());
                    return postelBatchRepository.update(postelBatch);
                });
    }

    private Mono<SendMessageResponse> sendToInputQueue(CallbackRequestData callbackRequestData, FileDownloadInfo fileDownloadInfo) {
        PostelCallbackSqsDto postelCallbackSqsDto = PostelCallbackSqsDto.builder()
                .fileKeyInput(callbackRequestData.getFileKeyInput())
                .fileKeyOutput(callbackRequestData.getFileKeyOutput())
                .build();

        if (fileDownloadInfo != null) {
            postelCallbackSqsDto.setFileOutputUrl(fileDownloadInfo.getUrl());
        }

        return sqsService.pushToInputQueue(postelCallbackSqsDto, AM_POSTEL_CALLBACK_EVENTTYPE);
    }

    public Mono<FileDownloadResponse> getFile(String fileKey, String pnAddressManagerCxId) {
        return safeStorageService.getFile(fileKey, pnAddressManagerCxId);
    }
}
