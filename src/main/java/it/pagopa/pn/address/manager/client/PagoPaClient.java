package it.pagopa.pn.address.manager.client;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaRequest;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaResponse;
import it.pagopa.pn.commons.log.PnLogger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_SERVICE_DEDUPLICA_ONLINE;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_SERVICE_POSTEL_ATTIVAZIONE_SINI;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_DEDUPLICA_ONLINE_ERROR_CODE;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_POSTEL_ATTIVAZIONE_SINI;

@Component
@lombok.CustomLog
public class PagoPaClient {

    private final WebClient webClient;

    protected PagoPaClient(PagoPaWebClient pagoPaWebClient) {
        this.webClient = pagoPaWebClient.init();
    }


    public Mono<DeduplicaResponse> deduplicaOnline(DeduplicaRequest deduplicaRequest) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_ADDRESS_MANAGER, PROCESS_SERVICE_DEDUPLICA_ONLINE);
        return webClient.post()
                .uri("/normalizzaRest")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(deduplicaRequest)
                .retrieve()
                .bodyToMono(DeduplicaResponse.class)
                .doOnError(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        throw new PnAddressManagerException(ex.getMessage(), ex.getStatusText()
                                , ex.getStatusCode().value(), ERROR_ADDRESS_MANAGER_DEDUPLICA_ONLINE_ERROR_CODE);
                    }
                });
    }

    public Mono<String> activateSINIComponent(String soapMessage) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_ADDRESS_MANAGER, PROCESS_SERVICE_POSTEL_ATTIVAZIONE_SINI);
        return webClient.post()
                .uri("/SINI/WcfSiniService.svc")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(soapMessage)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        throw new PnAddressManagerException(ex.getMessage(), ex.getStatusText()
                                , ex.getStatusCode().value(), ERROR_ADDRESS_MANAGER_POSTEL_ATTIVAZIONE_SINI);
                    }
                });
    }
}
