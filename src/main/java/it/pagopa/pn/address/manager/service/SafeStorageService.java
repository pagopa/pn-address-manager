package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.middleware.client.safestorage.PnSafeStorageClient;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.NormalizzatoreConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.model.NormalizeRequestPostelInput;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SafeStorageService {
    private final CsvService csvService;
    private final PnSafeStorageClient pnSafeStorageClient;
    private final UploadDownloadClient uploadDownloadClient;

    private final AddressUtils addressUtils;

    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final NormalizzatoreConverter normalizzatoreConverter;

    public SafeStorageService(CsvService csvService,
                              PnSafeStorageClient pnSafeStorageClient,
                              UploadDownloadClient uploadDownloadClient,
                              AddressUtils addressUtils,
                              PnAddressManagerConfig pnAddressManagerConfig,
                              NormalizzatoreConverter normalizzatoreConverter) {
        this.csvService = csvService;
        this.pnSafeStorageClient = pnSafeStorageClient;
        this.uploadDownloadClient = uploadDownloadClient;
        this.addressUtils = addressUtils;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.normalizzatoreConverter = normalizzatoreConverter;
    }

    public Mono<FileCreationResponseDto> callSelfStorageCreateFileAndUpload(List<BatchRequest> requests) {

        FileCreationRequestDto fileCreationRequestDto = addressUtils.getFileCreationRequest();
        List<NormalizeRequestPostelInput> listToConvert = new ArrayList<>();
        requests.forEach(batchRequest ->
                listToConvert.addAll(addressUtils.normalizeRequestToPostelCsvRequest(batchRequest)));

        String csvContent = csvService.writeItemsOnCsvToString(listToConvert);
        String sha256 = addressUtils.computeSha256(csvContent.getBytes());

        return pnSafeStorageClient.createFile(fileCreationRequestDto, pnAddressManagerConfig.getPagoPaCxId())
                .doOnNext(fileCreationResponseDto -> uploadDownloadClient.uploadContent(csvContent, fileCreationResponseDto, sha256))
                .onErrorResume(e -> {
                    log.error("ADDRESS MANAGER -> POSTEL - failed to create file", e);
                    return Mono.error(e);
                });
    }

    public Mono<FileDownloadResponse> getFile(String fileKey, String pnAddressManagerCxId) {
        return pnSafeStorageClient.getFile(fileKey, pnAddressManagerCxId)
                .map(normalizzatoreConverter::fileDownloadResponseDtoToFileDownloadResponse);

    }
}
