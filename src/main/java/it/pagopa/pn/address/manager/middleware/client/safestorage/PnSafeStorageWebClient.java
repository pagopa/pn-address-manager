package it.pagopa.pn.address.manager.middleware.client.safestorage;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileDownloadApi;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PnSafeStorageWebClient extends CommonBaseClient {


    @Bean
    FileUploadApi fileUploadApi(PnAddressManagerConfig cfg, ResponseExchangeFilter responseExchangeFilter) {
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getSafeStorageBasePath());
        return new FileUploadApi(apiClient);
    }

    @Bean
    FileDownloadApi fileDownloadApi(PnAddressManagerConfig cfg, ResponseExchangeFilter responseExchangeFilter) {
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()
                .filters(f -> f.add(responseExchangeFilter))));
        apiClient.setBasePath(cfg.getSafeStorageBasePath());
        return new FileDownloadApi(apiClient);
    }

}
