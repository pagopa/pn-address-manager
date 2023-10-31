package it.pagopa.pn.address.manager.service;

import com.amazonaws.services.eventbridge.model.PutEventsResult;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnPostelException;
import it.pagopa.pn.address.manager.exception.PostelException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.address.manager.middleware.client.NormalizzatoreClient;
import it.pagopa.pn.address.manager.model.EventDetail;
import it.pagopa.pn.address.manager.model.NormalizeRequestPostelInput;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.CustomLog;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.CONTEXT_BATCH_ID;
import static it.pagopa.pn.address.manager.constant.BatchSendStatus.NOT_SENT;
import static it.pagopa.pn.address.manager.constant.BatchSendStatus.SENT;
import static it.pagopa.pn.address.manager.constant.BatchStatus.*;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_SERVICE_POSTEL_ATTIVAZIONE;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;

@Service
@CustomLog
public class AddressBatchRequestService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final AddressConverter addressConverter;
    private final SqsService sqsService;
    private final NormalizzatoreClient postelClient;
    private final SafeStorageService safeStorageService;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final EventService eventService;
    private final CsvService csvService;
    private final AddressUtils addressUtils;
    private final Clock clock;
    private final List<NormalizeRequestPostelInput> listToConvert = new ArrayList<>();
    private final List<BatchRequest> requestToProcess = new ArrayList<>();
    private final Map<String, List<BatchRequest>> requestToProcessMap = new HashMap<>();
    private final Map<String, List<NormalizeRequestPostelInput>> fileMap = new HashMap<>();
    private String batchId;



    public AddressBatchRequestService(AddressBatchRequestRepository addressBatchRequestRepository,
                                      PostelBatchRepository postelBatchRepository,
                                      AddressConverter addressConverter,
                                      SqsService sqsService,
                                      NormalizzatoreClient postelClient,
                                      SafeStorageService safeStorageService,
                                      PnAddressManagerConfig pnAddressManagerConfig,
                                      EventService eventService,
                                      CsvService csvService,
                                      AddressUtils addressUtils,
                                      Clock clock) {
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
        this.clock = clock;
        this.batchId = pnAddressManagerConfig.getNormalizer().getPostel().getRequestPrefix() + UUID.randomUUID();
    }

    //    lockAtMostFor specifies how long the lock should be kept in case the executing node dies. You have to set lockAtMostFor to a value which
//    is much longer than normal execution time  If the task takes longer than lockAtMostFor the resulting behavior may be unpredictable
//    (more than one process will effectively hold the lock).
//    lockAtLeastFor specifies minimum amount of time for which the lock should be kept. is to prevent execution from multiple nodes
//    in case of really short tasks and clock difference between the nodes.
//
    @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-request.delay}")
    @SchedulerLock(name = "batchRequest", lockAtMostFor = "${pn.address-manager.normalizer.batch-request.lockAtMostFor}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-request.lockAtLeastFor}")
    protected void pollForNormalizeRequestProcessing() {
        try {
            LockAssert.assertLocked();
            log.info("batch request for new request start on: {}", LocalDateTime.now());
            batchAddressRequest();
        } catch (Exception ex) {
            log.error("Exception in actionPool", ex);
        }
    }

    public void batchAddressRequest() {

        Instant start = clock.instant();
        log.debug(ADDRESS_NORMALIZER_ASYNC + "batchPecRequest batchId: [{}] start from first {}", batchId, start);

        retrieveAndProcessBatchRequest();
        closeOpenedRequest();
        createAndProcessFile(start);
        batchId = pnAddressManagerConfig.getNormalizer().getPostel().getRequestPrefix() + UUID.randomUUID();

        Duration timeSpent = AddressUtils.getTimeSpent(start);
        List<String> batchIdList = fileMap.keySet().stream().toList();
        log.debug(ADDRESS_NORMALIZER_ASYNC + "batchPecRequest - batchId: [{}] query end. Time spent is {} millis", String.join(",", batchIdList), timeSpent.toMillis());

        if (timeSpent.compareTo(Duration.ofMillis(pnAddressManagerConfig.getNormalizer().getBatchRequest().getLockAtMost())) > 0) {
            log.error("Time spent is greater then lockAtMostFor. Multiple nodes could schedule the same actions.");
        }
    }

    private void retrieveAndProcessBatchRequest() {
        Page<BatchRequest> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();

        do {
            int csvCount = 0;
            Instant startPagedQuery = clock.instant();
            page = getBatchRequest(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();

            if (!page.items().isEmpty()) {
                processRequest(page.items(), csvCount);
            } else {
                log.info(ADDRESS_NORMALIZER_ASYNC + "no batch request available");
            }

            Duration timeSpent = AddressUtils.getTimeSpent(startPagedQuery);
            List<String> batchIdList = fileMap.keySet().stream().toList();
            log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId: [{}] end query. Time spent is {} millis", String.join(",", batchIdList), timeSpent.toMillis());
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey) &&
                (pnAddressManagerConfig.getNormalizer().getMaxFileNumber() == 0 || (fileMap.size() + 1) <= pnAddressManagerConfig.getNormalizer().getMaxFileNumber()));

    }

    private void createAndProcessFile(Instant start) {
        if (!CollectionUtils.isEmpty(requestToProcessMap) && !CollectionUtils.isEmpty(fileMap)) {
            fileMap.forEach((key, normalizeRequestPostelInputs) -> execBatchRequest(requestToProcessMap.get(key), key, normalizeRequestPostelInputs, start)
                    .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, CONTEXT_BATCH_ID + key))
                    .block());
            clearMap();
        }
    }

    private void closeOpenedRequest() {
        if(!CollectionUtils.isEmpty(requestToProcess) && !CollectionUtils.isEmpty(listToConvert)){
            List<NormalizeRequestPostelInput> finalListToConvert = new ArrayList<>(listToConvert);
            List<BatchRequest> finalRequestToProcess = new ArrayList<>(requestToProcess);
            fileMap.put(batchId, finalListToConvert);
            requestToProcessMap.put(batchId, finalRequestToProcess);
            clearList();
        }
    }

    private void processRequest(List<BatchRequest> items, int csvCount) {
        for (BatchRequest batchRequest : items) {
            List<NormalizeRequestPostelInput> batchRequestAddresses = addressUtils.normalizeRequestToPostelCsvRequest(batchRequest);
            if (csvCount + batchRequestAddresses.size() <= pnAddressManagerConfig.getNormalizer().getMaxCsvSize()) {
                csvCount = processCsvRawAndIncrementCsvCount(csvCount, batchRequestAddresses, batchRequest);
            } else {
                closeMaps();
                csvCount = openNewRequest(batchRequestAddresses, batchRequest);
                if(csvCount == pnAddressManagerConfig.getNormalizer().getMaxCsvSize()) {
                    break;
                }
            }
        }
    }

    private int openNewRequest(List<NormalizeRequestPostelInput> batchRequestAddresses, BatchRequest batchRequest) {
        String newBatchId =  pnAddressManagerConfig.getNormalizer().getPostel().getRequestPrefix() + UUID.randomUUID();
        if(fileMap.size() < pnAddressManagerConfig.getNormalizer().getMaxFileNumber()) {
            listToConvert.addAll(batchRequestAddresses);
            startProcessingBatchRequest(batchRequest, newBatchId, requestToProcess)
                    .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, CONTEXT_BATCH_ID + newBatchId))
                    .block();
            batchId = newBatchId;
            return batchRequestAddresses.size();
        }
        return pnAddressManagerConfig.getNormalizer().getMaxCsvSize();
    }

    private void closeMaps() {
        List<NormalizeRequestPostelInput> finalListToConvert = new ArrayList<>(listToConvert);
        List<BatchRequest> finalRequestToProcess = new ArrayList<>(requestToProcess);
        fileMap.put(batchId, finalListToConvert);
        requestToProcessMap.put(batchId, finalRequestToProcess);
        clearList();
    }

    private int processCsvRawAndIncrementCsvCount(int csvCount, List<NormalizeRequestPostelInput> batchRequestAddresses, BatchRequest batchRequest) {
            listToConvert.addAll(batchRequestAddresses);
            startProcessingBatchRequest(batchRequest, batchId, requestToProcess)
                    .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, CONTEXT_BATCH_ID + batchId))
                    .block();
            return csvCount + batchRequestAddresses.size();
    }


    private Mono<Void> startProcessingBatchRequest(BatchRequest request, String batchId, List<BatchRequest> requestToProcess) {
        setNewDataOnBatchRequest(request, batchId);
        return addressBatchRequestRepository.setNewBatchIdToBatchRequest(request)
                .doOnError(ConditionalCheckFailedException.class,
                        e -> log.info(ADDRESS_NORMALIZER_ASYNC + "conditional check failed - skip correlationId: {}", request.getCorrelationId(), e))
                .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty())
                .map(requestToProcess::add)
                .then();
    }


    private Page<BatchRequest> getBatchRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        return addressBatchRequestRepository.getBatchRequestByNotBatchId(lastEvaluatedKey, pnAddressManagerConfig.getNormalizer().getBatchRequest().getQueryMaxSize())
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn(ADDRESS_NORMALIZER_ASYNC + "can not get batch request - DynamoDB Mono<Page> is null");
                    return new PostelException(ADDRESS_NORMALIZER_ASYNC + "can not get batch request");
                });
    }

    private Mono<Void> execBatchRequest(List<BatchRequest> items, String key, List<NormalizeRequestPostelInput> listToConvert, Instant start) {

            String csvContent = csvService.writeItemsOnCsvToString(listToConvert);
            String sha256 = addressUtils.computeSha256(csvContent.getBytes(StandardCharsets.UTF_8));

            return safeStorageService.callSelfStorageCreateFileAndUpload(csvContent, sha256)
                    .onErrorResume(e -> {
                        log.error(ADDRESS_NORMALIZER_ASYNC + "failed to create file", e);
                        return incrementAndCheckRetry(items, e, key)
                                .then(Mono.error(e));
                    })
                    .flatMap(t -> activatePostelBatch(items, t, key, sha256, start));
    }

    private Mono<Void> activatePostelBatch(List<BatchRequest> items, FileCreationResponseDto fileCreationResponseDto, String key, String sha256, Instant start) {
        return createPostelBatch(fileCreationResponseDto.getKey(), key, sha256)
                .onErrorResume(v -> incrementAndCheckRetry(items, v, key).then(Mono.error(v)))
                .doOnNext(postelBatch -> {
                    Duration timeSpent = AddressUtils.getTimeSpent(start);
                    log.debug(ADDRESS_NORMALIZER_ASYNC + "PostelBatch with batchId: {} created. Time spent is {} millis", key, timeSpent.toMillis());
                })
                .flatMap(this::callPostelActivationApi);
    }

    private void setNewDataOnBatchRequest(BatchRequest item, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        item.setBatchId(batchId);
        item.setStatus(TAKEN_CHARGE.name());
        item.setLastReserved(now);
    }

    public Mono<Void> callPostelActivationApi(PostelBatch postelBatch) {
        log.logInvokingExternalService(PROCESS_SERVICE_POSTEL_ATTIVAZIONE, postelBatch.getBatchId());
        log.info(ADDRESS_NORMALIZER_ASYNC + "batchId {} - calling postel activation", postelBatch.getBatchId());
        Mono.fromCallable(() -> postelClient.activatePostel(postelBatch))
                .onErrorResume(throwable -> {
                    if (throwable instanceof WebClientResponseException ex && ex.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        log.error(ADDRESS_NORMALIZER_ASYNC + "batchId {} - Error during call postel activation api", postelBatch.getBatchId(), ex);
                        Mono.error(new PnPostelException("Error during call postel activation api", "NOR400", ex));
                    }
                    return Mono.error(throwable);
                })
                .flatMap(activatePostelResponse -> {
                    log.info(ADDRESS_NORMALIZER_ASYNC + "batchId {} - called postel activation", postelBatch.getBatchId());
                    if (!StringUtils.hasText(activatePostelResponse.getError())) {
                        return updatePostelBatchToWorking(postelBatch);
                    }
                    return Mono.error(new PnPostelException("Error during call postel activation api", activatePostelResponse.getError(), null));
                })
                .doOnError(e -> log.error(ADDRESS_NORMALIZER_ASYNC + "batchId {} - failed to execute call to Activation Postel Api", postelBatch.getBatchId(), e))
                .onErrorResume(v -> incrementAndCheckRetry(postelBatch, v).then(Mono.error(v)))
                .subscribe();

        return Mono.empty();
    }

    private Mono<Void> updatePostelBatchToWorking(PostelBatch postelBatch) {
        LocalDateTime now = LocalDateTime.now();
        log.info(ADDRESS_NORMALIZER_ASYNC + "batchId {} - update PostelBatch with status: {}", postelBatch.getBatchId(), BatchStatus.WORKING.getValue());
        postelBatch.setStatus(BatchStatus.WORKING.getValue());
        postelBatch.setWorkingTtl(now.plusSeconds(pnAddressManagerConfig.getNormalizer().getPostel().getWorkingTtl()));
        return postelBatchRepository.update(postelBatch)
                .doOnNext(polling -> log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - updated PostelBatch with status: {}", postelBatch.getBatchId(), BatchStatus.WORKING.getValue()))
                .then();
    }


    private Mono<PostelBatch> createPostelBatch(String fileKey, String key, String sha256) {
        log.info(ADDRESS_NORMALIZER_ASYNC + "batchId {} - creating PostelBatch with fileKey: {}", key, fileKey);
        return postelBatchRepository.create(addressConverter.createPostelBatchByBatchIdAndFileKey(key, fileKey, sha256))
                .flatMap(polling -> {
                    log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - created PostelBatch with fileKey: {}", key, fileKey);
                    return setBatchRequestStatusToWorking(key)
                            .map(batchRequests -> {
                                log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - created PostelBatch with fileKey: {}", key, fileKey);
                                return polling;
                            })
                            .doOnError(e -> log.warn(ADDRESS_NORMALIZER_ASYNC + "batchId {} - failed to create PostelBatch with fileKey: {}", key, fileKey, e));
                });
    }

    private Mono<List<BatchRequest>> setBatchRequestStatusToWorking(String batchId) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(batchId, BatchStatus.TAKEN_CHARGE)
                .doOnNext(requests -> log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - updating {} requests in status {}", batchId, requests.size(), BatchStatus.WORKING))
                .flatMapIterable(requests -> requests)
                .doOnNext(request -> request.setStatus(BatchStatus.WORKING.getValue()))
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(r -> log.debug(ADDRESS_NORMALIZER_ASYNC + "correlationId {} - set status in {}", r.getCorrelationId(), r.getStatus()))
                .doOnError(e -> log.warn(ADDRESS_NORMALIZER_ASYNC + "batchId {} - failed to set request in status {}", batchId, BatchStatus.WORKING, e))
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
                    r.setStatus(TAKEN_CHARGE.getValue());
                    r.setLastReserved(now);
                    if (nextRetry >= pnAddressManagerConfig.getNormalizer().getPostel().getMaxRetry()
                            || (throwable instanceof PnInternalAddressManagerException exception && exception.getStatus() == HttpStatus.BAD_REQUEST.value())
                            || throwable instanceof PnPostelException ex && StringUtils.hasText(ex.getError()) && ex.getError().equals("NOR400")) {
                        r.setStatus(BatchStatus.ERROR.getValue());
                        log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - status in {} (retry: {})", postelBatch.getBatchId(), r.getStatus(), r.getRetry());
                    }
                })
                .flatMap(postelBatchRepository::update)
                .doOnNext(r -> log.debug(ADDRESS_NORMALIZER_ASYNC + "batchId {} - retry incremented", postelBatch.getBatchId()))
                .doOnError(e -> log.warn(ADDRESS_NORMALIZER_ASYNC + "batchId {} - failed to increment retry", postelBatch.getBatchId(), e))
                .filter(r -> BatchStatus.ERROR.getValue().equals(r.getStatus()))
                .flatMap(l -> {
                    log.debug(ADDRESS_NORMALIZER_ASYNC + "there is at least one request in ERROR - call batch to send to SQS");
                    return updateBatchRequest(postelBatch.getBatchId(), WORKING);
                });
    }

    public Mono<Void> updateBatchRequest(String batchId, BatchStatus status) {
        return addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(batchId, status)
                .flatMap(batchRequests -> {
                    batchRequests.forEach(batchRequest -> batchRequest.setStatus(TAKEN_CHARGE.getValue()));
                    return incrementAndCheckRetry(batchRequests, null, batchId).then();
                });
    }

    public Mono<Void> updateBatchRequest(List<BatchRequest> batchRequests, String batchId) {
        return Flux.fromIterable(batchRequests)
                .flatMap(addressBatchRequestRepository::update)
                .doOnNext(request -> log.debug("Normalize Address - correlationId {} - set status in {}", request.getCorrelationId(), request.getStatus()))
                .flatMap(this::sendToEventBridgeOrInDlq)
                .flatMap(addressBatchRequestRepository::update)
                .filter(request -> TAKEN_CHARGE.getValue().equalsIgnoreCase(request.getStatus()))
                .collectList()
                .filter(l -> !l.isEmpty())
                .flatMap(batchRequestList -> incrementAndCheckRetry(batchRequestList, null, batchId));
    }

    private Mono<BatchRequest> sendToEventBridgeOrInDlq(BatchRequest request) {
        LocalDateTime now = LocalDateTime.now();

        return switch (BatchStatus.fromValue(request.getStatus())) {
            case WORKED -> sendEvents(request, request.getClientId())
                    .map(putEventsResult -> {
                        request.setSendStatus(SENT.getValue());
                        request.setTtl(now.plusSeconds(pnAddressManagerConfig.getNormalizer().getBatchRequest().getTtl()).toEpochSecond(ZoneOffset.UTC));
                        return request;
                    })
                    .onErrorResume(throwable -> {
                        request.setSendStatus(NOT_SENT.getValue());
                        return Mono.just(request);
                    })
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
        return eventService.sendEvent(finalMessage)
                .doOnNext(putEventsResult -> {
                    log.info("Event with correlationId {} sent successfully", batchRequest.getCorrelationId());
                    log.debug("Sent event result: {}", putEventsResult.getEntries());
                })
                .doOnError(throwable -> log.error("Send event with correlationId {} failed", batchRequest.getCorrelationId(), throwable));
    }

    private void clearList() {
        requestToProcess.clear();
        listToConvert.clear();
    }

    private void clearMap() {
        requestToProcessMap.clear();
        fileMap.clear();
    }
}
