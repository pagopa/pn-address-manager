package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.service.NormalizeAddressService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NormalizeAddressController.class, SchedulerConfig.class})
class NormalizeAddressControllerTest {

    @Autowired
    NormalizeAddressController normalizeAddressController;

    @MockBean
    private NormalizeAddressService normalizeAddressService;

    @MockBean
    ServerWebExchange serverWebExchange;

    @Test
    void normalize() {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId("CorrelationId");
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        when(normalizeAddressService.normalizeAddress(any(), any(), any())).
                thenReturn(Mono.just(acceptedResponse));
        StepVerifier.create(normalizeAddressController.normalize("cxId", "ApiKey", Mono.just(normalizeItemsRequest), serverWebExchange))
              .expectNext(ResponseEntity.ok().body(acceptedResponse)).verifyComplete();
    }
}
