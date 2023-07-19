package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@lombok.CustomLog
public class DeduplicatesAddressService {

    private final AddressService addressService;

    public DeduplicatesAddressService(AddressService addressService){
        this.addressService = addressService;
    }

    public Mono<DeduplicatesResponse> deduplicates(DeduplicatesRequest request){
        return addressService.normalizeAddress(request);
    }

}
