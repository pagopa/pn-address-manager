package it.pagopa.pn.address.manager.service;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.deduplica.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.deduplica.v1.dto.DeduplicaResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.middleware.client.DeduplicaClient;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DeduplicatesAddressServiceTest {

    @MockBean
    AddressUtils addressUtils;

    @MockBean
    DeduplicaClient postelClient;

    @MockBean
    PnAddressManagerConfig pnAddressManagerConfig;

    @MockBean
    ApiKeyUtils apiKeyUtils;

    @MockBean
    CapAndCountryService capAndCountryService;

    @MockBean
    AddressConverter addressConverter;

    private DeduplicatesAddressService deduplicatesAddressService;

    @Test
    void deduplicates(){
        deduplicatesAddressService = new DeduplicatesAddressService(addressUtils, postelClient, pnAddressManagerConfig,apiKeyUtils, capAndCountryService, addressConverter);
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        when(apiKeyUtils.checkApiKey(anyString(), anyString())).thenReturn(Mono.just(apiKeyModel));
        when(postelClient.deduplica(any())).thenReturn(Mono.just(new DeduplicaResponse()));
        StepVerifier.create(deduplicatesAddressService.deduplicates(new DeduplicatesRequest(), "cxId","apiKey")).expectError().verify();
    }

    @Test
    void deduplicates1(){
        deduplicatesAddressService = new DeduplicatesAddressService(addressUtils, postelClient, pnAddressManagerConfig,apiKeyUtils, capAndCountryService, addressConverter);
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey("apiKey");
        apiKeyModel.setCxId("cxId");
        when(apiKeyUtils.checkApiKey(anyString(), anyString())).thenReturn(Mono.just(apiKeyModel));
        when(postelClient.deduplica(any())).thenReturn(Mono.just(new DeduplicaResponse()));
        StepVerifier.create(deduplicatesAddressService.deduplicates(new DeduplicatesRequest(), "cxId","apiKey")).expectError().verify();
    }

    @Test
    void deduplicates2(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setFlagCsv(Boolean.TRUE);
        deduplicatesAddressService = new DeduplicatesAddressService(addressUtils, postelClient, pnAddressManagerConfig,apiKeyUtils, capAndCountryService, addressConverter);
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey("apiKey");
        apiKeyModel.setCxId("cxId");
        when(apiKeyUtils.checkApiKey(anyString(), anyString())).thenReturn(Mono.just(apiKeyModel));
        when(postelClient.deduplica(any())).thenReturn(Mono.just(new DeduplicaResponse()));
        when(addressUtils.normalizeAddress(any(),any(), any())).thenReturn(new NormalizedAddressResponse());
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setEqualityResult(false);
        StepVerifier.create(deduplicatesAddressService.deduplicates(new DeduplicatesRequest(), "cxId","apiKey")).expectNext(deduplicatesResponse).verifyComplete();
    }
}
