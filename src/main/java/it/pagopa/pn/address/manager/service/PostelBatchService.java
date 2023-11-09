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
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.BatchStatus.TAKEN_CHARGE;
import static java.util.stream.Collectors.groupingBy;

@Component
@CustomLog
@RequiredArgsConstructor
public class PostelBatchService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final CsvService csvService;
    private final AddressUtils addressUtils;
    private final UploadDownloadClient uploadDownloadClient;
    private final AddressBatchRequestService addressBatchRequestService;
    private final CapAndCountryService capAndCountryService;
    private final Clock clock;


    public Mono<Void> getResponse(String url, PostelBatch postelBatch) {
        return uploadDownloadClient.downloadContent(url)
                .flatMap(bytes -> {
                    List<NormalizedAddress> normalizedAddressList = csvService.readItemsFromCsv(NormalizedAddress.class, bytes, 0);
                    Map<String, List<NormalizedAddress>> map = normalizedAddressList.stream().collect(groupingBy(normalizedAddress -> addressUtils.getCorrelationIdCreatedAt(normalizedAddress.getId())));
                    return retrieveAndProcessRelatedRequest(postelBatch.getBatchId(), map);
                })
                .onErrorResume(throwable -> {
                    log.warn("Error in getResponse with postelBatch: {}. Increment", postelBatch.getBatchId(), throwable);
                    return addressBatchRequestService.incrementAndCheckRetry(postelBatch, throwable);
                })
                .then();
    }

    private Mono<Void> retrieveAndProcessRelatedRequest(String batchId, Map<String, List<NormalizedAddress>> map) {
        return Flux.defer(() -> processBatchRequests(batchId, map, getBatchRequestByBatchIdAndStatusReactive(Map.of(), batchId)))
                .then();
    }

    private Mono<Page<BatchRequest>> processBatchRequests(String batchId, Map<String, List<NormalizedAddress>> map, Mono<Page<BatchRequest>> pageMono) {
        return pageMono.flatMap(page -> {
            if (page.items().isEmpty()) {
                log.info(ADDRESS_NORMALIZER_ASYNC + "no batch request found for: {}", batchId);
                return Mono.just(page);
            }

            Instant startPagedQuery = clock.instant();
            return processCallbackResponse(page.items(), batchId, map)
                    .thenReturn(page)
                    .doOnTerminate(() -> {
                        Duration timeSpent = AddressUtils.getTimeSpent(startPagedQuery);
                        log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId: [{}] end internal query. Time spent is {} millis", batchId, timeSpent.toMillis());
                    })
                    .flatMap(lastProcessedPage -> {
                        if (lastProcessedPage.lastEvaluatedKey() != null) {
                            return processBatchRequests(batchId, map, getBatchRequestByBatchIdAndStatusReactive(lastProcessedPage.lastEvaluatedKey(), batchId));
                        } else {
                            return Mono.just(lastProcessedPage);
                        }
                    });
        });
    }

    private Mono<Void> processCallbackResponse(List<BatchRequest> items, String batchId, Map<String, List<NormalizedAddress>> map ) {
        return Flux.fromIterable(items)
                .map(batchRequest -> retrieveNormalizedAddressAndSetToBatchRequestMessage(batchRequest, map))
                .collectList()
                .flatMap(batchRequestList -> addressBatchRequestService.updateBatchRequest(batchRequestList, batchId));
    }

    private BatchRequest retrieveNormalizedAddressAndSetToBatchRequestMessage(BatchRequest batchRequest, Map<String, List<NormalizedAddress>> map) {
        log.info("Start check postel response for normalizeRequest with correlationId: [{}]", batchRequest.getCorrelationId());
        String correlationIdCreatedAt = addressUtils.getCorrelationIdCreatedAt(batchRequest);
        if (map.get(correlationIdCreatedAt) != null
                && map.get(correlationIdCreatedAt).size() == addressUtils.getNormalizeRequestFromBatchRequest(batchRequest).size()) {
            log.info("Postel response for request with correlationId: [{}] and createdAt: [{}] is complete", batchRequest.getCorrelationId(), batchRequest.getCreatedAt());
            batchRequest.setStatus(BatchStatus.WORKED.name());
            batchRequest.setMessage(verifyPostelAddressResponse(map.get(correlationIdCreatedAt), batchRequest.getCorrelationId()));
        } else {
            log.error("Postel response for request with correlationId: [{}] is not complete", batchRequest.getCorrelationId());
            batchRequest.setStatus(TAKEN_CHARGE.name());
        }
        return batchRequest;
    }

    public Mono<PostelBatch> findPostelBatch(String requestId) {
        return postelBatchRepository.findByBatchId(requestId);
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
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();

        do {
            Instant startPagedQuery = clock.instant();

            page = getBatchRequestByBatchIdAndStatus(lastEvaluatedKey, batch.getBatchId());

            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                updateRequestAndIncrementRetry(page.items(), batch.getBatchId())
                        .block();
            } else {
                log.info(ADDRESS_NORMALIZER_ASYNC + "no batch request available");
            }

            Duration timeSpent = AddressUtils.getTimeSpent(startPagedQuery);

            log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId: [{}] end internal query. Time spent is {} millis", batch.getBatchId(), timeSpent.toMillis());

        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));

        return Mono.empty();
    }

    private Mono<Void> updateRequestAndIncrementRetry(List<BatchRequest> items, String batchId) {
        return Flux.fromIterable(items)
                .map(item -> {
                    item.setStatus(TAKEN_CHARGE.getValue());
                    return item;
                })
                .collectList()
                .flatMap(batchRequests -> addressBatchRequestService.incrementAndCheckRetry(batchRequests, null, batchId));
    }

    private Page<BatchRequest> getBatchRequestByBatchIdAndStatus(Map<String, AttributeValue> lastEvaluatedKey, String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(lastEvaluatedKey, batchId, BatchStatus.WORKING)
                .block();
    }

    private Mono<Page<BatchRequest>> getBatchRequestByBatchIdAndStatusReactive(Map<String, AttributeValue> lastEvaluatedKey, String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(lastEvaluatedKey, batchId, BatchStatus.WORKING);
    }
}
