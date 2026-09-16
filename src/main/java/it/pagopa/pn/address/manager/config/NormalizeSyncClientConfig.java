package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.ApiClient;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.api.NormalizzatoreApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class NormalizeSyncClientConfig extends InsecureHttpsCommonBaseClient {

    @Bean
    NormalizzatoreApi normalizzatoreSyncApi(PnAddressManagerConfig cfg) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder(), "NORMALIZZATORE"));
        apiClient.setBasePath(cfg.getDeduplicaBasePath());
        return new NormalizzatoreApi(apiClient);
    }

    @Autowired
    @Override
    public void setReadTimeoutMillis(@Value("${pn.address-manager.normalizer-postel-read-timeout-millis}") int readTimeoutMillis) {
        super.setReadTimeoutMillis(readTimeoutMillis);
    }
}


