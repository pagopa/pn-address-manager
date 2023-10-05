package it.pagopa.pn.address.manager.middleware.client;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.v1.ApiClient;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.stereotype.Component;

@Component
public class PostelWebClient extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;
    private final PnAddressManagerConfig cfg;
    public PostelWebClient(ResponseExchangeFilter responseExchangeFilter, PnAddressManagerConfig cfg) {
        this.responseExchangeFilter = responseExchangeFilter;
        this.cfg = cfg;
    }


    public ApiClient init() {
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getPostelBasePath());
        return apiClient;
    }
}
