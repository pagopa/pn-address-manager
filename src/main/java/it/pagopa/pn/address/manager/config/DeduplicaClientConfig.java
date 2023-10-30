package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.api.DeduplicaApi;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.ApiClient;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeduplicaClientConfig extends CommonBaseClient {
    @Bean
    DeduplicaApi deduplicaApi(PnAddressManagerConfig cfg) {
        var apiClient = new it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getDeduplicaBasePath());
        return new DeduplicaApi(apiClient);
    }
}
