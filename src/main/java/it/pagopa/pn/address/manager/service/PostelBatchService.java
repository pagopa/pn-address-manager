package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static it.pagopa.pn.address.manager.constant.BatchStatus.TAKEN_CHARGE;
import static java.util.stream.Collectors.groupingBy;

@Component
@Slf4j
public class PostelBatchService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final CsvService csvService;
    private final AddressUtils addressUtils;
    private final UploadDownloadClient uploadDownloadClient;

    private final AddressBatchRequestService addressBatchRequestService;
    private final CapAndCountryService capAndCountryService;

    public PostelBatchService(AddressBatchRequestRepository addressBatchRequestRepository,
                              PostelBatchRepository postelBatchRepository,
                              CsvService csvService,
                              AddressUtils addressUtils,
                              UploadDownloadClient uploadDownloadClient,
                              AddressBatchRequestService addressBatchRequestService,
                              CapAndCountryService capAndCountryService) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.postelBatchRepository = postelBatchRepository;
        this.csvService = csvService;
        this.addressUtils = addressUtils;
        this.uploadDownloadClient = uploadDownloadClient;
        this.addressBatchRequestService = addressBatchRequestService;
        this.capAndCountryService = capAndCountryService;
    }

    public Mono<Void> getResponse(String url, PostelBatch postelBatch) {
        return uploadDownloadClient.downloadContent(url)
                .flatMap(bytes -> {
                    List<NormalizedAddress> normalizedAddressList = csvService.readItemsFromCsv(NormalizedAddress.class, bytes, 0);
                    Map<String, List<NormalizedAddress>> map = normalizedAddressList.stream().collect(groupingBy(normalizedAddress -> addressUtils.getCorrelationId(normalizedAddress.getId())));
                    return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(postelBatch.getBatchId(), BatchStatus.WORKING)
                            .flatMapIterable(requests -> requests)
                            .map(batchRequest -> retrieveNormalizedAddressAndSetToBatchRequestMessage(batchRequest, map))
                            .collectList()
                            .flatMap(batchRequestList -> addressBatchRequestService.updateBatchRequest(batchRequestList, postelBatch.getBatchId()));
                })
                .then();
    }

    private BatchRequest retrieveNormalizedAddressAndSetToBatchRequestMessage(BatchRequest batchRequest, Map<String, List<NormalizedAddress>> map) {
        log.info("Start check postel response for normalizeRequest with correlationId: [{}]", batchRequest.getCorrelationId());
        if (map.get(batchRequest.getCorrelationId()) != null
                && map.get(batchRequest.getCorrelationId()).size() == addressUtils.getNormalizeRequestFromBatchRequest(batchRequest).size()) {
            log.info("Postel response for request with correlationId: [{}] is complete", batchRequest.getCorrelationId());
            batchRequest.setStatus(BatchStatus.WORKED.name());
            batchRequest.setMessage(verifyPostelAddressResponse(map.get(batchRequest.getCorrelationId()), batchRequest.getCorrelationId()));
        } else {
            log.error("Postel response for request with correlationId: [{}] is not complete", batchRequest.getCorrelationId());
            batchRequest.setStatus(TAKEN_CHARGE.name());
        }
        return batchRequest;
    }

    public Mono<PostelBatch> findPostelBatch(String fileKey) {
        return postelBatchRepository.findByBatchId(fileKey);
    }

    private String verifyPostelAddressResponse(List<NormalizedAddress> normalizedAddresses, String correlationId) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        normalizeItemsResult.setCorrelationId(correlationId);
        normalizeItemsResult.setResultItems(addressUtils.toResultItem(normalizedAddresses));
        return Flux.fromIterable(normalizeItemsResult.getResultItems())
                .flatMap(capAndCountryService::verifyCapAndCountryList)
                .collectList()
                .map(addressUtils::toJson)
                .block();
    }

    public Mono<Void> resetRelatedBatchRequestForRetry(PostelBatch postelBatch) {
        postelBatch.setStatus(BatchStatus.ERROR.name());
        return postelBatchRepository.update(postelBatch)
                .flatMap(this::updateBatchRequest)
                .then();

    }

    private Mono<Void> updateBatchRequest(PostelBatch batch) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(batch.getBatchId(), BatchStatus.WORKING)
                .flatMap(batchRequests -> {
                    batchRequests.forEach(batchRequest -> batchRequest.setStatus(TAKEN_CHARGE.getValue()));
                    return addressBatchRequestService.incrementAndCheckRetry(batchRequests, null, batch.getBatchId()).then();
                });
    }
}
