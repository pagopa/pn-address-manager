package it.pagopa.pn.address.manager.client;

import it.pagopa.pn.address.manager.config.PostelClientConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.v1.ApiClient;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.stereotype.Component;

@Component
public class PostelWebClient extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;
    private final PostelClientConfig config;

    public PostelWebClient(ResponseExchangeFilter responseExchangeFilter, PostelClientConfig config) {
        this.responseExchangeFilter = responseExchangeFilter;
        this.config = config;
    }


    public ApiClient init() {
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(config.getPostelBasePath());
        return apiClient;
    }
}
