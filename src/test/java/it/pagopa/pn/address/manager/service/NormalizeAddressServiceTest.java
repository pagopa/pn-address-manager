package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NormalizeAddressService.class, SchedulerConfig.class})
@TestPropertySource(properties = "pn.address.manager.flag.csv=true")
@TestPropertySource(properties = "pn.address.manager.batch.ttl=1000")
class NormalizeAddressServiceTest {

    @Autowired
    private NormalizeAddressService normalizeAddressService;
    @MockBean
    private AddressUtils addressUtils;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private SqsService sqsService;

    @MockBean
    private AddressBatchRequestRepository addressBatchRequestRepository;

    @MockBean
    private EventService eventService;

    @MockBean
    private ApiKeyRepository apiKeyRepository;

    @MockBean
    private PnAddressManagerConfig pnAddressManagerConfig;

    @MockBean
    private PostelBatchService postelBatchService;

    @Test
    void normalizeAddressAsync() throws JsonProcessingException {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId("correlationId");
        List<NormalizeResult> normalize = new ArrayList<>();
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
        when(addressUtils.normalizeAddresses(any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        when(apiKeyRepository.findById(any())).thenReturn(Mono.just(apiKeyModel));
        StepVerifier.create(normalizeAddressService.normalizeAddress("xApiKey", "cxId", normalizeItemsRequest))
                .expectError().verify();
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
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        when(apiKeyRepository.findById(any())).thenReturn(Mono.just(apiKeyModel));
        StepVerifier.create(normalizeAddressService.normalizeAddress("xApiKey", "cxId", normalizeItemsRequest))
                .expectError().verify();
    }
}