package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.PostelBatch;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface PostelBatchRepository {

    Mono<PostelBatch> create(PostelBatch postelBatch);

    Mono<PostelBatch> findByFileKey(String fileKey);

    Mono<PostelBatch> update(PostelBatch postelBatch);

    Mono<Page<PostelBatch>> getPostelBatchWithoutReservationIdAndStatusNotWorked(Map<String, AttributeValue> lastKey, int limit);

    Mono<PostelBatch> setNewReservationIdToPostelBatch(PostelBatch postelBatch);

    Mono<PostelBatch> resetPostelBatchForRecovery(PostelBatch postelBatch);

    Mono<List<PostelBatch>> getPostelBatchToRecover();
}
