package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.rest.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.rest.v1.dto.DeduplicatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeduplicatesAddressService {

    private final AddressUtils addressUtils;

    public DeduplicatesAddressService(AddressUtils addressUtils){
        this.addressUtils = addressUtils;
    }

    public DeduplicatesResponse deduplicates(DeduplicatesRequest request){
        return createDeduplicatesResponseByDeduplicatesRequest(request);
    }

    private DeduplicatesResponse createDeduplicatesResponseByDeduplicatesRequest(DeduplicatesRequest request){
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress()));
        NormalizedAddressResponse normalizeAddressResponse = addressUtils.normalizeAddress(request.getTargetAddress());
        deduplicatesResponse.setError(normalizeAddressResponse.getError());
        deduplicatesResponse.setNormalizedAddress(normalizeAddressResponse.getNormalizedAddress());
        return deduplicatesResponse;
    }

}
