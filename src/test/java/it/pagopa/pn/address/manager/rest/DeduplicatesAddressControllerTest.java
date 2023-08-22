package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.service.DeduplicatesAddressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DeduplicatesAddressController.class, SchedulerConfig.class})
class DeduplicatesAddressControllerTest {

    @Autowired
    private DeduplicatesAddressController deduplicatesAddressController;

    @MockBean
    private DeduplicatesAddressService deduplicatesAddressService;
    @Test
    void testDeduplicates() {
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId("correlationId");
        DeduplicatesRequest deduplicateRequest = new DeduplicatesRequest();
        deduplicateRequest.setCorrelationId("correlationId");
        when(this.deduplicatesAddressService.deduplicates(deduplicateRequest, "xApiKey"))
                .thenReturn(Mono.just(deduplicatesResponse));
        StepVerifier.create(this.deduplicatesAddressController.deduplicates("pnAddressManagerCxId","xApiKey",Mono.just(deduplicateRequest), mock(ServerWebExchange.class)))
                .expectNext(ResponseEntity.ok(deduplicatesResponse))
                .verifyComplete();
    }
}
