package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NormalizeAddressService.class, SchedulerConfig.class})
class NormalizeAddressServiceTest {

    @Autowired
    private NormalizeAddressService normalizeAddressService;
    @MockBean
    private AddressService addressService;
    @MockBean
    private ApiKeyRepository apiKeyRepository;
    @Test
    void normalizeAddressAsync() {
        NormalizeItemsRequest normalizeItemsRequest = mock(NormalizeItemsRequest.class);
        AcceptedResponse acceptedResponse = mock(AcceptedResponse.class);
        when(addressService.normalizeAddressAsync(any(),anyString())).thenReturn(Mono.just(acceptedResponse));
        when(apiKeyRepository.findById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(normalizeAddressService.normalizeAddressAsync(normalizeItemsRequest,"cxId","xApiKey"))
                .expectNext(acceptedResponse);
    }
}