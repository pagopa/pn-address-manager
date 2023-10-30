package it.pagopa.pn.address.manager.middleware.client;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.api.DefaultApi;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.api.NormalizzatoreApi;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class PostelWebClient  {

    @Configuration
    class NormalizzatoreClientConfig extends CommonBaseClient {

        @Bean
        NormalizzatoreApi normalizzatoreApi(PnAddressManagerConfig cfg) {
            var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
            apiClient.setBasePath(cfg.getNormalizzatoreBasePath());
            return new NormalizzatoreApi(apiClient);
        }

        @Autowired
        @Override
        public void setConnectionTimeoutMillis(@Value("${pn.address-manager.connection-timeout-millis}") int connectionTimeoutMillis) {
            super.setConnectionTimeoutMillis(connectionTimeoutMillis);
        }

        @Autowired
        @Override
        public void setReadTimeoutMillis(@Value("${pn.address-manager.connection-timeout-millis}") int readTimeoutMillis) {
            super.setReadTimeoutMillis(readTimeoutMillis);
        }
    }

    @Configuration
    class DeduplicaClientConfig extends CommonBaseClient {

        @Bean
        DefaultApi deduplicaApi(PnAddressManagerConfig cfg) {
            var apiClient = new it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
            apiClient.setBasePath(cfg.getDeduplicaBasePath());
            return new DefaultApi(apiClient);
        }
    }

}
