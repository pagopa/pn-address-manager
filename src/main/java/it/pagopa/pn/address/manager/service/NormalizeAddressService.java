package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class NormalizeAddressService {

    private final AddressService addressService;
    private final ApiKeyRepository apiKeyRepository;

    public NormalizeAddressService(AddressService addressService,
                                   ApiKeyRepository apiKeyRepository) {
        this.addressService = addressService;
        this.apiKeyRepository = apiKeyRepository;
    }

    public Mono<AcceptedResponse> normalizeAddressAsync(NormalizeItemsRequest normalizeItemsRequest, String cxId, String xApiKey) {
        return apiKeyRepository.findById(xApiKey)
                .flatMap(apiKeyModel -> addressService.normalizeAddressAsync(normalizeItemsRequest, cxId));

    }

}