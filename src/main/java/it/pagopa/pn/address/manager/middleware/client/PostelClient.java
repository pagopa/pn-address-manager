package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.DeduplicaResponse;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.NormalizzazioneRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.NormalizzazioneResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes;
import it.pagopa.pn.address.manager.msclient.generated.postel.v1.api.DefaultApi;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.constant.AddressmanagerConstant.POSTEL;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;


@CustomLog
@Component
public class PostelClient {
	private final DefaultApi postelApi;

	private final PnAddressManagerConfig pnAddressManagerConfig;
	public PostelClient(PostelWebClient postelWebClient, PnAddressManagerConfig pnAddressManagerConfig) {
		this.postelApi = new DefaultApi(postelWebClient.init());
		this.pnAddressManagerConfig = pnAddressManagerConfig;
	}

	public Mono<DeduplicaResponse> deduplica(DeduplicaRequest inputDeduplica, String cxId, String xApiKey) {
		log.logInvokingExternalService(POSTEL, "Calling DeduplicaNormalizzaRest");
		return postelApi.deduplica(cxId, xApiKey, inputDeduplica)
				.onErrorMap(throwable -> {
					if (throwable instanceof WebClientResponseException ex) {
						throw new PnInternalAddressManagerException(ERROR_MESSAGE_POSTEL_CLIENT, ERROR_CODE_POSTEL_CLIENT
								, ex.getStatusCode().value(), PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_DEDUPLICA_ONLINE_ERROR_CODE);
					}
					return throwable;
				});
	}

	public Mono<NormalizzazioneResponse> activatePostel(PostelBatch postelBatch) {
		log.logInvokingExternalService(POSTEL, "Calling Activate Postel");

		NormalizzazioneRequest activatePostelRequest = new NormalizzazioneRequest();
		activatePostelRequest.setRequestId(postelBatch.getBatchId());
		activatePostelRequest.setUri(postelBatch.getFileKey());
		activatePostelRequest.setSha256(postelBatch.getSha256());
		return postelApi.normalizzazione(pnAddressManagerConfig.getPagoPaCxId(), pnAddressManagerConfig.getNormalizer().getPostelAuthKey(), activatePostelRequest)
				.onErrorMap(throwable -> {
					if (throwable instanceof WebClientResponseException ex) {
						throw new PnInternalAddressManagerException(ERROR_MESSAGE_POSTEL_CLIENT, ERROR_CODE_POSTEL_CLIENT
								, ex.getStatusCode().value(), PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_ACTIVATE_POSTEL_ERROR_CODE);
					}
					return throwable;
				});

	}
}
