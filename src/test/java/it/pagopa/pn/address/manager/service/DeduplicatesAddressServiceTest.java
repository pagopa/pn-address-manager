package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DeduplicatesAddressServiceTest {
    @Mock
    AddressService addressService;
    @InjectMocks
    private DeduplicatesAddressService deduplicatesAddressService;

    @Test
    void deduplicates() {
        DeduplicatesRequest deduplicatesRequest = mock(DeduplicatesRequest.class);
        DeduplicatesResponse deduplicatesResponse = mock(DeduplicatesResponse.class);
        when(addressService.normalizeAddress(any())).thenReturn(Mono.just(deduplicatesResponse));
        StepVerifier.create(deduplicatesAddressService.deduplicates(deduplicatesRequest))
                .expectNext(deduplicatesResponse)
                .verifyComplete();
    }
}

