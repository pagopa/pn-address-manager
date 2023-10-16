package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.entity.CountryModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.repository.CapRepository;
import it.pagopa.pn.address.manager.repository.CountryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j

public class CapAndCountryService {

    private final CapRepository capRepository;
    private final CountryRepository countryRepository;


    public CapAndCountryService(CapRepository capRepository, CountryRepository countryRepository) {
        this.capRepository = capRepository;
        this.countryRepository = countryRepository;
    }

    public Mono<DeduplicatesResponse> verifyCapAndCountry(DeduplicatesResponse item) {
        if (item.getNormalizedAddress() != null) {
            if (org.apache.commons.lang3.StringUtils.isBlank(item.getNormalizedAddress().getCountry())
                    || item.getNormalizedAddress().getCountry().toUpperCase().trim().startsWith("ITAL")) {
                return verifyCap(item.getNormalizedAddress().getCap())
                        .onErrorResume(throwable -> {
                            log.error("Verify cap in whitelist result: {}", throwable.getMessage());
                            item.setError(throwable.getMessage());
                            item.setNormalizedAddress(null);
                            return Mono.empty();
                        })
                        .thenReturn(item);
            } else {
                return verifyCountry(item.getNormalizedAddress().getCountry())
                        .onErrorResume(throwable -> {
                            log.error("Verify country in whitelist result: {}", throwable.getMessage());
                            item.setError(throwable.getMessage());
                            item.setNormalizedAddress(null);
                            return Mono.empty();
                        })
                        .thenReturn(item);
            }
        } else {
            return Mono.just(item);
        }
    }

    public Mono<NormalizeResult> verifyCapAndCountryList(NormalizeResult item) {
        if (!StringUtils.hasText(item.getNormalizedAddress().getCountry())
                || item.getNormalizedAddress().getCountry().toUpperCase().trim().startsWith("ITAL")) {
            return verifyCap(item.getNormalizedAddress().getCap())
                    .onErrorResume(throwable -> {
                        log.error("Verify cap in whitelist result: {}", throwable.getMessage());
                        item.setError(throwable.getMessage());
                        item.setNormalizedAddress(null);
                        return Mono.empty();
                    }).thenReturn(item);
        } else {
            return verifyCountry(item.getNormalizedAddress().getCountry())
                    .onErrorResume(throwable -> {
                        log.error("Verify country in whitelist result: {}", throwable.getMessage());
                        item.setError(throwable.getMessage());
                        item.setNormalizedAddress(null);
                        return Mono.empty();
                    }).thenReturn(item);
        }
    }

    private Mono<CountryModel> verifyCountry(String country) {
        return countryRepository.findByName(country)
                .flatMap(this::checkValidity);
    }

    private Mono<CapModel> verifyCap(String cap) {
        return capRepository.findValidCap(cap)
                .flatMap(this::checkValidity);
    }

    private Mono<CapModel> checkValidity(CapModel capModel) {
        LocalDateTime now = LocalDateTime.now();
        if (capModel.getStartValidity() != null && capModel.getStartValidity().isAfter(now)) {
            return Mono.error(new Throwable(String.format("Cap is present in whitelist but start validity date is %s", capModel.getStartValidity())));
        } else if (capModel.getEndValidity() != null && capModel.getEndValidity().isBefore(now)) {
            return Mono.error(new Throwable(String.format("Cap is present in whitelist but end validity date is %s", capModel.getEndValidity())));
        }
        return Mono.just(capModel);
    }

    private Mono<CountryModel> checkValidity(CountryModel countryModel) {
        LocalDateTime now = LocalDateTime.now();
        if (countryModel.getStartValidity() != null && countryModel.getStartValidity().isAfter(now)) {
            return Mono.error(new Throwable(String.format("Country is present in whitelist but start validity date is %s", countryModel.getStartValidity())));
        } else if (countryModel.getEndValidity() != null && countryModel.getEndValidity().isBefore(now)) {
            return Mono.error(new Throwable(String.format("Country is present in whitelist but end validity date is %s", countryModel.getEndValidity())));
        }
        return Mono.just(countryModel);
    }
}
