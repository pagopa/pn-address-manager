package it.pagopa.pn.address.manager.middleware.client;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.api.DefaultApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static org.mockito.Mockito.*;

class NormalizzatoreClientTest {

    DefaultApi defaultApi = mock(DefaultApi.class);
    NormalizzatoreWebClient postelWebClient = mock(NormalizzatoreWebClient.class);
    ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);

    @Test
    @Disabled
    void testActivatePostel() {
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setPostelAuthKey("Postel Auth Key");
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setPagoPaCxId("cxId");
        pnAddressManagerConfig.setNormalizzatoreBasePath("http://localhost:8080");
        pnAddressManagerConfig.setNormalizer(normalizer);
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        PostelBatch postelBatch = new PostelBatch();
        postelBatch.setBatchId("batchId");
        postelBatch.setFileKey("fileKey");
        postelBatch.setSha256("sha256");
        (new NormalizzatoreClient(new NormalizzatoreWebClient(responseExchangeFilter, pnAddressManagerConfig), pnAddressManagerConfig)).activatePostel(postelBatch);
        verify(responseExchangeFilter).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction).apply(Mockito.<ExchangeFunction>any());
    }
}

