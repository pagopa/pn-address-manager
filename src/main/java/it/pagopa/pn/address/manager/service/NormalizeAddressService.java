package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class NormalizeAddressService {

    private final AddressService addressService;

    public NormalizeAddressService(AddressService addressService) {
        this.addressService = addressService;
    }

    public Mono<AcceptedResponse> normalizeAddressAsync(NormalizeItemsRequest normalizeItemsRequest, String cxId) {
        return addressService.normalizeAddressAsync(normalizeItemsRequest, cxId);
    }

}