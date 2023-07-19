package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.service.CsvService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_VERIFY_ADDRESS;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
@lombok.CustomLog
public class AddressUtils {

    private final AddressConverter addressConverter;
    private final Map<String, Object> capMap;
    private final Map<String, String> countryMap;

    public AddressUtils(AddressConverter addressConverter,
                        CsvService csvService) {
        this.addressConverter = addressConverter;
        this.capMap = csvService.capMap();
        this.countryMap = csvService.countryMap();
    }

    public List<NormalizeResult> normalizeAddresses(List<NormalizeRequest> requestItems) {
        return requestItems.stream()
                .map(normalizeRequest -> normalizeAddress(normalizeRequest.getAddress(), normalizeRequest.getId()))
                .map(addressConverter::normalizedAddressResponsetoNormalizeResult)
                .toList();
    }

    public NormalizedAddressResponse normalizeAddress(AnalogAddress analogAddress, String id) {
        NormalizedAddressResponse normalizedAddressResponse = verifyAddress(analogAddress);
        normalizedAddressResponse.setId(id);
        if (StringUtils.isBlank(normalizedAddressResponse.getError())) {
            normalizedAddressResponse.setNormalizedAddress(toUpperCase(analogAddress));
        }
        return normalizedAddressResponse;
    }

    private NormalizedAddressResponse verifyAddress(AnalogAddress analogAddress) {
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        log.logChecking(PROCESS_VERIFY_ADDRESS);
        try {
            verifyAddressInCsv(analogAddress, normalizedAddressResponse);
        } catch (PnAddressManagerException e) {
            log.logCheckingOutcome(PROCESS_VERIFY_ADDRESS, false, e.getDescription());
            normalizedAddressResponse.setError(e.getDescription());
        }
        log.logCheckingOutcome(PROCESS_VERIFY_ADDRESS,true);
        return normalizedAddressResponse;
    }

    private void verifyAddressInCsv(AnalogAddress analogAddress, NormalizedAddressResponse normalizedAddressResponse) {
        if (StringUtils.isBlank(analogAddress.getCountry()) || analogAddress.getCountry().toUpperCase().trim().startsWith("ITAL")){
            normalizedAddressResponse.setItalian(true);
            if(StringUtils.isBlank(analogAddress.getCap())){
                throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_DURING_VERIFY_CSV, ERROR_ADDRESS_MANAGER_DURING_CAP_MANDATORY_DESCRIPTION, HttpStatus.BAD_REQUEST.value(), ERROR_ADDRESS_MANAGER_CAP_NOT_FOUND_ERROR);
            }
            searchCap(analogAddress.getCap());
        } else {
            searchCountry(StringUtils.normalizeSpace(analogAddress.getCountry().toUpperCase()));
        }
    }

    private void searchCap(String cap) {
        if(!capMap.containsKey(cap.trim())) {
            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_DURING_VERIFY_CSV, String.format(ERROR_ADDRESS_MANAGER_DURING_CAP_NOT_FOUND_DESCRIPTION, cap), HttpStatus.BAD_REQUEST.value(), ERROR_ADDRESS_MANAGER_CAP_NOT_FOUND_ERROR);
        }
    }

    private void searchCountry(String country) {
        if (!countryMap.containsKey(country)){
            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_DURING_VERIFY_CSV, String.format(ERROR_ADDRESS_MANAGER_DURING_COUNTRY_NOT_FOUND_DESCRIPTION, country), HttpStatus.BAD_REQUEST.value(), ERROR_ADDRESS_MANAGER_COUNTRY_NOT_FOUND_ERROR);
        }
    }

    public boolean compareAddress(AnalogAddress baseAddress, AnalogAddress targetAddress, boolean isItalian) {
        return compare(baseAddress.getAddressRow(), targetAddress.getAddressRow())
                && compare(baseAddress.getAddressRow2(), targetAddress.getAddressRow2())
                && compare(baseAddress.getCap(), targetAddress.getCap())
                && compare(baseAddress.getCity(), targetAddress.getCity())
                && compare(baseAddress.getCity2(), targetAddress.getCity2())
                && compare(baseAddress.getPr(), targetAddress.getPr())
                && (isItalian || compare(baseAddress.getCountry(), targetAddress.getCountry()));
    }

    private boolean compare(String base, String target) {
        String trimmedBase = StringUtils.normalizeSpace(Optional.ofNullable(base).orElse(""));
        String trimmedTarget = StringUtils.normalizeSpace(Optional.ofNullable(target).orElse(""));
        return trimmedBase.equalsIgnoreCase(trimmedTarget);
    }

    private AnalogAddress toUpperCase(AnalogAddress analogAddress) {
        analogAddress.setAddressRow(Optional.ofNullable(analogAddress.getAddressRow()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCity(Optional.ofNullable(analogAddress.getCity()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCap(Optional.ofNullable(analogAddress.getCap()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setPr(Optional.ofNullable(analogAddress.getPr()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setAddressRow2(Optional.ofNullable(analogAddress.getAddressRow2()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCity2(Optional.ofNullable(analogAddress.getCity2()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCountry(Optional.ofNullable(analogAddress.getCountry()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        return analogAddress;
    }

}
