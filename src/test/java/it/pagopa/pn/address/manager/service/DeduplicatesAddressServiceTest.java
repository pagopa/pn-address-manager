package it.pagopa.pn.address.manager.service;


import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.InputDeduplica;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.RisultatoDeduplica;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.middleware.client.PostelClient;
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
    PostelClient postelClient;

    @MockBean
    AddressConverter addressConverter;

    @MockBean
    PnAddressManagerConfig pnAddressManagerConfig;

    @MockBean
    ApiKeyRepository apiKeyRepository;

    private DeduplicatesAddressService deduplicatesAddressService;

    @Test
    void deduplicates(){
        deduplicatesAddressService = new DeduplicatesAddressService(addressUtils, postelClient, addressConverter,pnAddressManagerConfig,apiKeyRepository);
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        when(apiKeyRepository.findById(anyString())).thenReturn(Mono.just(apiKeyModel));
        when(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(any())).thenReturn(new InputDeduplica());
        when(postelClient.deduplica(any())).thenReturn(Mono.just(new RisultatoDeduplica()));
        StepVerifier.create(deduplicatesAddressService.deduplicates(new DeduplicatesRequest(), "apiKey")).expectError().verify();
    }

    @Test
    void deduplicates1(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setFlagCsv(true);
        deduplicatesAddressService = new DeduplicatesAddressService(addressUtils, postelClient, addressConverter,pnAddressManagerConfig,apiKeyRepository);
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        when(apiKeyRepository.findById(anyString())).thenReturn(Mono.just(apiKeyModel));
        when(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(any())).thenReturn(new InputDeduplica());
        when(postelClient.deduplica(any())).thenReturn(Mono.just(new RisultatoDeduplica()));
        DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
        deduplicatesRequest.setCorrelationId("correlationId");
        AnalogAddress base = new AnalogAddress();
        base.setCity("Roma ");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("RM  ");
        base.setCap("00010");
        deduplicatesRequest.setBaseAddress(base);
        deduplicatesRequest.setTargetAddress(base);
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        normalizedAddressResponse.setNormalizedAddress(base);
        normalizedAddressResponse.setId("id");
        normalizedAddressResponse.setItalian(true);
        when(addressUtils.normalizeAddress(base,null)).thenReturn(normalizedAddressResponse);
        when(addressUtils.compareAddress(base, base, true)).thenReturn(true);
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setNormalizedAddress(base);
        deduplicatesResponse.setEqualityResult(true);
        deduplicatesResponse.setCorrelationId("correlationId");
        StepVerifier.create(deduplicatesAddressService.deduplicates(deduplicatesRequest, "apiKey")).expectNext(deduplicatesResponse).verifyComplete();
    }


}
