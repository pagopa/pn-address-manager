package it.pagopa.pn.address.manager.middleware.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.normalizzatore.v1.ApiClient;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.stereotype.Component;
import reactor.netty.http.client.HttpClient;
import java.util.concurrent.TimeUnit;

@Component
public class NormalizzatoreWebClient extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;
    private final PnAddressManagerConfig cfg;

    public NormalizzatoreWebClient(ResponseExchangeFilter responseExchangeFilter, PnAddressManagerConfig cfg) {
        this.responseExchangeFilter = responseExchangeFilter;
        this.cfg = cfg;
    }


    public ApiClient init() {
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getNormalizzatoreBasePath());
        return apiClient;
    }

    @Override
    protected HttpClient buildHttpClient() {
        return super.buildHttpClient()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.parseInt(cfg.getNormalizer().getPostel().getClientTimeout()))
                .doOnConnected(connection -> connection.addHandlerLast
                        (new ReadTimeoutHandler(Long.parseLong(cfg.getNormalizer().getPostel().getClientTimeout()), TimeUnit.MILLISECONDS)));

    }
}
