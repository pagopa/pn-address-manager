package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.*;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.*;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;


@CustomLog
@Service
@RequiredArgsConstructor
public class NormalizeAddressService {

    private final EventService eventService;
    private final AddressUtils addressUtils;
    private final SqsService sqsService;
    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final PostelBatchService postelBatchService;

    public Mono<ApiKeyModel> checkApiKey(String cxId, String xApiKey) {
        log.logChecking(PROCESS_CHECKING_APIKEY + ": starting check ApiKey");
        return apiKeyRepository.findById(cxId)
                .switchIfEmpty(Mono.error(new PnInternalAddressManagerException(ERROR_CLIENT_ID_MESSAGE, ERROR_CLIENT_ID_MESSAGE, HttpStatus.FORBIDDEN.value(), ERROR_CLIENT_ID)));

    }

    public Mono<AcceptedResponse> normalizeAddress(String xApiKey, String cxId, NormalizeItemsRequest normalizeItemsRequest) {
        return checkApiKey(cxId, xApiKey)
                .doOnNext(apiKeyModel -> {
                    log.logCheckingOutcome(PROCESS_CHECKING_APIKEY, true);
                    log.info(ADDRESS_NORMALIZER_SYNC + "Founded apikey for request: [{}]", normalizeItemsRequest.getCorrelationId());
                })
                .flatMap(apiKeyModel -> checkFieldsLength(normalizeItemsRequest.getRequestItems(), normalizeItemsRequest.getCorrelationId()).thenReturn(normalizeItemsRequest))
                .flatMap(request -> sendToInputQueue(xApiKey, cxId, request))
                .onErrorResume(throwable -> sendToDlq(xApiKey, cxId, normalizeItemsRequest));
    }

    private Mono<AcceptedResponse> sendToInputQueue(String xApiKey, String cxId, NormalizeItemsRequest request) {
        return sqsService.pushToInputQueue(InternalCodeSqsDto.builder()
                        .xApiKey(xApiKey)
                        .pnAddressManagerCxId(cxId)
                        .normalizeItemsRequest(request)
                        .build(), cxId)
                .map(sendMessageResponse -> {
                    log.info(ADDRESS_NORMALIZER_SYNC + "Sent request with correlationId: [{}] to {}", request.getCorrelationId(), pnAddressManagerConfig.getSqs().getInputQueueName());
                    return addressUtils.mapToAcceptedResponse(request);
                })
                .doOnError(throwable -> {
                    log.error(ADDRESS_NORMALIZER_SYNC + "Failed to sendRequest with correlationId: [{}] to inputQueue", request.getCorrelationId());
                    throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_NORMALIZE_ADDRESS, ERROR_CODE_ADDRESS_MANAGER_NORMALIZE_ADDRESS);
                });
    }

    private Mono<AcceptedResponse> sendToDlq(String xApiKey, String cxId, NormalizeItemsRequest normalizeItemsRequest) {
        return sqsService.pushToInputDlqQueue(InternalCodeSqsDto.builder()
                        .xApiKey(xApiKey)
                        .pnAddressManagerCxId(cxId)
                        .normalizeItemsRequest(normalizeItemsRequest)
                        .build(), cxId)
                .map(sendMessageResponse -> {
                    log.info(ADDRESS_NORMALIZER_SYNC + "Sent request with correlationId: [{}] to {}", normalizeItemsRequest.getCorrelationId(), pnAddressManagerConfig.getSqs().getInputQueueName());
                    return addressUtils.mapToAcceptedResponse(normalizeItemsRequest);
                })
                .doOnError(e -> {
                    log.error(ADDRESS_NORMALIZER_SYNC + "Failed to sendRequest with correlationId: [{}] to inputQueue", normalizeItemsRequest.getCorrelationId());
                    throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_NORMALIZE_ADDRESS, ERROR_CODE_ADDRESS_MANAGER_NORMALIZE_ADDRESS);
                });
    }

    public Mono<Void> handleRequest(PnNormalizeRequestEvent.Payload payload) {
        log.logStartingProcess(PROCESS_SERVICE_NORMALIZE_ADDRESS);
        log.info("Received normalizeAddressAsync request for correlationId: {}", payload.getNormalizeItemsRequest().getCorrelationId());
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
        log.logStartingProcess(PROCESS_SERVICE_POSTEL_CALLBACK);
        log.info("Received postel callback for requestId: {}", payload.getRequestId());
        return postelBatchService.findPostelBatch(payload.getRequestId())
                .flatMap(postelBatch -> {
                    if (StringUtils.hasText(payload.getError())) {
                        return postelBatchService.resetRelatedBatchRequestForRetry(postelBatch);
                    }
                    return postelBatchService.getResponse(payload.getOutputFileKey(), postelBatch);
                });
    }

    public Mono<PnRequest> createBatchRequest(String pnAddressManagerCxId, NormalizeItemsRequest normalizeItemsRequest, String correlationId) {
        PnRequest pnRequest = addressUtils.createNewStartBatchRequest();
        String requestAsString = addressUtils.toJson(normalizeItemsRequest.getRequestItems());
        pnRequest.setAddresses(requestAsString);
        pnRequest.setClientId(pnAddressManagerCxId);
        pnRequest.setCorrelationId(correlationId);
        return addressBatchRequestRepository.create(pnRequest);
    }

    private void sendEvents(NormalizeItemsResult normalizeItemsResult, String cxId) {
        EventDetail eventDetail = new EventDetail(normalizeItemsResult, cxId);
        String message = addressUtils.toJson(eventDetail);
        eventService.sendEvent(message)
                .doOnNext(putEventsResult -> {
                    log.info("Event with correlationId {} sent successfully", normalizeItemsResult.getCorrelationId());
                    log.debug("Sent event result: {}", putEventsResult.getEntries());
                })
                .doOnError(throwable -> log.error("Send event with correlationId {} failed", normalizeItemsResult.getCorrelationId(), throwable));
    }

    public Mono<Void> checkFieldsLength(List<NormalizeRequest> normalizeRequestList, String correlationId) {
        if(pnAddressManagerConfig.getAddressLengthValidation() != 0){
            return normalizeRequestList.stream()
                    .map(NormalizeRequest::getAddress)
                    .map(analogAddress -> validateAddressLength(correlationId, analogAddress))
                    .filter(Boolean.FALSE::equals)
                    .findFirst()
                    .map(aBoolean -> Mono.error(new PnInternalAddressManagerException(INVALID_ADDRESS_FIELD_LENGTH, INVALID_ADDRESS_FIELD_LENGTH, HttpStatus.BAD_REQUEST.value(), INVALID_ADDRESS_FIELD_LENGTH_CODE)))
                    .orElse(Mono.empty())
                    .then();
        }
        return Mono.empty();
    }

    private boolean validateAddressLength(String correlationId, AnalogAddress analogAddress) {
        return validateFieldLength(correlationId, analogAddress.getAddressRow())
                && validateFieldLength(correlationId, analogAddress.getAddressRow2())
                && validateFieldLength(correlationId, analogAddress.getCity())
                && validateFieldLength(correlationId, analogAddress.getCity2())
                && validateFieldLength(correlationId, analogAddress.getCountry())
                && validateFieldLength(correlationId, analogAddress.getPr())
                && validateFieldLength(correlationId, analogAddress.getCap());
    }

    private boolean validateFieldLength(String correlationId, String fieldValue) {
        if(fieldValue == null || fieldValue.length() <= pnAddressManagerConfig.getAddressLengthValidation()){
            return true;
        }else{
            log.warn("Address Validation for CorrelationId: [{}] - Field length violation for value: {}", correlationId, fieldValue);
            return false;
        }
    }
}