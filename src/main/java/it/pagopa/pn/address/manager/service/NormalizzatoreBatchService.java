package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnFileNotFoundException;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
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
import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.SEMANTIC_ERROR_CODE;
import static it.pagopa.pn.address.manager.constant.BatchStatus.TAKEN_CHARGE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_ADDRESS_MANAGER_POSTELOUTPUTFILEKEYNOTFOUND;
import static java.util.stream.Collectors.groupingBy;

@Component
@CustomLog
@RequiredArgsConstructor
public class NormalizzatoreBatchService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final CsvService csvService;
    private final AddressUtils addressUtils;
    private final UploadDownloadClient uploadDownloadClient;
    private final PnRequestService pnRequestService;
    private final CapAndCountryService capAndCountryService;
    private final Clock clock;
    private final SafeStorageService safeStorageService;
    private final PnAddressManagerConfig pnAddressManagerConfig;


    public Mono<Void> getResponse(String outputFileKey, NormalizzatoreBatch normalizzatoreBatch) {
        return getFile(outputFileKey)
                .flatMap(fileDownloadResponse -> downloadCSV(fileDownloadResponse.getDownload().getUrl(), normalizzatoreBatch)
                        .flatMap(bytes -> {
                            List<NormalizedAddress> normalizedAddressList = csvService.readItemsFromCsv(NormalizedAddress.class, bytes, 0);
                            Map<String, List<NormalizedAddress>> map = normalizedAddressList.stream().collect(groupingBy(normalizedAddress -> addressUtils.getCorrelationIdCreatedAt(normalizedAddress.getId())));
                            return retrieveAndProcessRelatedRequest(normalizzatoreBatch.getBatchId(), map);
                        }))
                .onErrorResume(throwable -> {
                    log.warn("Error in getResponse with postelBatch: {}. Increment", normalizzatoreBatch.getBatchId(), throwable);
                    return pnRequestService.incrementAndCheckRetry(normalizzatoreBatch, throwable);
                });
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

    public Mono<byte[]> downloadCSV(String url, NormalizzatoreBatch normalizzatoreBatch) {
        return uploadDownloadClient.downloadContent(url)
                .doOnNext(bytes -> log.debug("Downloaded CSV for batchId: {}", normalizzatoreBatch.getBatchId()))
                .doOnError(throwable -> {
                    log.warn("Error in getResponse with postelBatch: {}. Increment", normalizzatoreBatch.getBatchId(), throwable);
                    pnRequestService.incrementAndCheckRetry(normalizzatoreBatch, throwable).block();
                });
    }

    private Mono<Void> retrieveAndProcessRelatedRequest(String batchId, Map<String, List<NormalizedAddress>> map) {
        return Flux.defer(() -> processBatchRequests(batchId, map, getBatchRequestByBatchIdAndStatusReactive(Map.of(), batchId)))
                .then();
    }

    private Mono<Page<PnRequest>> processBatchRequests(String batchId, Map<String, List<NormalizedAddress>> map, Mono<Page<PnRequest>> pageMono) {
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

    private Mono<Page<PnRequest>> getBatchRequestByBatchIdAndStatusReactive(Map<String, AttributeValue> lastEvaluatedKey, String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(lastEvaluatedKey, batchId, BatchStatus.WORKING);
    }

    private Mono<Void> processCallbackResponse(List<PnRequest> items, String batchId, Map<String, List<NormalizedAddress>> map ) {
        return Flux.fromIterable(items)
                .flatMap(batchRequest -> retrieveNormalizedAddressAndSetToBatchRequestMessage(batchRequest, map))
                .collectList()
                .flatMap(batchRequestList -> pnRequestService.updateBatchRequest(batchRequestList, batchId));
    }

    private Mono<PnRequest> retrieveNormalizedAddressAndSetToBatchRequestMessage(PnRequest pnRequest, Map<String, List<NormalizedAddress>> map) {
        log.info("Start check postel response for normalizeRequest with correlationId: [{}]", pnRequest.getCorrelationId());
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, pnRequest.getCorrelationId());
        Mono<PnRequest> pnRequestMono = Mono.fromCallable(() -> {
            String correlationIdCreatedAt = addressUtils.getCorrelationIdCreatedAt(pnRequest);
            if (map.get(correlationIdCreatedAt) != null
                    && map.get(correlationIdCreatedAt).size() == addressUtils.getNormalizeRequestFromBatchRequest(pnRequest).size()) {
                log.info("Postel response for request with correlationId: [{}] and createdAt: [{}] is complete", pnRequest.getCorrelationId(), pnRequest.getCreatedAt());
                pnRequest.setStatus(BatchStatus.WORKED.name());
                pnRequest.setMessage(verifyPostelAddressResponseAndRetrieveMessage(map.get(correlationIdCreatedAt), pnRequest));
            } else {
                log.error("Postel response for request with correlationId: [{}] is not complete", pnRequest.getCorrelationId());
                pnRequest.setStatus(TAKEN_CHARGE.name());
            }
            return pnRequest;
        });
        return MDCUtils.addMDCToContextAndExecute(pnRequestMono);
    }

    public Mono<NormalizzatoreBatch> findPostelBatch(String requestId) {
        return postelBatchRepository.findByBatchId(requestId);
    }

    private String verifyPostelAddressResponseAndRetrieveMessage(List<NormalizedAddress> normalizedAddresses, PnRequest pnRequest) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, pnRequest.getCorrelationId());
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        normalizeItemsResult.setCorrelationId(pnRequest.getCorrelationId());
        normalizeItemsResult.setResultItems(addressUtils.toResultItem(normalizedAddresses, pnRequest));
        final Mono<String> resultVerify = Flux.fromIterable(normalizeItemsResult.getResultItems())
                .flatMap(capAndCountryService::verifyCapAndCountryList)
                .collectList()
                .map(addressUtils::toJson);

        return MDCUtils.addMDCToContextAndExecute(resultVerify).block();
    }

    public Mono<Void> resetRelatedBatchRequestForRetry(NormalizzatoreBatch normalizzatoreBatch) {
        normalizzatoreBatch.setStatus(BatchStatus.ERROR.name());
        return postelBatchRepository.update(normalizzatoreBatch)
                .flatMap(this::updateBatchRequest)
                .then();

    }

    private Mono<Void> updateBatchRequest(NormalizzatoreBatch batch) {
        Page<PnRequest> page;
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

    private Mono<Void> updateRequestAndIncrementRetry(List<PnRequest> items, String batchId) {
        return Flux.fromIterable(items)
                .map(item -> {
                    item.setStatus(TAKEN_CHARGE.getValue());
                    return item;
                })
                .collectList()
                .flatMap(batchRequests -> pnRequestService.incrementAndCheckRetry(batchRequests, null, batchId));
    }

    private Page<PnRequest> getBatchRequestByBatchIdAndStatus(Map<String, AttributeValue> lastEvaluatedKey, String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(lastEvaluatedKey, batchId, BatchStatus.WORKING)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn(ADDRESS_NORMALIZER_ASYNC + "can not get batch request - DynamoDB Mono<Page> is null");
                    return new PostelException(ADDRESS_NORMALIZER_ASYNC + "can not get batch request");
                });
    }
}
