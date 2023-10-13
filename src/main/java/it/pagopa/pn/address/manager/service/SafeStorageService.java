package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.SAFE_STORAGE_URL_PREFIX;

@Service
@Slf4j
public class SafeStorageService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final UploadDownloadClient uploadDownloadClient;

    private final AddressUtils addressUtils;

    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final NormalizzatoreConverter normalizzatoreConverter;
    public SafeStorageService(PnSafeStorageClient pnSafeStorageClient,
                              UploadDownloadClient uploadDownloadClient,
                              AddressUtils addressUtils,
                              PnAddressManagerConfig pnAddressManagerConfig,
                              NormalizzatoreConverter normalizzatoreConverter) {
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.uploadDownloadClient = uploadDownloadClient;
        this.addressUtils = addressUtils;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.normalizzatoreConverter = normalizzatoreConverter;
    }

    public Mono<FileCreationResponseDto> callSelfStorageCreateFileAndUpload(String csvContent, String sha256) {

        FileCreationRequestDto fileCreationRequestDto = addressUtils.getFileCreationRequest();

        return pnSafeStorageClient.createFile(fileCreationRequestDto, pnAddressManagerConfig.getPagoPaCxId(), sha256)
                .flatMap(fileCreationResponseDto -> uploadDownloadClient.uploadContent(csvContent, fileCreationResponseDto, sha256)
                        .doOnNext(response -> log.info("file {} uploaded", fileCreationResponseDto.getKey()))
                        .thenReturn(fileCreationResponseDto))
                .onErrorResume(e -> {
                    log.error(ADDRESS_NORMALIZER_ASYNC + "failed to create file", e);
                    return Mono.error(e);
                });
    }

    public Mono<FileDownloadResponse> getFile(String fileKey, String pnAddressManagerCxId) {
        String finalFileKey = fileKey.replace(SAFE_STORAGE_URL_PREFIX, "");
        return pnSafeStorageClient.getFile(finalFileKey, pnAddressManagerCxId)
                .map(normalizzatoreConverter::fileDownloadResponseDtoToFileDownloadResponse);

    }
}
