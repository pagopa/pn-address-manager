package it.pagopa.pn.address.manager.middleware.client;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.deduplica.v1.dto.DeduplicaRequest;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.api.DefaultApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static org.mockito.Mockito.*;

class DeduplicaClientTest {

    DefaultApi defaultApi = mock(DefaultApi.class);
    DeduplicaWebClient postelWebClient = mock(DeduplicaWebClient.class);
    ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);

    @Test
    @Disabled
    void testDeduplica() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        pnAddressManagerConfig.setPostelCxId("postelCxId");
        pnAddressManagerConfig.setPagoPaCxId("pagoPaCxId");
        pnAddressManagerConfig.setNormalizer(normalizer);
        pnAddressManagerConfig.getNormalizer().setPostelAuthKey("postelAuthKey");
        pnAddressManagerConfig.setNormalizer(normalizer);
        pnAddressManagerConfig.setDeduplicaBasePath("http://localhost:8080");
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        DeduplicaClient postelClient = new DeduplicaClient(new DeduplicaWebClient(responseExchangeFilter, pnAddressManagerConfig), pnAddressManagerConfig);
        postelClient.deduplica(new DeduplicaRequest());
        verify(responseExchangeFilter).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction).apply(Mockito.<ExchangeFunction>any());
    }
}

