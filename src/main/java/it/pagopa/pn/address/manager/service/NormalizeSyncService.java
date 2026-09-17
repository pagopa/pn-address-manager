package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeSyncRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeSyncResponse;
import it.pagopa.pn.address.manager.middleware.client.NormalizeSyncClient;
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
public class NormalizeSyncService {

    private final NormalizeSyncClient normalizeSyncClient;
    private final AddressConverter addressConverter;
    private final ApiKeyUtils apiKeyUtils;

    public Mono<NormalizeSyncResponse> normalizeSync(String pnAddressManagerCxId,
                                                     String xApiKey,
                                                     NormalizeSyncRequest request) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, request.getCorrelationId());

        Mono<NormalizeSyncResponse> normalizeSyncResponseMono = apiKeyUtils.checkApiKey(pnAddressManagerCxId, xApiKey)
                .doOnNext(apiKeyModel -> {
                    log.logCheckingOutcome(PROCESS_CHECKING_APIKEY, true);
                    log.info(ADDRESS_NORMALIZER_SYNC + "Founded apikey for request: [{}]", request.getCorrelationId());
                })
                .map(unused -> addressConverter.createNormalizeSyncRequestFromRequest(request))
                .flatMap(postelRequest -> normalizeSyncClient.normalizzazioneSync(pnAddressManagerCxId, xApiKey, postelRequest))
                .map(addressConverter::createNormalizeSyncResponseFromResponse);

        return MDCUtils.addMDCToContextAndExecute(normalizeSyncResponseMono);
    }
}


