package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.normalizzatore.v1.dto.NormalizzazioneResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.normalizzatore.v1.api.NormalizzatoreApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class NormalizzatoreClientTest {
    @MockBean
    NormalizzatoreApi defaultApi;

    @Test
    void testActivatePostel() {
        when(defaultApi.normalizzazione(anyString(), anyString(), any())).thenReturn(Mono.just(new NormalizzazioneResponse()));
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
        normalizzatoreClient.activatePostel(normalizzatoreBatch);
        verify(defaultApi, times(1)).normalizzazione(anyString(), anyString(), any());
    }
}

