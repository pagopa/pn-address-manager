package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.service.DeduplicatesAddressService;
import it.pagopa.pn.address.manager.rest.v1.api.DeduplicatesAddressServiceApi;
import it.pagopa.pn.address.manager.rest.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.rest.v1.dto.DeduplicatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@Slf4j
public class DeduplicatesAddressController implements DeduplicatesAddressServiceApi {

    private final Scheduler scheduler;
    private final DeduplicatesAddressService deduplicatesAddressService;

    public DeduplicatesAddressController(@Qualifier("addressManagerScheduler") Scheduler scheduler,
                                         DeduplicatesAddressService deduplicatesAddressService){
        this.scheduler = scheduler;
        this.deduplicatesAddressService = deduplicatesAddressService;
    }


    /**
     * POST /address-private/deduplicates : Richiesta di deduplica per una coppia di indirizzi
     * Servizio Sincrono per la deduplica di indirizzi.  I due indirizzi vengono confrontatati e viene restituito il risultato del confronto In caso di disuguaglianza viene restituita anche la versione Normalizzata del secondo
     *
     * @param pnAddressManagerCxId  (required)
     * @param xApiKey Credenziale di accesso (required)
     * @param deduplicatesRequest  (required)
     * @return Risultato servizio deduplica (status code 200)
     *         or Bad Request (status code 400)
     *         or InternalServerError (status code 500)
     */
    @Override
    public Mono<ResponseEntity<DeduplicatesResponse>> deduplicates(String pnAddressManagerCxId, String xApiKey, Mono<DeduplicatesRequest> deduplicatesRequest, ServerWebExchange exchange) {
        return deduplicatesRequest
                .map(deduplicatesAddressService::deduplicates)
                .map(deduplicateResponse -> ResponseEntity.ok().body(deduplicateResponse))
                .publishOn(scheduler);
    }
}
