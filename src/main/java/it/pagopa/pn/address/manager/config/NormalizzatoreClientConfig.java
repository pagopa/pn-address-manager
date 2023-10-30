package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.api.NormalizzatoreApi;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NormalizzatoreClientConfig extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;

    @Bean
    NormalizzatoreApi normalizzatoreApi(PnAddressManagerConfig cfg) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getNormalizzatoreBasePath());
        return new NormalizzatoreApi(apiClient);
    }

    @Autowired
    @Override
    public void setReadTimeoutMillis(@Value("${pn.address-manager.normalizer-postel-read-timeout-millis}") int readTimeoutMillis) {
        super.setReadTimeoutMillis(readTimeoutMillis);
    }

}
