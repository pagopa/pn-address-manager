package it.pagopa.pn.address.manager.rest;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeSyncRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeSyncResponse;
import it.pagopa.pn.address.manager.service.NormalizeSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {NormalizeSyncController.class})
@ExtendWith(SpringExtension.class)
class NormalizeSyncControllerTest {

    @Autowired
    private NormalizeSyncController normalizeSyncController;

    @MockitoBean
    private NormalizeSyncService normalizeSyncService;

    @MockitoBean(name = "addressManagerScheduler")
    private Scheduler scheduler;

    @MockitoBean
    private ServerWebExchange exchange;

    @Test
    void testNormalizeSync() {
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("Via Roma 1");
        analogAddress.setCity("Roma");

        NormalizeSyncRequest request = new NormalizeSyncRequest();
        request.setCorrelationId("corr-1");
        request.setRequestItem(new NormalizeRequest("id-1", analogAddress));

        NormalizeSyncResponse response = new NormalizeSyncResponse();
        response.setNormalizedAddress(analogAddress);

        when(normalizeSyncService.normalizeSync(eq("cxId"), eq("apiKey"), any()))
                .thenReturn(Mono.just(response));

        Assertions.assertNotNull(normalizeSyncController.normalizeSync("cxId", "apiKey", Mono.just(request), exchange));
    }
}


