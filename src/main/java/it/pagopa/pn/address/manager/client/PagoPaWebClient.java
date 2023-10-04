package it.pagopa.pn.address.manager.client;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Component
@lombok.CustomLog
public class PagoPaWebClient extends CommonWebClient {

    private final PnAddressManagerConfig pnAddressManagerConfig;

    public PagoPaWebClient(PnAddressManagerConfig pnAddressManagerConfig) {
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    public WebClient init() {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(pnAddressManagerConfig.getWebClient().getTcpMaxPoolsize())
                .pendingAcquireMaxCount(pnAddressManagerConfig.getWebClient().getTcpMaxQueuedConnections())
                .pendingAcquireTimeout(Duration.ofMillis(pnAddressManagerConfig.getWebClient().getTcpPendingAcquiredTimeout()))
                .maxIdleTime(Duration.ofMillis(pnAddressManagerConfig.getWebClient().getTcpPoolIdleTimeout())).build();

        HttpClient httpClient = HttpClient.create(provider);

        return super.initWebClient(httpClient, pnAddressManagerConfig.getWebClient().getBasePath());
    }
}
