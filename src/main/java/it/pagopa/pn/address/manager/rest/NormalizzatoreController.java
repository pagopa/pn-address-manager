package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.service.NormalizzatoreService;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.api.NormalizzatoreApi;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadRequestData;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.PreLoadResponseData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
public class NormalizzatoreController implements NormalizzatoreApi {
	@Qualifier ("addressManagerScheduler")
	private final Scheduler scheduler;
	private final NormalizzatoreService normalizzatoreService;

	public NormalizzatoreController (Scheduler scheduler, NormalizzatoreService normalizzatoreService) {
		this.scheduler = scheduler;
		this.normalizzatoreService = normalizzatoreService;
	}

	@Override
	public Mono<ResponseEntity<PreLoadResponseData>> presignedUploadRequest (String pnAddressManagerCxId, String xApiKey, Mono<PreLoadRequestData> preLoadRequestData, final ServerWebExchange exchange) {
		return preLoadRequestData
				.flatMap(preLoadRequestData1 -> normalizzatoreService.presignedUploadRequest(preLoadRequestData1, pnAddressManagerCxId))
				.map(preLoadResponseData -> ResponseEntity.ok().body(preLoadResponseData))
				.publishOn(scheduler);
	}

	@Override
	public Mono<ResponseEntity<FileDownloadResponse>> getFile (String fileKey, String pnAddressManagerCxId, String xApiKey, final ServerWebExchange exchange) {
		return normalizzatoreService.getFile(fileKey, pnAddressManagerCxId)
				.map(fileDownloadResponse -> ResponseEntity.ok().body(fileDownloadResponse))
				.publishOn(scheduler);
	}
}
