package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.api.DeduplicaApi;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DeduplicaClientConfig extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;
    @Bean
    DeduplicaApi deduplicaApi(PnAddressManagerConfig cfg) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getDeduplicaBasePath());
        return new DeduplicaApi(apiClient);
    }
}
