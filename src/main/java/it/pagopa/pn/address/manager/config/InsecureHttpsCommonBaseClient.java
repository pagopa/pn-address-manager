package it.pagopa.pn.address.manager.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import lombok.CustomLog;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@CustomLog
public class InsecureHttpsCommonBaseClient extends CommonBaseClient {
    @Override
    protected HttpClient buildHttpClient() {
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            return super.buildHttpClient().secure(t -> t.sslContext(sslContext));

        } catch (SSLException e) {
            log.error("Failed to initialize Deduplica client: {}", e.getMessage(), e);
            throw new PnAddressManagerException("Failed to initialize Deduplica client", 500, "DEDUPLICA_CLIENT_ERROR");
        }
    }
}
