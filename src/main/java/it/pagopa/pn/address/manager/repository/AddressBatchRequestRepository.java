package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface AddressBatchRequestRepository {

    Mono<BatchRequest> update(BatchRequest batchRequest);

    Mono<BatchRequest> create(BatchRequest batchRequest);

    Mono<Page<BatchRequest>> getBatchRequestByNotBatchId(Map<String, AttributeValue> lastKey, int limit);

    Mono<Page<BatchRequest>> getBatchRequestByBatchId(Map<String, AttributeValue> lastKey, String batchId);

    Mono<List<BatchRequest>> getBatchRequestByBatchIdAndStatus(String batchId, BatchStatus status);

    Mono<BatchRequest> setNewBatchIdToBatchRequest(BatchRequest batchRequest);

    Mono<BatchRequest> setNewReservationIdToBatchRequest(BatchRequest batchRequest);

    Mono<BatchRequest> resetBatchRequestForRecovery(BatchRequest batchRequest);

    Mono<List<BatchRequest>> getBatchRequestToRecovery();

    Mono<Page<BatchRequest>> getBatchRequestToSend(Map<String, AttributeValue> lastKey, int limit);
}
