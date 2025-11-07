package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.middleware.client.DeduplicaClient;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.ADDRESS_NORMALIZER_SYNC;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_CHECKING_APIKEY;

@Service
@CustomLog
@RequiredArgsConstructor
public class DeduplicatesAddressService {

    private final AddressUtils addressUtils;
    private final DeduplicaClient postelClient;
    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final ApiKeyUtils apiKeyUtils;
    private final CapAndCountryService capAndCountryService;
    private final AddressConverter addressConverter;

    public Mono<DeduplicatesResponse> deduplicates(DeduplicatesRequest request, String pnAddressManagerCxId, String xApiKey) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, request.getCorrelationId());
        Mono<DeduplicatesResponse> deduplicatesResponseMono = apiKeyUtils.checkApiKey(pnAddressManagerCxId, xApiKey)
                .flatMap(apiKeyModel -> {
                    log.logCheckingOutcome(PROCESS_CHECKING_APIKEY, true);
                    log.info(ADDRESS_NORMALIZER_SYNC + "Founded apikey for request: [{}]", request.getCorrelationId());
                    if(Boolean.TRUE.equals(pnAddressManagerConfig.getFlagCsv())){
                        return Mono.just(createDeduplicatesResponseByDeduplicatesRequest(request));
                    }
                    return postelClient
                            .deduplica(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(request))
                            .map(risultatoDeduplica -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, request.getCorrelationId()))
                            .map(addressUtils::verifyRequiredFields)
                            .flatMap(capAndCountryService::verifyCapAndCountry)
                            .doOnError(Mono::error);
                });
        return MDCUtils.addMDCToContextAndExecute(deduplicatesResponseMono);
    }

    private DeduplicatesResponse createDeduplicatesResponseByDeduplicatesRequest(DeduplicatesRequest request) {
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        NormalizedAddressResponse normalizeAddressResponse = addressUtils.normalizeAddress(request.getTargetAddress(), request.getCorrelationId(), request.getCorrelationId());
        deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress(), normalizeAddressResponse.isItalian()));
        deduplicatesResponse.setError(normalizeAddressResponse.getError());
        deduplicatesResponse.setNormalizedAddress(normalizeAddressResponse.getNormalizedAddress());
        return deduplicatesResponse;
    }
}
