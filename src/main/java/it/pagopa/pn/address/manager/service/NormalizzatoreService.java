package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Service
@CustomLog
public class NormalizzatoreService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final UploadDownloadClient uploadDownloadClient;
    private final NormalizzatoreConverter normalizzatoreConverter;
    private final PostelBatchService postelBatchService;

    public NormalizzatoreService (PnSafeStorageClient pnSafeStorageClient,
                                  UploadDownloadClient uploadDownloadClient,
                                  NormalizzatoreConverter normalizzatoreConverter
            , PostelBatchService postelBatchService) {
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.uploadDownloadClient = uploadDownloadClient;
        this.normalizzatoreConverter = normalizzatoreConverter;
        this.postelBatchService = postelBatchService;
    }
    public Mono<PreLoadResponseData> presignedUploadRequest (PreLoadRequestData request, String pnAddressManagerCxId) {
        return Flux.fromStream(request.getPreloads().stream())
                .flatMap(preload -> {
                    log.info("preloadDocuments contentType:{} preloadIdx:{}", preload.getContentType(), preload.getPreloadIdx());
                    FileCreationRequestDto fileCreationRequest = normalizzatoreConverter.preLoadRequestToFileCreationRequestDto(preload);
                    return pnSafeStorageClient.createFile(fileCreationRequest, pnAddressManagerCxId)
                            .map(v -> normalizzatoreConverter.fileDownloadResponseDtoToFileDownloadResponse(v, preload.getPreloadIdx()));
                }).collectList()
                .map(normalizzatoreConverter::collectPreLoadRequestToPreLoadRequestData);
    }
    public Mono<FileDownloadResponse> getFile (String fileKey, String pnAddressManagerCxId) {
        return pnSafeStorageClient.getFile(fileKey, pnAddressManagerCxId)
                .map(normalizzatoreConverter::fileDownloadResponseDtoToFileDownloadResponse);
    }

    public Mono<CallbackResponseData> callbackNormalizedAddress(CallbackRequestData v, String pnAddressManagerCxId) {
        return pnSafeStorageClient.getFile(v.getFileKeyOutput(), pnAddressManagerCxId)
                .map(normalizzatoreConverter::fileDownloadResponseDtoToFileDownloadResponse)
                .doOnNext(fileDownloadResponse -> {
                    log.info("callbackNormalizedAddress fileDownloadResponse:{}", fileDownloadResponse);
                    uploadDownloadClient.downloadContent(fileDownloadResponse.getDownload().getUrl())
                            .doOnNext(z -> postelBatchService.getResponsesFromCsv(z, v.getFileKeyInput()));
                    // gestione documento non disponibili (?)
                })
                .map(fileDownloadResponse -> {
                    CallbackResponseData callbackResponseData = new CallbackResponseData();
                    callbackResponseData.setResponse(CallbackResponseData.ResponseEnum.OK);

                    return callbackResponseData;
                })
                .onErrorResume(throwable -> {
                    log.error("callbackNormalizedAddress error:{}", throwable.getMessage(), throwable);
                    CallbackResponseData callbackResponseData = new CallbackResponseData();
                    callbackResponseData.setResponse(CallbackResponseData.ResponseEnum.KO);
                    return Mono.just(callbackResponseData);
                });
    }
}
