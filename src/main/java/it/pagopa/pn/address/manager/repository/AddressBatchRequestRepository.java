package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface AddressBatchRequestRepository {

    Mono<PnRequest> update(PnRequest pnRequest);

    Mono<PnRequest> create(PnRequest pnRequest);

    Mono<Page<PnRequest>> getBatchRequestByNotBatchId(Map<String, AttributeValue> lastKey, int limit);

    Mono<List<PnRequest>> getBatchRequestByBatchIdAndStatus(String batchId, BatchStatus status);

    Mono<Page<PnRequest>> getBatchRequestByBatchIdAndStatus(Map<String, AttributeValue> lastKey, String batchId, BatchStatus status);

    Mono<PnRequest> setNewBatchIdToBatchRequest(PnRequest pnRequest);

    Mono<PnRequest> setNewReservationIdToBatchRequest(PnRequest pnRequest);

    Mono<PnRequest> resetBatchRequestForRecovery(PnRequest pnRequest);

    Mono<List<PnRequest>> getBatchRequestToRecovery();

    Mono<Page<PnRequest>> getBatchRequestToSend(Map<String, AttributeValue> lastKey, int limit);
}
