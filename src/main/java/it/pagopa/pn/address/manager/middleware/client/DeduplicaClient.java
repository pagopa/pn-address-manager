package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.deduplica.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.deduplica.v1.dto.DeduplicaResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.api.DeduplicaApi;
import it.pagopa.pn.commons.log.PnLogger;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.POSTEL;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_SERVICE_DEDUPLICA;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_POSTEL_CLIENT;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_POSTEL_CLIENT;


@CustomLog
@Component
@RequiredArgsConstructor
public class DeduplicaClient {
    private final DeduplicaApi postelApi;
    private final PnAddressManagerConfig pnAddressManagerConfig;


    public Mono<DeduplicaResponse> deduplica(DeduplicaRequest inputDeduplica) {
        log.logInvokingExternalDownstreamService(POSTEL, PROCESS_SERVICE_DEDUPLICA);
        return postelApi.deduplica(pnAddressManagerConfig.getPostelCxId(), pnAddressManagerConfig.getNormalizer().getPostelAuthKey(), inputDeduplica)
                .doOnError(throwable -> log.logInvokationResultDownstreamFailed(POSTEL, throwable.getMessage()))
                .onErrorMap(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        throw new PnInternalAddressManagerException(ERROR_MESSAGE_POSTEL_CLIENT, ERROR_CODE_POSTEL_CLIENT
                                , ex.getStatusCode().value(), PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_DEDUPLICA_ONLINE_ERROR_CODE);
                    }
                    return throwable;
                });
    }
}
