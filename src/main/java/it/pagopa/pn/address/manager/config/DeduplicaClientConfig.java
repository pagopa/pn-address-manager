package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.api.DeduplicaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DeduplicaClientConfig extends InsecureHttpsCommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;

    @Bean
    DeduplicaApi deduplicaApi(PnAddressManagerConfig cfg) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getDeduplicaBasePath());
        return new DeduplicaApi(apiClient);
    }
}
