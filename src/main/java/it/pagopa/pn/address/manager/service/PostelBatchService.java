package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.entity.CountryModel;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.CapRepository;
import it.pagopa.pn.address.manager.repository.CountryRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final CapRepository capRepository;
    private final CountryRepository countryRepository;

    public PostelBatchService(AddressBatchRequestRepository addressBatchRequestRepository,
                              PostelBatchRepository postelBatchRepository,
                              CsvService csvService,
                              AddressUtils addressUtils,
                              UploadDownloadClient uploadDownloadClient,
                              AddressBatchRequestService addressBatchRequestService,
                              CapRepository capRepository,
                              CountryRepository countryRepository) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.postelBatchRepository = postelBatchRepository;
        this.csvService = csvService;
        this.addressUtils = addressUtils;
        this.uploadDownloadClient = uploadDownloadClient;
        this.addressBatchRequestService = addressBatchRequestService;
        this.capRepository = capRepository;
        this.countryRepository = countryRepository;
    }

    public Mono<Void> getResponse(String url, PostelBatch postelBatch) {
        return uploadDownloadClient.downloadContent(url)
                .flatMap(bytes -> {
                    List<NormalizedAddress> normalizedAddressList = csvService.readItemsFromCsv(NormalizedAddress.class, bytes, 1);
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
            batchRequest.setStatus(BatchStatus.NOT_WORKED.name());
        }
        return batchRequest;
    }

    public Mono<PostelBatch> findPostelBatch(String fileKey) {
        return postelBatchRepository.findByFileKey(fileKey);
    }

    private String verifyPostelAddressResponse(List<NormalizedAddress> normalizedAddresses, String correlationId) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        normalizeItemsResult.setCorrelationId(correlationId);
        normalizeItemsResult.setResultItems(addressUtils.toResultItem(normalizedAddresses));
        verifyCapAndCountry(normalizeItemsResult.getResultItems());
        return addressUtils.toJson(normalizeItemsResult);
    }

    private void verifyCapAndCountry(List<NormalizeResult> resultItems) {
        resultItems.forEach(item -> {
                    if (org.apache.commons.lang3.StringUtils.isBlank(item.getNormalizedAddress().getCountry())
                            || item.getNormalizedAddress().getCountry().toUpperCase().trim().startsWith("ITAL")) {
                        verifyCap(item.getNormalizedAddress().getCap())
                                .onErrorResume(throwable -> {
                                    log.error("Verify cap in whitelist result: {}", throwable.getMessage());
                                    item.setError(throwable.getMessage());
                                    return Mono.empty();
                                });
                    } else {
                        verifyCountry(item.getNormalizedAddress().getCountry())
                                .onErrorResume(throwable -> {
                                    log.error("Verify country in whitelist result: {}", throwable.getMessage());
                                    item.setError(throwable.getMessage());
                                    return Mono.empty();
                                });
                    }
                });
    }

    private Mono<CountryModel> verifyCountry(String country) {
        return countryRepository.findByName(country)
                .switchIfEmpty(Mono.error(new Throwable("Country is not present in whitelist")));
    }

    private Mono<CapModel> verifyCap(String cap) {
        return capRepository.findValidCap(cap)
                .switchIfEmpty(Mono.error(new Throwable("Cap is not present in whitelist")))
                .flatMap(this::checkValidity);
    }

    private Mono<CapModel> checkValidity(CapModel capModel) {
        LocalDateTime now = LocalDateTime.now();
        if(capModel.getStartValidity() != null && capModel.getStartValidity().isAfter(now)){
            return Mono.error(new Throwable(String.format("Cap is present in whitelist but start validity date is %s", capModel.getStartValidity())));
        } else if (capModel.getEndValidity() != null && capModel.getEndValidity().isBefore(now)){
            return Mono.error(new Throwable(String.format("Cap is present in whitelist but end validity date is %s", capModel.getEndValidity())));
        }
        return Mono.just(capModel);
    }

}
