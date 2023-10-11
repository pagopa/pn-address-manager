package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.CapModel;
import reactor.core.publisher.Mono;

public interface CapRepository {

    Mono<CapModel> findValidCap(String cap);
}
