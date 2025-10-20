package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.DeduplicatesError;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.entity.CountryModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.repository.CapRepository;
import it.pagopa.pn.address.manager.repository.CountryRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.PNADDR002_MESSAGE;

@Component
@CustomLog
@RequiredArgsConstructor
public class CapAndCountryService {

    private final CapRepository capRepository;
    private final CountryRepository countryRepository;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public Mono<DeduplicatesResponse> verifyCapAndCountry(DeduplicatesResponse item) {
        if (Boolean.TRUE.equals(pnAddressManagerConfig.getEnableWhitelisting()) && Objects.nonNull(item.getNormalizedAddress())) {
            return verifyCapAndCountry(item.getNormalizedAddress())
                    .onErrorResume(throwable -> {
                        log.warn("Error during verify CAP or country deduplicate: correlationId: [{}] - error: {}", item.getCorrelationId(), throwable.getMessage());
                        item.setError(DeduplicatesError.PNADDR002.name());
                        item.setNormalizedAddress(null);
                        return Mono.empty();
                    })
                    .thenReturn(item);
        }
        return Mono.just(item);
    }

    public Mono<NormalizeResult> verifyCapAndCountry(NormalizeResult item) {
        if (Boolean.TRUE.equals(pnAddressManagerConfig.getEnableWhitelisting()) && Objects.nonNull(item.getNormalizedAddress())) {
                return verifyCapAndCountry(item.getNormalizedAddress())
                        .onErrorResume(throwable -> {
                            log.warn("Error during verify cap or country: {}", throwable.getMessage());
                            item.setError(PNADDR002_MESSAGE);
                            item.setNormalizedAddress(null);
                            return Mono.empty();
                        })
                        .thenReturn(item);
        }
        return Mono.just(item);
    }

    Mono<AnalogAddress> verifyCapAndCountry(AnalogAddress analogAddress){
        if ((StringUtils.isBlank(analogAddress.getCountry()) || analogAddress.getCountry().toUpperCase().trim().startsWith("ITA"))) {
            return verifyCap(analogAddress.getCap()).thenReturn(analogAddress);
        } else {
            return verifyCountry(analogAddress.getCountry()).thenReturn(analogAddress);
        }
    }

    private Mono<CountryModel> verifyCountry(String country) {
        log.logChecking("checking country");
        return countryRepository.findByName(country)
                .flatMap(this::checkValidity);
    }

    private Mono<CapModel> verifyCap(String cap) {
        log.logChecking("checking cap");
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
