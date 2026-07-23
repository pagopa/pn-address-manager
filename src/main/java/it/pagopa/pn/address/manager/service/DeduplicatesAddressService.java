package it.pagopa.pn.address.manager.service;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.AddressOut;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.DeduplicatesError;
import it.pagopa.pn.address.manager.constant.DeduplicatesResultDetails;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.middleware.client.DeduplicaClient;
import it.pagopa.pn.address.manager.middleware.queue.producer.SqsSender;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
    private final SqsSender sqsSender;

    public Mono<DeduplicatesResponse> deduplicates(DeduplicatesRequest request, String pnAddressManagerCxId, String xApiKey) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, request.getCorrelationId());
        Mono<DeduplicatesResponse> deduplicatesResponseMono = apiKeyUtils.checkApiKey(pnAddressManagerCxId, xApiKey)
                .doOnNext(apiKeyModel -> {
                    log.logCheckingOutcome(PROCESS_CHECKING_APIKEY, true);
                    log.info(ADDRESS_NORMALIZER_SYNC + "Founded apikey for request: [{}]", request.getCorrelationId());
                })
                .map(unused -> pnAddressManagerConfig.getFlagCsv())
                .flatMap(aBoolean -> {
                    if (Boolean.TRUE.equals(aBoolean)) {
                        log.info(ADDRESS_NORMALIZER_SYNC + "Flag CSV is enabled, skipping Postel call for request: [{}]", request.getCorrelationId());
                        return Mono.just(createDeduplicatesResponseByDeduplicatesRequest(request));
                    } else {
                        log.info(ADDRESS_NORMALIZER_SYNC + "Flag CSV is disabled, calling Postel for request: [{}]", request.getCorrelationId());
                        return callPostel(request);
                    }
                });

        return MDCUtils.addMDCToContextAndExecute(deduplicatesResponseMono);
    }

    private Mono<DeduplicatesResponse> callPostel(DeduplicatesRequest request) {
        if (areRequiredFieldsMissing(request)) {
            log.warn("{} Required fields are missing for request: [{}]", ADDRESS_NORMALIZER_SYNC, request.getCorrelationId());
            return Mono.just(getDeduplicatesResponse(request));
        }
        return Mono.just(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(request))
                .flatMap(deduplicaRequest -> Mono.fromRunnable(() -> sqsSender.pushDeduplicaRequestEvent(deduplicaRequest))
                        .thenReturn(deduplicaRequest))
                .flatMap(postelClient::deduplica)
                .flatMap(deduplicaResponse -> Mono.fromRunnable(() -> sqsSender.pushDeduplicaResponseEvent(deduplicaResponse))
                        .thenReturn(deduplicaResponse))
                .flatMap(deduplicaResponse -> Mono.just(addressConverter.createDeduplicatesResponseFromDeduplicaResponse(deduplicaResponse, request.getCorrelationId()))
                .map(addressUtils::verifyRequiredFields)
                .flatMap(capAndCountryService::verifyCapAndCountry)
                .map(deduplicatesResponse -> compareNormalizedAddress(deduplicaResponse.getMasterOut(), deduplicatesResponse)))
                .doOnError(error -> log.error("Error during deduplica call for request: [{}]", request.getCorrelationId(), error));
    }

    private DeduplicatesResponse compareNormalizedAddress(AddressOut addressOut, DeduplicatesResponse deduplicatesResponse) {
        AnalogAddress normalizedAddress = deduplicatesResponse.getNormalizedAddress();
        if (normalizedAddress != null && Boolean.FALSE.equals(deduplicatesResponse.getEqualityResult())) {

            boolean equalityResult = addressUtils.compareAddress(
                    addressConverter.getAnalogAddressFromAddressOut(addressOut),
                    normalizedAddress,
                    null);
            if (equalityResult){
                deduplicatesResponse.setEqualityResult(true);
                deduplicatesResponse.setError(DeduplicatesError.PNADDR003.name());
                deduplicatesResponse.setNormalizedAddress(null);
            }
        }
        return deduplicatesResponse;
    }

    private static boolean areRequiredFieldsMissing(DeduplicatesRequest request) {
        return !StringUtils.hasText(request.getTargetAddress().getCity()) ||
                !StringUtils.hasText(request.getTargetAddress().getAddressRow());
    }

    private static DeduplicatesResponse getDeduplicatesResponse(DeduplicatesRequest request) {
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        deduplicatesResponse.setEqualityResult(false);
        deduplicatesResponse.setError(DeduplicatesError.PNADDR001.name());
        deduplicatesResponse.setResultDetails(DeduplicatesResultDetails.RD04.name());
        return deduplicatesResponse;
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
