package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.CountryModel;
import reactor.core.publisher.Mono;

public interface CountryRepository {
    Mono<CountryModel> findByName(String country);
}
