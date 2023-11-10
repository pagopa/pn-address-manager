package it.pagopa.pn.address.manager.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.api.DeduplicaApi;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DeduplicaClientConfig extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;

    @Bean
    DeduplicaApi deduplicaApi(PnAddressManagerConfig cfg) {
        var apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getDeduplicaBasePath());
        return new DeduplicaApi(apiClient);
    }

    @Override
    protected WebClient.Builder enrichWithDefaultProps(WebClient.Builder builder) {
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            HttpClient httpClient = buildHttpClient().secure(t -> t.sslContext(sslContext));

            return builder
                    .filter(buildRetryExchangeFilterFunction())
                    .clientConnector(new ReactorClientHttpConnector(httpClient));

        } catch (SSLException e) {
            log.error("Failed to initialize Deduplica client: {}", e.getMessage(), e);
            throw new PnAddressManagerException("Failed to initialize Deduplica client", 500, "DEDUPLICA_CLIENT_ERROR");
        }
    }
}
