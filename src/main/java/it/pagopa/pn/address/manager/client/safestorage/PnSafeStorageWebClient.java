package it.pagopa.pn.address.manager.client.safestorage;

import it.pagopa.pn.address.manager.config.PnSafeStorageClientConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.ApiClient;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.stereotype.Component;

@Component
public class PnSafeStorageWebClient extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;
    private final PnSafeStorageClientConfig config;

    public PnSafeStorageWebClient(ResponseExchangeFilter responseExchangeFilter, PnSafeStorageClientConfig config) {
        this.responseExchangeFilter = responseExchangeFilter;
        this.config = config;
    }


    public ApiClient init() {
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(config.getPnSafeStorageBasePath());
        return apiClient;
    }
}
