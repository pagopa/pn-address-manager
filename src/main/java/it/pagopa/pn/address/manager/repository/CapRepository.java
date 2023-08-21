package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.CAPModel;
import reactor.core.publisher.Mono;

public interface CapRepository {
    Mono<CAPModel> findByCap(String cap);
}
