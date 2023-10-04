package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.client.PagoPaClient;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeduplicatesAddressService {

    private final AddressUtils addressUtils;
    private final PagoPaClient pagoPaClient;
    private final AddressConverter addressConverter;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final ApiKeyRepository apiKeyRepository;

    public DeduplicatesAddressService(AddressUtils addressUtils,
                                      PagoPaClient pagoPaClient,
                                      AddressConverter addressConverter,
                                      PnAddressManagerConfig pnAddressManagerConfig,
                                      ApiKeyRepository apiKeyRepository){
        this.addressUtils = addressUtils;
        this.pagoPaClient = pagoPaClient;
        this.addressConverter = addressConverter;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.apiKeyRepository = apiKeyRepository;
    }

    public Mono<DeduplicatesResponse> deduplicates(DeduplicatesRequest request) {
        if(Boolean.TRUE.equals(pnAddressManagerConfig.getFlagCsv())){
            return Mono.just(createDeduplicatesResponseByDeduplicatesRequest(request));
        }
        return pagoPaClient
                    .deduplicaOnline(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(request))
                    .map(risultatoDeduplica -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, request.getCorrelationId()));
    }

    private DeduplicatesResponse createDeduplicatesResponseByDeduplicatesRequest(DeduplicatesRequest request){
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        NormalizedAddressResponse normalizeAddressResponse = addressUtils.normalizeAddress(request.getTargetAddress(), null);
        deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress(), normalizeAddressResponse.isItalian()));
        deduplicatesResponse.setError(normalizeAddressResponse.getError());
        deduplicatesResponse.setNormalizedAddress(normalizeAddressResponse.getNormalizedAddress());
        return deduplicatesResponse;
    }

    public void checkApiKey(String xApiKey) {
        apiKeyRepository.findById(xApiKey);
    }
}
