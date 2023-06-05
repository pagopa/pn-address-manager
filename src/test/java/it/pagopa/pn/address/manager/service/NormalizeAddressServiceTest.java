package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.address.manager.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.server.v1.dto.NormalizeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NormalizeAddressService.class, SchedulerConfig.class})
class NormalizeAddressServiceTest {

    @Autowired
    private NormalizeAddressService normalizeAddressService;
    @MockBean
    private AddressUtils addressUtils;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void normalizeAddressAsync() throws JsonProcessingException {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId("correlationId");
        List<NormalizeResult> normalize = new ArrayList<>();
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
        when(addressUtils.normalizeAddresses(any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        StepVerifier.create(normalizeAddressService.normalizeAddressAsync("cxId", normalizeItemsRequest))
                .expectNext(acceptedResponse).verifyComplete();
    }

    @Test
    void normalizeAddressAsyncError() throws JsonProcessingException {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId("correlationId");
        List<NormalizeResult> normalize = new ArrayList<>();
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        when(addressUtils.normalizeAddresses(any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        StepVerifier.create(normalizeAddressService.normalizeAddressAsync("cxId", normalizeItemsRequest))
                .expectNext(acceptedResponse).verifyComplete();
    }
}