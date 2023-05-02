package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.service.CsvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_COUNTRYNOTFOUND;

@Component
@Slf4j
public class AddressUtils {

    private final boolean flagCsv;
    private final Map<String, Object> capMap;
    private final Map<String, String> countryMap;

    public AddressUtils(@Value("${pn.address.manager.flag.csv}") boolean flagCsv,
                        CsvService csvService) {
        this.flagCsv = flagCsv;
        this.capMap = csvService.capMap();
        this.countryMap = csvService.countryMap();
    }

    public boolean compareAddress(AnalogAddress baseAddress, AnalogAddress targetAddress) {
        return compare(baseAddress.getAddressRow(), targetAddress.getAddressRow())
                && compare(baseAddress.getAddressRow2(), targetAddress.getAddressRow2())
                && compare(baseAddress.getCap(), targetAddress.getCap())
                && compare(baseAddress.getCity(), targetAddress.getCity())
                && compare(baseAddress.getCity2(), targetAddress.getCity2())
                && compare(baseAddress.getPr(), targetAddress.getPr())
                && compare(baseAddress.getCountry(), targetAddress.getCountry());
    }

    private boolean compare(String base, String target) {
        String trimmedBase = Optional.ofNullable(base).orElse("").trim();
        String trimmedTarget = Optional.ofNullable(target).orElse("").trim();
        return trimmedBase.equalsIgnoreCase(trimmedTarget);
    }

    public NormalizedAddressResponse normalizeAddress(AnalogAddress analogAddress, String id) {
        NormalizedAddressResponse normalizedAddressResponse = verifyAddress(analogAddress);
        normalizedAddressResponse.setId(id);
        if (!StringUtils.hasText(normalizedAddressResponse.getError())) {
            normalizedAddressResponse.setNormalizedAddress(toUpperCase(analogAddress));
        }
        return normalizedAddressResponse;
    }

    private AnalogAddress toUpperCase(AnalogAddress analogAddress) {
        analogAddress.setAddressRow(Optional.ofNullable(analogAddress.getAddressRow()).map(s -> s.trim().toUpperCase()).orElse(null));
        analogAddress.setCity(Optional.ofNullable(analogAddress.getCity()).map(s -> s.trim().toUpperCase()).orElse(null));
        analogAddress.setCap(Optional.ofNullable(analogAddress.getCap()).map(s -> s.trim().toUpperCase()).orElse(null));
        analogAddress.setPr(Optional.ofNullable(analogAddress.getPr()).map(s -> s.trim().toUpperCase()).orElse(null));
        analogAddress.setAddressRow2(Optional.ofNullable(analogAddress.getAddressRow2()).map(s -> s.trim().toUpperCase()).orElse(null));
        analogAddress.setCity2(Optional.ofNullable(analogAddress.getCity2()).map(s -> s.trim().toUpperCase()).orElse(null));
        analogAddress.setCountry(Optional.ofNullable(analogAddress.getCountry()).map(s -> s.trim().toUpperCase()).orElse(null));
        return analogAddress;
    }


    public NormalizedAddressResponse verifyAddress(AnalogAddress analogAddress) {
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        if (flagCsv) {
            try {
                verifyAddressInCsv(analogAddress);
            } catch (PnAddressManagerException e) {
                log.error("Error during verifyAddressInCsv: {}", e.getDescription(), e);
                normalizedAddressResponse.setError(e.getDescription());
            }
        } else {
            //TODO: verify with postel
            normalizedAddressResponse.setError("TODO: verify with postel");
        }
        return normalizedAddressResponse;
    }

    private void verifyAddressInCsv(AnalogAddress analogAddress) {
        if (!StringUtils.hasText(analogAddress.getCountry())
                || analogAddress.getCountry().toUpperCase().trim().startsWith("ITAL")){
            searchCap(analogAddress.getCap(), capMap);
        } else {
            searchCountry(analogAddress.getCountry(), countryMap);
        }
    }

    private void searchCountry(String country, Map<String, String> countryMap) {
        if (!countryMap.containsKey(country.trim().toUpperCase())) {
            throw new PnAddressManagerException("Error during verify CSV", String.format("Country %s not found", country), HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_COUNTRYNOTFOUND);
        }
    }

    private void searchCap(String cap, Map<String, Object> capMap) {
        if (StringUtils.hasText(cap) && !capMap.containsKey(cap)) {
            throw new PnAddressManagerException("Error during verify CSV", String.format("Cap %s not found", cap), HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        }
        //TODO: cap null?
    }

    public List<NormalizeResult> normalizeAddresses(List<NormalizeRequest> requestItems) {
        return requestItems.stream()
                .map(normalizeRequest -> normalizeAddress(normalizeRequest.getAddress(), normalizeRequest.getId()))
                .map(this::toNormalizeResult)
                .toList();
    }

    private NormalizeResult toNormalizeResult(NormalizedAddressResponse response) {
        NormalizeResult normalizeResult = new NormalizeResult();
        normalizeResult.setId(response.getId());
        normalizeResult.setError(response.getError());
        normalizeResult.setNormalizedAddress(response.getNormalizedAddress());
        return normalizeResult;
    }
}
