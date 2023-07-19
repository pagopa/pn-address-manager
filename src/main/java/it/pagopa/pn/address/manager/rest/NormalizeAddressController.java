package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.api.NormalizeAddressServiceApi;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@lombok.CustomLog
public class NormalizeAddressController implements NormalizeAddressServiceApi {
    @Qualifier("addressManagerScheduler")
    private final Scheduler scheduler;

    private final NormalizeAddressService normalizeAddressService;

    public NormalizeAddressController(Scheduler scheduler, NormalizeAddressService normalizeAddressService) {
        this.scheduler = scheduler;
        this.normalizeAddressService = normalizeAddressService;
    }


    /**
     * POST /address-private/normalize : Richiesta di normalizzazione per una lista di indirizzi
     * Servizio Asincrono per la normalizzazione di un indirizzo. La risposta arriva su una specifica coda da configurare
     *
     * @param pnAddressManagerCxId  (required)
     * @param xApiKey               Credenziale di accesso (required)
     * @param normalizeItemsRequest (required)
     * @return Richiesta presa in carico (status code 202)
     * or Bad Request (status code 400)
     * or InternalServerError (status code 500)
     */
    @Override
    public Mono<ResponseEntity<AcceptedResponse>> normalize(String pnAddressManagerCxId, String xApiKey, Mono<NormalizeItemsRequest> normalizeItemsRequest, final ServerWebExchange exchange) {
        return normalizeItemsRequest
                .flatMap(request -> normalizeAddressService.normalizeAddressAsync(request, pnAddressManagerCxId))
                .map(acceptedResponse -> ResponseEntity.ok().body(acceptedResponse))
                .publishOn(scheduler);
    }
}
