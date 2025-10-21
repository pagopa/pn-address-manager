package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.ForeignValidationMode;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.api.DeduplicaApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class DeduplicaClientTest {

    @MockitoBean
    DeduplicaApi defaultApi;

    @Test
    void testDeduplica() {
        when(defaultApi.deduplica(anyString(),anyString(),any())).thenReturn(Mono.just(new DeduplicaResponse()));
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        pnAddressManagerConfig.setPostelCxId("postelCxId");
        pnAddressManagerConfig.setPagoPaCxId("pagoPaCxId");
        pnAddressManagerConfig.setNormalizer(normalizer);
        pnAddressManagerConfig.getNormalizer().setPostelAuthKey("postelAuthKey");
        pnAddressManagerConfig.setNormalizer(normalizer);
        pnAddressManagerConfig.setDeduplicaBasePath("http://localhost:8080");
        pnAddressManagerConfig.setForeignValidationMode(ForeignValidationMode.PASSTHROUGH);
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        DeduplicaClient postelClient = new DeduplicaClient(defaultApi, pnAddressManagerConfig);
        postelClient.deduplica(new DeduplicaRequest());
        verify(defaultApi, times(1)).deduplica(anyString(),anyString(),any());
    }
}

