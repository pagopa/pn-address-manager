package it.pagopa.pn.address.manager.middleware.client;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_POSTEL_CLIENT;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_POSTEL_CLIENT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.DeduplicaRequest;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.v1.api.DefaultApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PostelClientTest {

    DefaultApi defaultApi = mock(DefaultApi.class);
    PostelWebClient postelWebClient = mock(PostelWebClient.class);
    ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);

    @Test
    void testDeduplica() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setPostelBasePath("http://localhost:8080");
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        PostelClient postelClient = new PostelClient(new PostelWebClient(responseExchangeFilter, pnAddressManagerConfig), pnAddressManagerConfig);
        postelClient.deduplica(new DeduplicaRequest(), "cxId", "xApiKey");
        verify(responseExchangeFilter).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction).apply(Mockito.<ExchangeFunction>any());
    }
    @Test
    void testActivatePostel() {
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setPostelAuthKey("Postel Auth Key");
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setPagoPaCxId("cxId");
        pnAddressManagerConfig.setPostelBasePath("http://localhost:8080");
        pnAddressManagerConfig.setNormalizer(normalizer);
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        (new PostelClient(new PostelWebClient(responseExchangeFilter, pnAddressManagerConfig), pnAddressManagerConfig)).activatePostel(new PostelBatch());
        verify(responseExchangeFilter).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction).apply(Mockito.<ExchangeFunction>any());
    }
}

