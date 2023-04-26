package it.pagopa.pn.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.rest.v1.dto.AcceptedResponse;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.template.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NormalizeAddressServiceTest {

    @InjectMocks
    private NormalizeAddressService normalizeAddressService;
    @Mock
    private AddressUtils addressUtils;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventService eventService;

    @Test
    void normalizeAddressAsync() throws JsonProcessingException {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId("correlationId");
        NormalizeItemsResult normalize = mock(NormalizeItemsResult.class);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
        when(addressUtils.normalizeAddresses(any(),any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        StepVerifier.create(normalizeAddressService.normalizeAddressAsync("cxId", Mono.just(normalizeItemsRequest)))
                .expectNext(acceptedResponse).verifyComplete();
    }

    @Test
    void normalizeAddressAsyncError() throws JsonProcessingException {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId("correlationId");
        NormalizeItemsResult normalize = mock(NormalizeItemsResult.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        when(addressUtils.normalizeAddresses(any(),any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        StepVerifier.create(normalizeAddressService.normalizeAddressAsync("cxId", Mono.just(normalizeItemsRequest)))
                .expectNext(acceptedResponse).verifyComplete();
    }
}