package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.api.NormalizzatoreApi;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.POSTEL;
import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.RETRY_SUFFIX;


@CustomLog
@Component
@RequiredArgsConstructor
public class NormalizzatoreClient {
	private final NormalizzatoreApi postelApi;

	private final PnAddressManagerConfig pnAddressManagerConfig;

	public NormalizzazioneResponse activatePostel(NormalizzatoreBatch normalizzatoreBatch) {
		log.logInvokingExternalDownstreamService(POSTEL, "Calling Activate Postel");

		NormalizzazioneRequest activatePostelRequest = new NormalizzazioneRequest();
		activatePostelRequest.setRequestId(normalizzatoreBatch.getBatchId() + RETRY_SUFFIX + normalizzatoreBatch.getRetry());
		activatePostelRequest.setUri(normalizzatoreBatch.getFileKey());
		activatePostelRequest.setSha256(normalizzatoreBatch.getSha256());
		return postelApi.normalizzazione(pnAddressManagerConfig.getPostelCxId(), pnAddressManagerConfig.getNormalizer().getPostelAuthKey(), activatePostelRequest).
				doOnError(throwable -> log.logInvokationResultDownstreamFailed(POSTEL, throwable.getMessage()))
				.block();

	}
}
