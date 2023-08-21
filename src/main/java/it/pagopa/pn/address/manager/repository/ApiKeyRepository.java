package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import reactor.core.publisher.Mono;

public interface ApiKeyRepository {
    Mono<ApiKeyModel> findById(String id);
}
