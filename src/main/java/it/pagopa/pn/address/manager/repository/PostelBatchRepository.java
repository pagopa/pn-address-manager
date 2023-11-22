package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface PostelBatchRepository {

    Mono<NormalizzatoreBatch> create(NormalizzatoreBatch normalizzatoreBatch);

    Mono<NormalizzatoreBatch> findByBatchId(String batchId);

    Mono<NormalizzatoreBatch> update(NormalizzatoreBatch normalizzatoreBatch);

    Mono<NormalizzatoreBatch> resetPostelBatchForRecovery(NormalizzatoreBatch normalizzatoreBatch);

    Mono<List<NormalizzatoreBatch>> getPostelBatchToRecover();

    Mono<Page<NormalizzatoreBatch>> getPostelBatchToClean(Map<String, AttributeValue> lastKey);

    Mono<NormalizzatoreBatch> deleteItem(String batchId);
}
