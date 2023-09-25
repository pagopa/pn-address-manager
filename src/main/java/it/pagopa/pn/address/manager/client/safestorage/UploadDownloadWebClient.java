package it.pagopa.pn.address.manager.client.safestorage;

import it.pagopa.pn.address.manager.config.UploadDownloadClientConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.v1.ApiClient;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.stereotype.Component;

@Component
public class UploadDownloadWebClient extends CommonBaseClient {

    private final ResponseExchangeFilter responseExchangeFilter;
    private final UploadDownloadClientConfig config;

    public UploadDownloadWebClient(ResponseExchangeFilter responseExchangeFilter, UploadDownloadClientConfig config) {
        this.responseExchangeFilter = responseExchangeFilter;
        this.config = config;
    }


    public ApiClient init() {
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(config.getUploadDownloadBasePath());
        return apiClient;
    }
}
