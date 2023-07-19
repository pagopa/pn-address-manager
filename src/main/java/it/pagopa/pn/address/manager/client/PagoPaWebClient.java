package it.pagopa.pn.address.manager.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Component
@lombok.CustomLog
public class PagoPaWebClient extends CommonWebClient {

    private final Integer tcpMaxPoolSize;
    private final Integer tcpMaxQueuedConnections;
    private final Integer tcpPendingAcquireTimeout;
    private final Integer tcpPoolIdleTimeout;
    private final String basePath;

    public PagoPaWebClient(@Value("${pn.address.manager.webclient.pago-pa.tcp-max-poolsize}") Integer tcpMaxPoolSize,
                           @Value("${pn.address.manager.webclient.pago-pa.tcp-max-queued-connections}") Integer tcpMaxQueuedConnections,
                           @Value("${pn.address.manager.webclient.pago-pa.tcp-pending-acquired-timeout}") Integer tcpPendingAcquireTimeout,
                           @Value("${pn.address.manager.webclient.pago-pa.tcp-pool-idle-timeout}") Integer tcpPoolIdleTimeout,
                           @Value("${pn.address.manager.webclient.pago-pa.base-path}") String basePath) {
        this.tcpMaxPoolSize = tcpMaxPoolSize;
        this.tcpMaxQueuedConnections = tcpMaxQueuedConnections;
        this.tcpPendingAcquireTimeout = tcpPendingAcquireTimeout;
        this.tcpPoolIdleTimeout = tcpPoolIdleTimeout;
        this.basePath = basePath;
    }

    public WebClient init() {
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(tcpMaxPoolSize)
                .pendingAcquireMaxCount(tcpMaxQueuedConnections)
                .pendingAcquireTimeout(Duration.ofMillis(tcpPendingAcquireTimeout))
                .maxIdleTime(Duration.ofMillis(tcpPoolIdleTimeout)).build();

        HttpClient httpClient = HttpClient.create(provider);

        return super.initWebClient(httpClient, basePath);
    }
}
