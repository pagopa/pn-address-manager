package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.normalizzatore.v1.dto.NormalizzazioneResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.api.NormalizzatoreApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith (SpringExtension.class)
class NormalizzatoreClientTest {
	@MockBean
	NormalizzatoreApi defaultApi;
	@MockBean
	ResponseExchangeFilter responseExchangeFilter;
	@MockBean
	ApiClient apiClient;
	@Test
	@Disabled
	void testActivatePostel () {
		PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
		normalizer.setPostelAuthKey("Postel Auth Key");
		PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
		pnAddressManagerConfig.setPostelCxId("cxId");
		pnAddressManagerConfig.setApiKey("apiKey");
		pnAddressManagerConfig.setPagoPaCxId("cxId");
		pnAddressManagerConfig.setNormalizzatoreBasePath("http://localhost:8080");
		pnAddressManagerConfig.setNormalizer(normalizer);
		NormalizzatoreClient normalizzatoreClient = new NormalizzatoreClient(defaultApi, pnAddressManagerConfig);
		NormalizzatoreBatch normalizzatoreBatch = new NormalizzatoreBatch();
		normalizzatoreBatch.setBatchId("batchId");
		normalizzatoreBatch.setFileKey("fileKey");
		normalizzatoreBatch.setSha256("sha256");
		NormalizzazioneResponse normalizzazioneResponse = new NormalizzazioneResponse();
		when(defaultApi.normalizzazione(anyString(), anyString(), any())).thenReturn(Mono.just(normalizzazioneResponse));
		normalizzatoreClient.activatePostel(normalizzatoreBatch);
		verify(responseExchangeFilter).andThen(Mockito.<ExchangeFilterFunction>any());
	}
}

