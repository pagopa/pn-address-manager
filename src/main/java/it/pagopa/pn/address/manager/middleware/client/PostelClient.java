package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.InputDeduplica;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.RequestActivatePostel;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.ResponseActivatePostel;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.RisultatoDeduplica;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes;
import it.pagopa.pn.address.manager.msclient.generated.postel.v1.api.DefaultApi;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;


@CustomLog
@Component
public class PostelClient {
	private final DefaultApi postelApi;
	private static final String POSTEL = "POSTEL";

	public PostelClient(PostelWebClient postelWebClient) {
		this.postelApi = new DefaultApi(postelWebClient.init());
	}

	public Mono<RisultatoDeduplica> deduplica(InputDeduplica inputDeduplica) {
		log.logInvokingExternalService(POSTEL, "Calling DeduplicaNormalizzaRest");
		return postelApi.postelMockPagoPaDeduplicaRestPagoPaDeduplicaRestNormalizzaRestPost(inputDeduplica)
				.onErrorMap(throwable -> {
					if (throwable instanceof WebClientResponseException ex) {
						throw new PnAddressManagerException(ERROR_MESSAGE_POSTEL_CLIENT, ERROR_CODE_POSTEL_CLIENT
								, ex.getStatusCode().value(), PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_DEDUPLICA_ONLINE_ERROR_CODE);
					}
					return throwable;
				});
	}

	public Mono<ResponseActivatePostel> activatePostel(String key) {
		log.logInvokingExternalService(POSTEL, "Calling Activate Postel");
		RequestActivatePostel activatePostelRequest = new RequestActivatePostel();
		activatePostelRequest.setFileKey(key);
		return postelApi.activatePostel(activatePostelRequest)
				.onErrorMap(throwable -> {
					if (throwable instanceof WebClientResponseException ex) {
						throw new PnAddressManagerException(ERROR_MESSAGE_POSTEL_CLIENT, ERROR_CODE_POSTEL_CLIENT
								, ex.getStatusCode().value(), PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_ACTIVATE_POSTEL_ERROR_CODE);
					}
					return throwable;
				});

	}
}
