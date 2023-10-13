package it.pagopa.pn.address.manager.service;

import com.amazonaws.services.eventbridge.model.PutEventsResult;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.PostelClient;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.model.NormalizeRequestPostelInput;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.BatchSendStatus.NOT_SENT;
import static it.pagopa.pn.address.manager.constant.BatchSendStatus.SENT;
import static it.pagopa.pn.address.manager.constant.BatchStatus.*;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@Slf4j
public class AddressBatchRequestService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final AddressConverter addressConverter;
    private final SqsService sqsService;
    private final PostelClient postelClient;
    private final SafeStorageService safeStorageService;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final EventService eventService;
    private final CsvService csvService;
    private final AddressUtils addressUtils;

    public AddressBatchRequestService(AddressBatchRequestRepository addressBatchRequestRepository,
                                      PostelBatchRepository postelBatchRepository,
                                      AddressConverter addressConverter,
                                      SqsService sqsService,
                                      PostelClient postelClient,
                                      SafeStorageService safeStorageService,
                                      PnAddressManagerConfig pnAddressManagerConfig,
                                      EventService eventService,
                                      CsvService csvService,
                                      AddressUtils addressUtils) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.postelBatchRepository = postelBatchRepository;
        this.addressConverter = addressConverter;
        this.safeStorageService = safeStorageService;
        this.sqsService = sqsService;
        this.postelClient = postelClient;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.eventService = eventService;
        this.csvService = csvService;
        this.addressUtils = addressUtils;
    }

    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-request.delay}")
    public void batchAddressRequest() {
        log.trace(ADDRESS_NORMALIZER_ASYNC +  "batchPecRequest start");
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String batchId = UUID.randomUUID().toString();
                execBatchRequest(page.items(), batchId)
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + batchId))
                        .block();
            } else {
                log.info(ADDRESS_NORMALIZER_ASYNC +  "no batch request available");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace(ADDRESS_NORMALIZER_ASYNC +  "batchPecRequest end");
    }


    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, pnAddressManagerConfig.getNormalizer().getBatchRequest().getMaxSize())
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn(ADDRESS_NORMALIZER_ASYNC + "can not get batch request - DynamoDB Mono<Page> is null");
                    return new PostelException(ADDRESS_NORMALIZER_ASYNC + "can not get batch request");
                });
    }

    private Mono<Void> execBatchRequest(List<BatchRequest> items, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<NormalizeRequestPostelInput> listToConvert = new ArrayList<>();
        items.forEach(batchRequest ->
                listToConvert.addAll(addressUtils.normalizeRequestToPostelCsvRequest(batchRequest)));

        String csvContent = csvService.writeItemsOnCsvToString(listToConvert);
        String sha256 = addressUtils.computeSha256(csvContent.getBytes(StandardCharsets.UTF_8));
        String finalBatchId = pnAddressManagerConfig.getNormalizer().getPostel().getRequestPrefix() + batchId;

        return Flux.fromStream(items.stream())
                .doOnNext(item -> {
                    MDC.put("AWS_messageId", item.getAwsMessageId());
                    item.setBatchId(finalBatchId);
                    item.setStatus(TAKEN_CHARGE.name());
                    item.setLastReserved(now);
                })
                .flatMap(item -> addressBatchRequestRepository.setNewBatchIdToBatchRequest(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info(ADDRESS_NORMALIZER_ASYNC + "conditional check failed - skip correlationId: {}", item.getCorrelationId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .collectList()
                .filter(requests -> !requests.isEmpty())
                .zipWhen(batchRequests -> safeStorageService.callSelfStorageCreateFileAndUpload(csvContent, sha256)
                        .onErrorResume(e -> {
                            log.error(ADDRESS_NORMALIZER_ASYNC +  "failed to create file", e);
                            return incrementAndCheckRetry(batchRequests, e, finalBatchId)
                                    .then(Mono.error(e));
                        }))
                .flatMap(t -> activatePostelBatch(t, finalBatchId, sha256));
    }

    private Mono<Void> activatePostelBatch(Tuple2<List<BatchRequest>, FileCreationResponseDto> t, String batchId, String sha256) {
        return createPostelBatch(t.getT2().getKey(), batchId, sha256)
                .onErrorResume(v -> incrementAndCheckRetry(t.getT1(), v, batchId).then(Mono.error(v)))
                .flatMap(this::callPostelActivationApi);
    }

    public Mono<Void> callPostelActivationApi(PostelBatch postelBatch) {
        return postelClient.activatePostel(postelBatch)
                .map(activatePostelResponse -> {
                    log.info(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - called postel activation", postelBatch.getBatchId());
                    if (!StringUtils.hasText(activatePostelResponse.getError())) {
                        return updatePostelBatchToWorking(postelBatch);
                    }
                    throw new PnInternalException(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - Error during call postel activation api", "ADDRESS MANAGER - POSTEL ACTIVATION");
                })
                .doOnError(e -> log.error(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - failed to execute batch", postelBatch.getBatchId(), e))
                .onErrorResume(v -> incrementAndCheckRetry(postelBatch, v).then(Mono.error(v)))
                .then();

    }

    private Mono<Void> updatePostelBatchToWorking(PostelBatch postelBatch) {
        log.info(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - update PostelBatch with status: {}", postelBatch.getBatchId(),BatchStatus.WORKING.getValue());
        postelBatch.setStatus(BatchStatus.WORKING.getValue());
        return postelBatchRepository.update(postelBatch)
                .doOnNext(polling -> log.debug(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - updated PostelBatch with status: {}", postelBatch.getBatchId(), BatchStatus.WORKING.getValue()))
                .then();
    }


    private Mono<PostelBatch> createPostelBatch(String fileKey, String batchId, String sha256) {
        log.info(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - creating PostelBatch with fileKey: {}", batchId, fileKey);
        return postelBatchRepository.create(addressConverter.createPostelBatchByBatchIdAndFileKey(batchId, fileKey, sha256))
                .flatMap(polling -> {
                    log.debug(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - created PostelBatch with fileKey: {}", batchId, fileKey);
                    return setBatchRequestStatusToWorking(batchId)
                            .map(batchRequests -> {
                                log.debug(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - created PostelBatch with fileKey: {}", batchId, fileKey);
                                return polling;
                            })
                            .doOnError(e -> log.warn(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - failed to create PostelBatch with fileKey: {}", batchId, fileKey, e));
                });
    }

    private Mono<List<BatchRequest>> setBatchRequestStatusToWorking(String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(batchId, BatchStatus.TAKEN_CHARGE)
                .doOnNext(requests -> log.debug(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - updating {} requests in status {}", batchId, requests.size(), BatchStatus.WORKING))
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> request.setStatus(BatchStatus.WORKING.getValue()))
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(r -> log.debug(ADDRESS_NORMALIZER_ASYNC +  "correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - failed to set request in status {}", batchId, BatchStatus.WORKING, e))
                .collectList();
    }


    protected Mono<Void> incrementAndCheckRetry(List<BatchRequest> requests, Throwable throwable, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(requests.stream())
                .doOnNext(r -> {
                    int nextRetry = (r.getRetry() != null) ? (r.getRetry() + 1) : 1;
                    r.setRetry(nextRetry);
                    r.setLastReserved(now);
                    if (nextRetry >= pnAddressManagerConfig.getNormalizer().getBatchRequest().getMaxRetry()
                            || (throwable instanceof PnInternalAddressManagerException exception && exception.getStatus() == HttpStatus.BAD_REQUEST.value())) {
                        r.setStatus(BatchStatus.ERROR.getValue());
                        log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - request {} status in {} (retry: {})", batchId, r.getCorrelationId(), r.getStatus(), r.getRetry());
                    }
                })
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(r -> log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - retry incremented for correlationId: {}", batchId, r.getCorrelationId()))
                .doOnError(e -> log.warn(ADDRESS_NORMALIZER_ASYNC + "batchId {} - failed to increment retry", batchId, e))
                .filter(r -> BatchStatus.ERROR.getValue().equals(r.getStatus()))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(l -> {
                    log.debug(ADDRESS_NORMALIZER_ASYNC + "there is at least one request in ERROR - call batch to send to SQS");
                    return sqsService.sendListToDlqQueue(l);
                });
    }

    protected Mono<Void> incrementAndCheckRetry(PostelBatch postelBatch, Throwable throwable) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Mono.just(postelBatch)
                .doOnNext(r -> {
                    int nextRetry = (r.getRetry() != null) ? (r.getRetry() + 1) : 1;
                    r.setRetry(nextRetry);
                    r.setLastReserved(now);
                    if (nextRetry >= pnAddressManagerConfig.getNormalizer().getPostel().getMaxRetry()
                            || (throwable instanceof PnInternalAddressManagerException exception && exception.getStatus() == HttpStatus.BAD_REQUEST.value())) {
                        r.setStatus(BatchStatus.ERROR.getValue());
                        log.debug(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - status in {} (retry: {})", postelBatch.getBatchId(), r.getStatus(), r.getRetry());
                    }
                })
                .flatMap(postelBatchRepository::update)
                .doOnNext(r -> log.debug(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - retry incremented", postelBatch.getBatchId()))
                .doOnError(e -> log.warn(ADDRESS_NORMALIZER_ASYNC +  "batchId {} - failed to increment retry", postelBatch.getBatchId(), e))
                .filter(r -> BatchStatus.ERROR.getValue().equals(r.getStatus()))
                .flatMap(l -> {
                    log.debug(ADDRESS_NORMALIZER_ASYNC +  "there is at least one request in ERROR - call batch to send to SQS");
                    return updateBatchRequest(postelBatch.getBatchId(), WORKING);
                });
    }

    public Mono<Void> updateBatchRequest(String batchId, BatchStatus status) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(batchId, status)
                .flatMapIterable(batchRequests -> batchRequests)
                .flatMap(batchRequest -> {
                    batchRequest.setStatus(ERROR.getValue());
                    return addressBatchRequestRepository.update(batchRequest);
                })
                .doOnNext(request -> log.debug("Normalize Address - correlationId {} - set status in {}", request.getCorrelationId(), request.getStatus()))
                .flatMap(this::sendToEventBridgeOrInDlq)
                .collectList()
                .then();
    }

    public Mono<Void> updateBatchRequest(List<BatchRequest> batchRequests, String batchId) {
        return Flux.fromIterable(batchRequests)
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(request -> log.debug("Normalize Address - correlationId {} - set status in {}", request.getCorrelationId(), request.getStatus()))
                .flatMap(this::sendToEventBridgeOrInDlq)
                .filter(request -> TAKEN_CHARGE.getValue().equalsIgnoreCase(request.getStatus()))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(batchRequestList -> incrementAndCheckRetry(batchRequestList, null, batchId));
    }

    private Mono<BatchRequest> sendToEventBridgeOrInDlq(BatchRequest request) {
        return switch (BatchStatus.fromValue(request.getStatus())) {
            case WORKED -> sendEvents(request, request.getClientId())
                    .doOnNext(putEventsResult -> request.setSendStatus(SENT.getValue()))
                    .doOnError(throwable -> request.setSendStatus(NOT_SENT.getValue()))
                    .flatMap(putEventsResult -> addressBatchRequestRepository.update(request)
                            .doOnNext(item -> log.debug("Normalize Address - correlationId {} - set Send Status in {}", request.getCorrelationId(), request.getStatus())));
            case ERROR -> sqsService.sendToDlqQueue(request)
                    .thenReturn(request);
            default -> Mono.just(request);
        };
    }

    private Mono<PutEventsResult> sendEvents(BatchRequest batchRequest, String cxId) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        List<NormalizeResult> itemsResult = addressUtils.getNormalizeResultFromBatchRequest(batchRequest);
        normalizeItemsResult.setResultItems(itemsResult);
        normalizeItemsResult.setCorrelationId(batchRequest.getCorrelationId());
        EventDetail eventDetail = new EventDetail(normalizeItemsResult, cxId);
        String finalMessage = addressUtils.toJson(eventDetail);
        return eventService.sendEvent(finalMessage, batchRequest.getCorrelationId());
    }
}
