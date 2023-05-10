package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchAddress;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.repository.BatchAddressRepository;
import it.pagopa.pn.address.manager.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.service.CsvService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_COUNTRYNOTFOUND;

@Component
@Slf4j
public class AddressUtils {

    private static final String ERROR_DURING_VERIFY_CSV = "Error during verify csv";
    private final long ttl;
    private final Map<String, Object> capMap;
    private final Map<String, String> countryMap;
    private final BatchAddressRepository batchAddressRepository;

    public AddressUtils(@Value("${pn.address.manager.batch.ttl}") long ttl,
                        CsvService csvService,
                        BatchAddressRepository batchAddressRepository) {
        this.ttl = ttl;
        this.capMap = csvService.capMap();
        this.countryMap = csvService.countryMap();
        this.batchAddressRepository = batchAddressRepository;
    }

    public void createBatchAddressByNormalizeItemsRequest(NormalizeItemsRequest normalizeItemsRequest, String cxId){
        normalizeItemsRequest.getRequestItems().forEach(normalizeRequest -> {
            BatchAddress batchAddress = createNewStartBatchAddress();
            setAddressToBatchAddress(normalizeRequest.getAddress(),batchAddress);
            batchAddress.setCorrelationId(normalizeItemsRequest.getCorrelationId());
            batchAddress.setAddressId(normalizeRequest.getId());
            batchAddress.setCxId(cxId);
            batchAddressRepository.create(batchAddress);
        });
    }

    private BatchAddress createNewStartBatchAddress() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BatchAddress batchAddress = new BatchAddress();
        batchAddress.setId(UUID.randomUUID().toString());
        batchAddress.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        batchAddress.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchAddress.setRetry(0);
        batchAddress.setLastReserved(now);
        batchAddress.setCreatedAt(now);
        batchAddress.setTtl(now.plusSeconds(ttl).toEpochSecond(ZoneOffset.UTC));
        log.trace("New Batch Address: {}", batchAddress);
        return batchAddress;
    }

    private void setAddressToBatchAddress(AnalogAddress analogAddress, BatchAddress batchAddress){
        batchAddress.setAddressRow(analogAddress.getAddressRow());
        batchAddress.setAddressRow2(analogAddress.getAddressRow2());
        batchAddress.setCity(analogAddress.getCity());
        batchAddress.setCity2(analogAddress.getCity2());
        batchAddress.setPr(analogAddress.getPr());
        batchAddress.setCap(analogAddress.getCap());
        batchAddress.setCountry(analogAddress.getCountry());
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

    public NormalizedAddressResponse normalizeAddress(AnalogAddress analogAddress, String id) {
        NormalizedAddressResponse normalizedAddressResponse = verifyAddress(analogAddress);
        normalizedAddressResponse.setId(id);
        if (StringUtils.isBlank(normalizedAddressResponse.getError())) {
            normalizedAddressResponse.setNormalizedAddress(toUpperCase(analogAddress));
        }
        return normalizedAddressResponse;
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


    public NormalizedAddressResponse verifyAddress(AnalogAddress analogAddress) {
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        try {
            verifyAddressInCsv(analogAddress, normalizedAddressResponse);
        } catch (PnAddressManagerException e) {
            log.error("Error during verifyAddressInCsv: {}", e.getDescription(), e);
            normalizedAddressResponse.setError(e.getDescription());
        }
        return normalizedAddressResponse;
    }

    private void verifyAddressInCsv(AnalogAddress analogAddress, NormalizedAddressResponse normalizedAddressResponse) {
        if (StringUtils.isBlank(analogAddress.getCountry())
                || analogAddress.getCountry().toUpperCase().trim().startsWith("ITAL")){
            normalizedAddressResponse.setItalian(true);
            searchCap(analogAddress.getCap(), capMap);
        } else {
            searchCountry(analogAddress.getCountry(), countryMap);
        }
    }

    private void searchCountry(String country, Map<String, String> countryMap) {
        String normalizedCountry = StringUtils.normalizeSpace(country.toUpperCase());
        if (!countryMap.containsKey(normalizedCountry)){
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, String.format("Country %s not found", normalizedCountry), HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_COUNTRYNOTFOUND);
        }
    }

    private void searchCap(String cap, Map<String, Object> capMap) {
        if(StringUtils.isBlank(cap)){
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, "Cap is mandatory", HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        }else if(!capMap.containsKey(cap.trim())) {
            throw new PnAddressManagerException(ERROR_DURING_VERIFY_CSV, String.format("Cap %s not found", cap), HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        }
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
