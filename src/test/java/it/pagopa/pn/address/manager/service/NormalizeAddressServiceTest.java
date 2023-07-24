package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {NormalizeAddressService.class, SchedulerConfig.class})
class NormalizeAddressServiceTest {

    @InjectMocks
    private NormalizeAddressService normalizeAddressService;
    @Mock
    private AddressService addressService;

    @Test
    void normalizeAddressAsync() {
        NormalizeItemsRequest normalizeItemsRequest = mock(NormalizeItemsRequest.class);
        AcceptedResponse acceptedResponse = mock(AcceptedResponse.class);
        when(addressService.normalizeAddressAsync(any(),anyString())).thenReturn(Mono.just(acceptedResponse));
        StepVerifier.create(normalizeAddressService.normalizeAddressAsync(normalizeItemsRequest, "cxId"))
                .expectNext(acceptedResponse)
                .verifyComplete();
    }
}