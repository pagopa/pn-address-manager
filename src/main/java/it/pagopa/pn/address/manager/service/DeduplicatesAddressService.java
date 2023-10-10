package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.middleware.client.PostelClient;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.APIKEY_DOES_NOT_EXISTS;

@Service
@Slf4j
public class DeduplicatesAddressService {

    private final AddressUtils addressUtils;
    private final PostelClient postelClient;
    private final AddressConverter addressConverter;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final ApiKeyRepository apiKeyRepository;
    private final CapAndCountryService capAndCountryService;

    public DeduplicatesAddressService(AddressUtils addressUtils,
                                      PostelClient postelClient,
                                      AddressConverter addressConverter,
                                      PnAddressManagerConfig pnAddressManagerConfig,
                                      ApiKeyRepository apiKeyRepository, CapAndCountryService capAndCountryService) {
        this.addressUtils = addressUtils;
        this.postelClient = postelClient;
        this.addressConverter = addressConverter;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.apiKeyRepository = apiKeyRepository;
        this.capAndCountryService = capAndCountryService;
    }

    public Mono<DeduplicatesResponse> deduplicates(DeduplicatesRequest request, String pnAddressManagerCxId, String xApiKey) {
        return checkApiKey(pnAddressManagerCxId, xApiKey)
                .flatMap(apiKeyModel -> {
                    if(Boolean.TRUE.equals(pnAddressManagerConfig.getFlagCsv())){
                        return Mono.just(createDeduplicatesResponseByDeduplicatesRequest(request));
                    }
                    return postelClient
                            .deduplica(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(request), pnAddressManagerCxId, xApiKey)
                            .map(risultatoDeduplica -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, request.getCorrelationId()))
                            .flatMap(capAndCountryService::verifyCapAndCountry)
                            .doOnError(Mono::error);
               });
    }

    private DeduplicatesResponse createDeduplicatesResponseByDeduplicatesRequest(DeduplicatesRequest request) {
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        NormalizedAddressResponse normalizeAddressResponse = addressUtils.normalizeAddress(request.getTargetAddress(), request.getCorrelationId());
        deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress(), normalizeAddressResponse.isItalian()));
        deduplicatesResponse.setError(normalizeAddressResponse.getError());
        deduplicatesResponse.setNormalizedAddress(normalizeAddressResponse.getNormalizedAddress());
        return deduplicatesResponse;
    }

    public Mono<ApiKeyModel> checkApiKey(String cxId, String xApiKey) {
        return apiKeyRepository.findById(cxId)
                .filter(apiKeyModel -> apiKeyModel.getApiKey().equalsIgnoreCase(xApiKey))
                .switchIfEmpty(Mono.error(new PnAddressManagerException(APIKEY_DOES_NOT_EXISTS, APIKEY_DOES_NOT_EXISTS, HttpStatus.FORBIDDEN.value(), "Api Key not found")));

    }
}
