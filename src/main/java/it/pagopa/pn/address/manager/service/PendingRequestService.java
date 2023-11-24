package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.ADDRESS_NORMALIZER_ASYNC;
import static it.pagopa.pn.address.manager.constant.BatchStatus.TAKEN_CHARGE;
import static it.pagopa.pn.address.manager.constant.BatchStatus.WORKING;

@Service
@CustomLog
@RequiredArgsConstructor
public class PendingRequestService {

    private final AddressBatchRequestRepository pnRequestRepository;
    private final PnRequestService addressBatchRequestService;
    private final PostelBatchRepository normalizzatoreBatchRepository;
    private final Clock clock;


    /**
     * The cleanStoppedRequest function is a scheduled function that checks for any PostelBatch objects in the DynamoDB table
     * with a status of WORKING and workingTtl expires.
     * If it finds any, it will then retrieve any related batchRequest, reset them and call incrementAndCheckRetry for these batch requests.
     * Finally, delete any expired postelBatch
     */
  @Scheduled(fixedDelayString = "${pn.address-manager.normalizer.batch-clean-request}")
    @SchedulerLock(name = "cleanStoppedRequest", lockAtMostFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-most}",
            lockAtLeastFor = "${pn.address-manager.normalizer.batch-recovery.lock-at-least}")
    public void cleanPendingPostelBatch() {
        log.trace(ADDRESS_NORMALIZER_ASYNC + "clean pending postel batch start");
        processPostelbatchToClean(getPostelBatchToClean(Map.of()))
                .doOnNext(batchRequestPage -> log.trace(ADDRESS_NORMALIZER_ASYNC + "recovery postel activation end"))
                .subscribe();
    }


    private Mono<Page<NormalizzatoreBatch>> processPostelbatchToClean(Mono<Page<NormalizzatoreBatch>> pageMono) {
        return pageMono.flatMap(page -> {
            if (page.items().isEmpty()) {
                log.info(ADDRESS_NORMALIZER_ASYNC + "no postelBatch to clean founded");
                return Mono.just(page);
            }

            Instant startPagedQuery = clock.instant();
            return retrieveAndProcessRelatedBatchRequest(page.items())
                    .thenReturn(page)
                    .doOnTerminate(() -> {
                        Duration timeSpent = AddressUtils.getTimeSpent(startPagedQuery);
                        log.debug(ADDRESS_NORMALIZER_ASYNC + "end retrieve and process related batchRequest. Time spent is {} millis", timeSpent.toMillis());
                    })
                    .flatMap(lastProcessedPage -> {
                        if (lastProcessedPage.lastEvaluatedKey() != null) {
                            return processPostelbatchToClean(getPostelBatchToClean(lastProcessedPage.lastEvaluatedKey()));
                        } else {
                            return Mono.just(lastProcessedPage);
                        }
                    });
        });

    }

    private Mono<Void> retrieveAndProcessRelatedBatchRequest(List<NormalizzatoreBatch> items) {
        return Flux.fromIterable(items)
                .flatMap(postelBatch -> processRelatedBatchRequest(getRelatedBatchRequest(Map.of(), postelBatch.getBatchId()), postelBatch.getBatchId()))
                .doOnNext(batchRequestPage -> log.trace(ADDRESS_NORMALIZER_ASYNC + "clean pending postel batch end"))
                .then();

    }

    private Mono<Page<PnRequest>> processRelatedBatchRequest(Mono<Page<PnRequest>> pageMono, String batchId) {
        return pageMono.flatMap(page -> {
            if (page.items().isEmpty()) {
                log.info(ADDRESS_NORMALIZER_ASYNC + "no postelBatch to recovery founded");
                return Mono.just(page);
            }

            Instant startPagedQuery = clock.instant();
            return processRelatedRequest(page.items(), batchId)
                    .thenReturn(page)
                    .doOnTerminate(() -> {
                        Duration timeSpent = AddressUtils.getTimeSpent(startPagedQuery);
                        log.debug(ADDRESS_NORMALIZER_ASYNC + "end recovery batchRequest query. Time spent is {} millis", timeSpent.toMillis());
                    })
                    .flatMap(pnRequestPage ->  normalizzatoreBatchRepository.deleteItem(batchId)
                            .thenReturn(pnRequestPage))
                    .flatMap(lastProcessedPage -> {
                        if (lastProcessedPage.lastEvaluatedKey() != null) {
                            return processRelatedBatchRequest(getRelatedBatchRequest(lastProcessedPage.lastEvaluatedKey(), batchId), batchId);
                        } else {
                            return Mono.just(lastProcessedPage);
                        }
                    });
        });
    }

    private Mono<Void> processRelatedRequest(List<PnRequest> items, String batchId) {
        return Flux.fromIterable(items)
                .flatMap(batchRequest -> {
                    batchRequest.setStatus(TAKEN_CHARGE.getValue());
                    return pnRequestRepository.update(batchRequest);
                })
                .flatMap(batchRequest -> addressBatchRequestService.incrementAndCheckRetry(items, null, batchId))
                .count()
                .doOnNext(count -> log.debug(ADDRESS_NORMALIZER_ASYNC + " increment retry for {} request", count))
                .then();
    }

    private Mono<Page<PnRequest>> getRelatedBatchRequest(Map<String, AttributeValue> lastEvaluatedKey, String batchId) {
        return pnRequestRepository.getBatchRequestByBatchIdAndStatus(lastEvaluatedKey, batchId, WORKING);
    }

    private Mono<Page<NormalizzatoreBatch>> getPostelBatchToClean(Map<String, AttributeValue> lastEvaluatedKey) {
        return normalizzatoreBatchRepository.getPostelBatchToClean(lastEvaluatedKey);
    }
}
