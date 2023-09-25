package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.*;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@CustomLog
public class NormalizzatoreService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final NormalizzatoreConverter normalizzatoreConverter;

    public NormalizzatoreService (PnSafeStorageClient pnSafeStorageClient, NormalizzatoreConverter normalizzatoreConverter) {
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.normalizzatoreConverter = normalizzatoreConverter;
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

        return null;
    }
}
