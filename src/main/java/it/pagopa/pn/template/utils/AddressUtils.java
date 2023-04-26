package it.pagopa.pn.template.utils;

import it.pagopa.pn.template.exception.PnAddressManagerException;
import it.pagopa.pn.template.model.NormalizedAddressResponse;
import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.template.rest.v1.dto.NormalizeRequest;
import it.pagopa.pn.template.rest.v1.dto.NormalizeResult;
import it.pagopa.pn.template.service.CsvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
@Slf4j
public class AddressUtils {

    private final boolean flagCsv;
    private final Map<String, Object> capMap;
    private final Map<String, String> countryMap;

    public AddressUtils(
            @Value("${pn.address.manager.flag.csv}") boolean flagCsv, CsvService csvService) {
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

    public NormalizedAddressResponse normalizeAddress(AnalogAddress analogAddress) {
        NormalizedAddressResponse normalizedAddressResponse = verifyAddress(analogAddress);
        normalizedAddressResponse.setNormalizedAddress(toUpperCase(analogAddress));
        return normalizedAddressResponse;
    }

    private AnalogAddress toUpperCase(AnalogAddress analogAddress) {
        analogAddress.setAddressRow(Optional.ofNullable(analogAddress.getAddressRow()).map(String::toUpperCase).orElse(null));
        analogAddress.setCity(Optional.ofNullable(analogAddress.getCity()).map(String::toUpperCase).orElse(null));
        analogAddress.setCap(Optional.ofNullable(analogAddress.getCap()).map(String::toUpperCase).orElse(null));
        analogAddress.setPr(Optional.ofNullable(analogAddress.getPr()).map(String::toUpperCase).orElse(null));
        analogAddress.setAddressRow2(Optional.ofNullable(analogAddress.getAddressRow2()).map(String::toUpperCase).orElse(null));
        analogAddress.setCity2(Optional.ofNullable(analogAddress.getCity2()).map(String::toUpperCase).orElse(null));
        if (analogAddress.getCountry() != null && countryMap.containsKey(analogAddress.getCountry())) {
            analogAddress.setCountry(countryMap.get(analogAddress.getCountry()).toUpperCase());
        }
        return analogAddress;
    }


    public NormalizedAddressResponse verifyAddress(AnalogAddress analogAddress) {
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        if (flagCsv) {
            try {
                verifyAddressInCsv(analogAddress);
            } catch (PnAddressManagerException e) {
                log.error("Error in verifyAddressInCsv", e);
                normalizedAddressResponse.setError(e.getMessage());
            }
        } else {
            //TODO: verify with postel
            normalizedAddressResponse.setError("TODO: verify with postel");
        }
        return normalizedAddressResponse;
    }

    private void verifyAddressInCsv(AnalogAddress analogAddress) {
        if (!StringUtils.hasText(analogAddress.getCountry())
                || analogAddress.getCountry().toUpperCase().trim().startsWith("ITAL")) {
            searchCap(analogAddress.getCap(), capMap);
        } else {
            searchCountry(analogAddress.getCountry(), countryMap);
        }
    }

    private void searchCountry(String country, Map<String, String> countryMap) {
        if (!countryMap.containsKey(country)) {
            throw new PnAddressManagerException("Country not found", HttpStatus.BAD_REQUEST);
        }
    }

    private void searchCap(String cap, Map<String, Object> capMap) {
        if (!capMap.containsKey(cap)) {
            throw new PnAddressManagerException("Cap not found", HttpStatus.BAD_REQUEST);
        }
    }

    public NormalizeItemsResult normalizeAddresses(String correlationId, List<NormalizeRequest> requestItems) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        normalizeItemsResult.setCorrelationId(correlationId);
        List<NormalizeResult> normalizeResultList = new ArrayList<>();
        for (NormalizeRequest n : requestItems) {
            NormalizeResult normalizeResult = new NormalizeResult();
            normalizeResult.setId(n.getId());
            NormalizedAddressResponse normalizedAddressResponse = normalizeAddress(n.getAddress());
            normalizeResult.setError(normalizedAddressResponse.getError());
            normalizeResult.setNormalizedAddress(normalizedAddressResponse.getNormalizedAddress());
            normalizeResultList.add(normalizeResult);
        }
        normalizeItemsResult.setResultItems(normalizeResultList);
        return normalizeItemsResult;
    }
}
