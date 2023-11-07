package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
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

	public NormalizzazioneResponse activatePostel(PostelBatch postelBatch) {
		log.logInvokingExternalDownstreamService(POSTEL, "Calling Activate Postel");

		NormalizzazioneRequest activatePostelRequest = new NormalizzazioneRequest();
		activatePostelRequest.setRequestId(postelBatch.getBatchId() + RETRY_SUFFIX + postelBatch.getRetry());
		activatePostelRequest.setUri(postelBatch.getFileKey());
		activatePostelRequest.setSha256(postelBatch.getSha256());
		return postelApi.normalizzazione(pnAddressManagerConfig.getPostelCxId(), pnAddressManagerConfig.getNormalizer().getPostelAuthKey(), activatePostelRequest).
				doOnError(throwable -> log.logInvokationResultDownstreamFailed(POSTEL, throwable.getMessage()))
				.block();

	}
}
