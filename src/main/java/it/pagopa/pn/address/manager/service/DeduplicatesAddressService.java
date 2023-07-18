package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.springframework.stereotype.Service;

@Service
@lombok.CustomLog
public class DeduplicatesAddressService {

    private final AddressUtils addressUtils;

    public DeduplicatesAddressService(AddressUtils addressUtils){
        this.addressUtils = addressUtils;
    }

    public DeduplicatesResponse deduplicates(DeduplicatesRequest request){
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        NormalizedAddressResponse normalizeAddressResponse = addressUtils.normalizeAddress(request.getTargetAddress(), null);
        deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress(), normalizeAddressResponse.isItalian()));
        deduplicatesResponse.setError(normalizeAddressResponse.getError());
        deduplicatesResponse.setNormalizedAddress(normalizeAddressResponse.getNormalizedAddress());
        return deduplicatesResponse;
    }

}
