package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.ADDRESS_NORMALIZER_SYNC;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;


@Slf4j
@Service
public class NormalizeAddressService {

    private final EventService eventService;
    private final AddressUtils addressUtils;
    private final SqsService sqsService;
    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final PostelBatchService postelBatchService;

    private static final String AM_NORMALIZE_INPUT_EVENTTYPE = "AM_NORMALIZE_INPUT";

    public NormalizeAddressService(AddressUtils addressUtils,
                                   EventService eventService,
                                   SqsService sqsService,
                                   AddressBatchRequestRepository addressBatchRequestRepository,
                                   ApiKeyRepository apiKeyRepository,
                                   PnAddressManagerConfig pnAddressManagerConfig,
                                   PostelBatchService batchService) {
        this.eventService = eventService;
        this.addressUtils = addressUtils;
        this.sqsService = sqsService;
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.postelBatchService = batchService;
    }

    public Mono<ApiKeyModel> checkApiKey(String cxId, String xApiKey) {
        return apiKeyRepository.findById(cxId)
                .filter(apiKeyModel -> apiKeyModel.getApiKey().equalsIgnoreCase(xApiKey))
                .switchIfEmpty(Mono.error(new PnAddressManagerException(APIKEY_DOES_NOT_EXISTS, APIKEY_DOES_NOT_EXISTS, HttpStatus.FORBIDDEN.value(), "Api Key not found")));

    }

    public Mono<AcceptedResponse> normalizeAddress(String xApiKey, String cxId, NormalizeItemsRequest normalizeItemsRequest) {
        return checkApiKey(cxId, xApiKey)
                .doOnNext(apiKeyModel -> log.info(ADDRESS_NORMALIZER_SYNC + "Founded apikey for request: [{}]", normalizeItemsRequest.getCorrelationId()))
                .flatMap(apiKeyModel -> sqsService.pushToInputQueue(InternalCodeSqsDto.builder()
                                .xApiKey(xApiKey)
                                .pnAddressManagerCxId(cxId)
                                .normalizeItemsRequest(normalizeItemsRequest)
                                .build(), cxId, AM_NORMALIZE_INPUT_EVENTTYPE)
                        .map(sendMessageResponse -> {
                            log.info(ADDRESS_NORMALIZER_SYNC + "Sent request with correlationId: [{}] to {}", normalizeItemsRequest.getCorrelationId(), pnAddressManagerConfig.getSqs().getInputQueueName());
                            return addressUtils.mapToAcceptedResponse(normalizeItemsRequest);
                        })
                        .doOnError(throwable -> {
                            log.error(ADDRESS_NORMALIZER_SYNC + "Failed to sendRequest with correlationId: [{}] to inputQueue", normalizeItemsRequest.getCorrelationId());
                            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_NORMALIZE_ADDRESS, ERROR_CODE_ADDRESS_MANAGER_NORMALIZE_ADDRESS);
                        }));
    }

    public Mono<Void> handleRequest(PnNormalizeRequestEvent.Payload payload) {
        if (Boolean.TRUE.equals(pnAddressManagerConfig.getFlagCsv())) {
            return Mono.fromCallable(() -> addressUtils.normalizeRequestToResult(payload.getNormalizeItemsRequest()))
                    .doOnNext(normalizeItemsResult -> {
                        log.info("normalizeAddressAsync response: {}", normalizeItemsResult);
                        sendEvents(normalizeItemsResult, payload.getPnAddressManagerCxId());
                    })
                    .doOnError(throwable -> log.error("normalizeAddressAsync error: {}", throwable.getMessage(), throwable))
                    .then();

        } else {
            return createBatchRequest(payload.getPnAddressManagerCxId(), payload.getNormalizeItemsRequest(), payload.getNormalizeItemsRequest().getCorrelationId())
                    .doOnNext(batchRequest -> log.info(ADDRESS_NORMALIZER_ASYNC + "Created Batch Request for correlationId: {}", payload.getNormalizeItemsRequest().getCorrelationId()))
                    .doOnError(throwable -> log.error(ADDRESS_NORMALIZER_ASYNC + "Failed to create Batch Request correlationId: {}", payload.getNormalizeItemsRequest().getCorrelationId()))
                    .then();
        }
    }

    public Mono<Void> handlePostelCallback(PnPostelCallbackEvent.Payload payload) {
        return postelBatchService.findPostelBatch(payload.getRequestId())
                .flatMap(postelBatch -> {
                    if (StringUtils.hasText(payload.getError())) {
                        return postelBatchService.resetRelatedBatchRequestForRetry(postelBatch);
                    }
                    return postelBatchService.getResponse(payload.getOutputFileUrl(), postelBatch);
                });
    }

    public Mono<BatchRequest> createBatchRequest(String pnAddressManagerCxId, NormalizeItemsRequest normalizeItemsRequest, String correlationId) {
        BatchRequest batchRequest = addressUtils.createNewStartBatchRequest();
        String requestAsString = addressUtils.toJson(normalizeItemsRequest.getRequestItems());
        batchRequest.setAddresses(requestAsString);
        batchRequest.setClientId(pnAddressManagerCxId);
        batchRequest.setCorrelationId(correlationId);
        return addressBatchRequestRepository.create(batchRequest);
    }

    private void sendEvents(NormalizeItemsResult normalizeItemsResult, String cxId) {
        EventDetail eventDetail = new EventDetail(normalizeItemsResult, cxId);
        String message = addressUtils.toJson(eventDetail);
        eventService.sendEvent(message, normalizeItemsResult.getCorrelationId());
    }
}