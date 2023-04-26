package it.pagopa.pn.template.rest;

import it.pagopa.pn.template.rest.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.template.service.DeduplicatesAddressService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.scheduler.VirtualTimeScheduler;

import static org.mockito.Mockito.*;

class DeduplicatesAddressControllerTest {

    /**
     * Method under test: {@link DeduplicatesAddressController#deduplicates(String, String, Mono, ServerWebExchange)}
     */
    @Test
    void testDeduplicates4() {
        DeduplicatesAddressService deduplicatesAddressService = mock(DeduplicatesAddressService.class);
        when(deduplicatesAddressService.deduplicates(any()))
                .thenReturn((Mono<DeduplicatesResponse>) mock(Mono.class));
        (new DeduplicatesAddressController(VirtualTimeScheduler.create(true), deduplicatesAddressService))
                .deduplicates("42 Main St", "X Api Key", null, null);
        verify(deduplicatesAddressService).deduplicates(any());
    }
}

