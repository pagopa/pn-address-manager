package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.ApiClient;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.api.DeduplicaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DeduplicaClientConfig extends InsecureHttpsCommonBaseClient {

    @Bean
    DeduplicaApi deduplicaApi(PnAddressManagerConfig cfg) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getDeduplicaBasePath());
        return new DeduplicaApi(apiClient);
    }
}
