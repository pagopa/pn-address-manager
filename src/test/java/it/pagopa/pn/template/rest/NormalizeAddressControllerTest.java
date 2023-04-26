package it.pagopa.pn.template.rest;

import it.pagopa.pn.template.rest.v1.dto.AcceptedResponse;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.template.service.NormalizeAddressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class NormalizeAddressControllerTest {

    @InjectMocks
    private NormalizeAddressController controller;

    @Mock
    private NormalizeAddressService normalizeAddressService;

    @Mock
    ServerWebExchange serverWebExchange;

    @Test
    void normalize() {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId("CorrelationId");
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        when(normalizeAddressService.normalizeAddressAsync(any(), any())).
                thenReturn(Mono.just(acceptedResponse));
        StepVerifier.create(controller.normalize("cxId", "ApiKey", Mono.just(normalizeItemsRequest), serverWebExchange))
              .expectNext(ResponseEntity.ok().body(acceptedResponse));
    }
}
