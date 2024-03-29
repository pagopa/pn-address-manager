package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.normalizzatore.v1.ApiClient;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.normalizzatore.v1.api.NormalizzatoreApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NormalizzatoreClientConfig extends InsecureHttpsCommonBaseClient {


    @Bean
    NormalizzatoreApi normalizzatoreApi(PnAddressManagerConfig cfg) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getNormalizzatoreBasePath());
        return new NormalizzatoreApi(apiClient);
    }

    @Autowired
    @Override
    public void setReadTimeoutMillis(@Value("${pn.address-manager.normalizer-postel-read-timeout-millis}") int readTimeoutMillis) {
        super.setReadTimeoutMillis(readTimeoutMillis);
    }
}
