package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.PostelBatch;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.List;

public interface PostelBatchRepository {

    Mono<PostelBatch> create(PostelBatch postelBatch);

    Mono<PostelBatch> findByFileKey(String fileKey);

    Mono<PostelBatch> update(PostelBatch postelBatch);

    Mono<PostelBatch> resetPostelBatchForRecovery(PostelBatch postelBatch);

    Mono<List<PostelBatch>> getPostelBatchToRecover();
}
