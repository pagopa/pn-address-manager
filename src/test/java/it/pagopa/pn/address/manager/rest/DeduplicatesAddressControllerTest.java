package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.service.DeduplicatesAddressService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {DeduplicatesAddressController.class})
@ExtendWith(SpringExtension.class)
class DeduplicatesAddressControllerTest {
    @Autowired
    private DeduplicatesAddressController deduplicatesAddressController;

    @MockBean
    private DeduplicatesAddressService deduplicatesAddressService;

    @MockBean(name = "addressManagerScheduler")
    private Scheduler scheduler;

    @MockBean
    private ServerWebExchange exchange;

    /**
     * Method under test: {@link DeduplicatesAddressController#deduplicates(String, String, Mono, ServerWebExchange)}
     */
    @Test
    void testDeduplicates() {
        AnalogAddress address = new AnalogAddress();
        DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
        deduplicatesRequest.setCorrelationId("correlationId");
        deduplicatesRequest.setBaseAddress(address);
        deduplicatesRequest.setTargetAddress(address);
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId("correlationId");
        deduplicatesResponse.setNormalizedAddress(address);
        when(deduplicatesAddressService.deduplicates(any(),any(), any())).thenReturn(Mono.just(deduplicatesResponse));
        Assertions.assertNotNull(deduplicatesAddressController.deduplicates("cxId","apiKey",Mono.just(deduplicatesRequest),exchange));
    }
}

