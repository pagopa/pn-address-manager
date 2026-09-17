package it.pagopa.pn.address.manager.middleware.client;

import it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.api.NormalizzatoreApi;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.NormalizzazioneSyncRequest;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.NormalizzazioneSyncResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.POSTEL;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_POSTEL_CLIENT;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_POSTEL_CLIENT;

@CustomLog
@Component
@RequiredArgsConstructor
public class NormalizeSyncClient {

    private final NormalizzatoreApi postelApi;

    public Mono<NormalizzazioneSyncResponse> normalizzazioneSync(String pnAddressManagerCxId,
                                                                 String xApiKey,
                                                                 NormalizzazioneSyncRequest request) {
        log.logInvokingExternalDownstreamService(POSTEL, "Calling Postel");
        return postelApi.normalizzazioneSync(pnAddressManagerCxId, xApiKey, request)
                .doOnError(throwable -> log.logInvokationResultDownstreamFailed(POSTEL, throwable.getMessage(), throwable))
                .onErrorMap(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        throw new PnInternalAddressManagerException(
                                ERROR_MESSAGE_POSTEL_CLIENT,
                                ERROR_CODE_POSTEL_CLIENT,
                                ex.getStatusCode().value(),
                                PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_NORMALIZZAZIONE_SYNC_ONLINE_ERROR_CODE);
                    }
                    return throwable;
                });
    }
}


