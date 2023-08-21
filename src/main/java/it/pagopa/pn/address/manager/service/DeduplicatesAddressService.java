package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@lombok.CustomLog
public class DeduplicatesAddressService {

    private final AddressService addressService;
    private final ApiKeyRepository apiKeyRepository;

    public DeduplicatesAddressService(AddressService addressService,
                                      ApiKeyRepository apiKeyRepository){
        this.addressService = addressService;
        this.apiKeyRepository = apiKeyRepository;
    }

    public Mono<DeduplicatesResponse> deduplicates(DeduplicatesRequest request, String xApiKey){
        return apiKeyRepository.findById(xApiKey).
                flatMap(apiKeyModel -> addressService.normalizeAddress(request));
    }

}
