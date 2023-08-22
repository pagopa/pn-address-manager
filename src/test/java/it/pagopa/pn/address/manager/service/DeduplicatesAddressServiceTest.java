package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration (classes = {DeduplicatesAddressService.class})
@ExtendWith(SpringExtension.class)
class DeduplicatesAddressServiceTest {
    @MockBean
    AddressService addressService;
    @Autowired
    private DeduplicatesAddressService deduplicatesAddressService;
    @MockBean
    private ApiKeyRepository apiKeyRepository;

    @Test
    void deduplicates() {
        DeduplicatesRequest deduplicatesRequest = mock(DeduplicatesRequest.class);
        DeduplicatesResponse deduplicatesResponse = mock(DeduplicatesResponse.class);
        when(apiKeyRepository.findById(any())).thenReturn(Mono.empty());
        when(addressService.normalizeAddress(any())).thenReturn(Mono.just(deduplicatesResponse));
        StepVerifier.create(deduplicatesAddressService.deduplicates(deduplicatesRequest, "xApiKey"))
                .expectNext(deduplicatesResponse);
    }
}

