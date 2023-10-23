package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.ForeignValidationMode;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
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

    private static final String ERROR_DURING_VERIFY_CSV = "Error during verify csv";
    private final List<CapModel> capList;
    private final Map<String, String> countryMap;

    private final PnAddressManagerConfig pnAddressManagerConfig;

    public AddressUtils(CsvService csvService, PnAddressManagerConfig pnAddressManagerConfig) {
        this.capList = csvService.capList();
        this.countryMap = csvService.countryMap();
        this.pnAddressManagerConfig = pnAddressManagerConfig;
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

    public NormalizedAddressResponse normalizeAddress(AnalogAddress analogAddress, String id, String correlationId) {
        NormalizedAddressResponse normalizedAddressResponse = verifyAddress(analogAddress, correlationId);
        normalizedAddressResponse.setId(id);
        if (StringUtils.isBlank(normalizedAddressResponse.getError())) {
            normalizedAddressResponse.setNormalizedAddress(toUpperCase(analogAddress));
        }
        return normalizedAddressResponse;
    }

    private boolean validateAddress(AnalogAddress analogAddress, String pattern) {
        return validateAddressField(analogAddress.getAddressRow(), pattern)
                && validateAddressField(analogAddress.getAddressRow2(), pattern)
                && validateAddressField(analogAddress.getCity(), pattern)
                && validateAddressField(analogAddress.getCity2(), pattern)
                && validateAddressField(analogAddress.getCountry(), pattern)
                && validateAddressField(analogAddress.getPr(), pattern)
                && validateAddressField(analogAddress.getCap(), pattern);
    }

    private boolean validateAddressField(String fieldValue, String pattern) {
        if (!StringUtils.isBlank(fieldValue)) {
            return fieldValue.matches("[" + pattern + "]*");
        }
        return true;
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


    private NormalizedAddressResponse verifyAddress(AnalogAddress analogAddress, String correlationId) {
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        log.logChecking(PROCESS_VERIFY_ADDRESS);
        if (Boolean.TRUE.equals(pnAddressManagerConfig.getFlagCsv())) {
            try {
                verifyAddressInCsv(analogAddress, normalizedAddressResponse);
            } catch (PnAddressManagerException e) {
                log.logCheckingOutcome(PROCESS_VERIFY_ADDRESS, false, e.getDescription());
                log.error("Error during verifyAddressInCsv for {}: {}", correlationId, e.getDescription(), e);
                normalizedAddressResponse.setError(e.getDescription());
            }
        } else {
            //TODO: verify with postel
            normalizedAddressResponse.setError("TODO: verify with postel");
        }
        log.logCheckingOutcome(PROCESS_VERIFY_ADDRESS, true);
        return normalizedAddressResponse;
    }

    private void verifyAddressInCsv(AnalogAddress analogAddress, NormalizedAddressResponse normalizedAddressResponse) {
        if (StringUtils.isBlank(analogAddress.getCountry())
                || analogAddress.getCountry().toUpperCase().trim().startsWith("ITAL")) {
            normalizedAddressResponse.setItalian(true);
            verifyCapAndCity(analogAddress);
        } else {
            searchCountry(analogAddress, countryMap);
        }
    }

    private void verifyCapAndCity(AnalogAddress analogAddress) {
        if (StringUtils.isBlank(analogAddress.getCap())
                || StringUtils.isBlank(analogAddress.getCity())
                || StringUtils.isBlank(analogAddress.getPr())) {
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, "Cap, city and Province are mandatory", HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        } else if (!compareWithCapModelObject(analogAddress)) {
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, "Invalid Address, Cap, City and Province: [" + analogAddress.getCap() + "," + analogAddress.getCity() + "," + analogAddress.getPr() + "]", HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        } else if (Boolean.TRUE.equals(pnAddressManagerConfig.getEnableValidation())
                && !validateAddress(analogAddress, pnAddressManagerConfig.getValidationPattern())) {
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, "Address contains invalid characters", HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        }
    }

    private boolean compareWithCapModelObject(AnalogAddress analogAddress) {
        return capList.stream()
                .anyMatch(capModel -> capModel.getCap().equalsIgnoreCase(analogAddress.getCap().trim())
                        && capModel.getProvince().equalsIgnoreCase(analogAddress.getPr().trim())
                        && capModel.getCity().equalsIgnoreCase(analogAddress.getCity().trim()));
    }

    private void searchCountry(AnalogAddress analogAddress, Map<String, String> countryMap) {
        String normalizedCountry = StringUtils.normalizeSpace(analogAddress.getCountry().toUpperCase());
        if (!countryMap.containsKey(normalizedCountry)) {
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, String.format("Country not found: [%s]", normalizedCountry), HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_COUNTRYNOTFOUND);
        } else if (Boolean.TRUE.equals(pnAddressManagerConfig.getEnableValidation()) && !validateAddressWithMode(analogAddress)) {
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, "Address contains invalid characters", HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        }
    }

    private boolean validateAddressWithMode(AnalogAddress analogAddress) {
        if (ForeignValidationMode.STANDARD == pnAddressManagerConfig.getForeignValidationMode()) {
            return validateAddress(analogAddress, pnAddressManagerConfig.getValidationPattern());
        } else if (ForeignValidationMode.PATTERN == pnAddressManagerConfig.getForeignValidationMode()) {
            return validateAddress(analogAddress, pnAddressManagerConfig.getForeignValidationPattern());
        }
        return true;
    }

    public List<NormalizeResult> normalizeAddresses(NormalizeItemsRequest normalizeItemsRequest) {
        List<NormalizeRequest> requestItems = normalizeItemsRequest.getRequestItems();
        return requestItems.stream()
                .map(normalizeRequest -> normalizeAddress(normalizeRequest.getAddress(), normalizeRequest.getId(), normalizeItemsRequest.getCorrelationId()))
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
