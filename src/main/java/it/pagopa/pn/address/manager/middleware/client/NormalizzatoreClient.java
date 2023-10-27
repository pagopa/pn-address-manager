package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.api.DefaultApi;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_SERVICE_POSTEL_ATTIVAZIONE;
import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.RETRY_SUFFIX;


@CustomLog
@Component
public class NormalizzatoreClient {
	private final DefaultApi postelApi;

	private final PnAddressManagerConfig pnAddressManagerConfig;
	public NormalizzatoreClient(NormalizzatoreWebClient postelWebClient, PnAddressManagerConfig pnAddressManagerConfig) {
		this.postelApi = new DefaultApi(postelWebClient.init()) {
		};
		this.pnAddressManagerConfig = pnAddressManagerConfig;
	}

	public NormalizzazioneResponse activatePostel(PostelBatch postelBatch) {
		log.logInvokingExternalService(PROCESS_SERVICE_POSTEL_ATTIVAZIONE, "Calling Activate Postel");

		NormalizzazioneRequest activatePostelRequest = new NormalizzazioneRequest();
		activatePostelRequest.setRequestId(postelBatch.getBatchId() + RETRY_SUFFIX + postelBatch.getRetry());
		activatePostelRequest.setUri(postelBatch.getFileKey());
		activatePostelRequest.setSha256(postelBatch.getSha256());
		return postelApi.normalizzazione(pnAddressManagerConfig.getPostelCxId(), pnAddressManagerConfig.getNormalizer().getPostelAuthKey(), activatePostelRequest)
				.block();

	}
}
