package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.api.NormalizeAddressServiceSyncApi;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeSyncRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeSyncResponse;
import it.pagopa.pn.address.manager.service.NormalizeSyncService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@lombok.CustomLog
public class NormalizeSyncController implements NormalizeAddressServiceSyncApi {

    private final Scheduler scheduler;
    private final NormalizeSyncService normalizeSyncService;

    public NormalizeSyncController(@Qualifier("addressManagerScheduler") Scheduler scheduler,
                                   NormalizeSyncService normalizeSyncService) {
        this.scheduler = scheduler;
        this.normalizeSyncService = normalizeSyncService;
    }

    @Override
    public Mono<ResponseEntity<NormalizeSyncResponse>> normalizeSync(String pnAddressManagerCxId,
                                                                     String xApiKey,
                                                                     Mono<NormalizeSyncRequest> normalizeSyncRequest,
                                                                     ServerWebExchange exchange) {
        return normalizeSyncRequest
                .flatMap(request -> normalizeSyncService.normalizeSync(pnAddressManagerCxId, xApiKey, request))
                .map(ResponseEntity::ok)
                .publishOn(scheduler);
    }
}


