package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.config.SchedulerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnNormalizeRequestEvent;
import it.pagopa.pn.address.manager.middleware.queue.consumer.event.PnPostelCallbackEvent;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NormalizeAddressService.class, SchedulerConfig.class})
@TestPropertySource(properties = "pn.address.manager.flag.csv=true")
@TestPropertySource(properties = "pn.address.manager.batch.ttl=1000")
class NormalizeAddressServiceTest {

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
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        List<NormalizeResult> normalize = new ArrayList<>();
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
        when(addressUtils.normalizeAddresses(any(), any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        when(apiKeyRepository.findById(any())).thenReturn(Mono.just(apiKeyModel));
        StepVerifier.create(normalizeAddressService.normalizeAddress("xApiKey", "cxId", normalizeItemsRequest))
                .expectError().verify();
    }
    @Test
    void checkFieldsLengthError(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setAddressLengthValidation(1);
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        NormalizeRequest item = new NormalizeRequest();
        item.setId("id");
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setCity("Roma");
        analogAddress.setCap("00178");
        analogAddress.setPr("RM");
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey("id");
        apiKeyModel.setCxId("id");
        when(apiKeyRepository.findById(any())).thenReturn(Mono.just(apiKeyModel));
        item.setAddress(analogAddress);
        normalizeItemsRequest.setRequestItems(List.of(item));
        StepVerifier.create(normalizeAddressService.normalizeAddress("id", "cxId", normalizeItemsRequest))
                .expectError().verify();
    }
    @Test
    void checkFieldsLength(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setAddressLengthValidation(1);
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        NormalizeRequest item = new NormalizeRequest();
        item.setId("id");
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey("id");
        apiKeyModel.setCxId("id");
        when(apiKeyRepository.findById(any())).thenReturn(Mono.just(apiKeyModel));
        item.setAddress(analogAddress);
        normalizeItemsRequest.setRequestItems(List.of(item));
        StepVerifier.create(normalizeAddressService.normalizeAddress("id", "cxId", normalizeItemsRequest))
                .expectError().verify();
    }

    @Test
    void normalizeAddressAsync1() throws JsonProcessingException {
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        List<NormalizeResult> normalize = new ArrayList<>();
        pnAddressManagerConfig.setAddressLengthValidation(1);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
        when(addressUtils.normalizeAddresses(any(), any())).thenReturn(normalize);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
        when(addressUtils.normalizeAddresses(any(), any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey("id");
        apiKeyModel.setCxId("id");
        when(apiKeyRepository.findById(any())).thenReturn(Mono.just(apiKeyModel));
        when(sqsService.pushToInputQueue(any(),any())).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        StepVerifier.create(normalizeAddressService.normalizeAddress("id", "id", normalizeItemsRequest))
                .expectError().verify();
    }

    @Test
    void normalizeAddressAsyncError() throws JsonProcessingException {
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        List<NormalizeResult> normalize = new ArrayList<>();
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        when(addressUtils.normalizeAddresses(any(), any())).thenReturn(normalize);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        when(apiKeyRepository.findById(any())).thenReturn(Mono.just(apiKeyModel));
        StepVerifier.create(normalizeAddressService.normalizeAddress("xApiKey", "cxId", normalizeItemsRequest))
                .expectError().verify();
    }

    @Test
    void handleRequest(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setFlagCsv(true);
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        when(addressUtils.toJson(any())).thenReturn("json");
        NormalizeItemsRequest request = new NormalizeItemsRequest();
        request.setCorrelationId("corrId");
        NormalizeRequest item = new NormalizeRequest();
        item.setId("id");
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setCity("Roma");
        analogAddress.setCap("00178");
        analogAddress.setPr("RM");
        item.setAddress(analogAddress);
        request.setRequestItems(List.of(item));
        StepVerifier.create(normalizeAddressService.handleRequest(PnNormalizeRequestEvent.Payload.builder()
                .normalizeItemsRequest(request).build())).expectComplete();
    }

    @Test
    void handleRequest1(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setFlagCsv(false);
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        when(addressUtils.normalizeRequestToResult(any())).thenReturn(new NormalizeItemsResult());
        when(addressUtils.toJson(any())).thenReturn("json");
        when(addressUtils.createNewStartBatchRequest()).thenReturn(new BatchRequest());
        when(addressBatchRequestRepository.create(any())).thenReturn(Mono.just(new BatchRequest()));
        StepVerifier.create(normalizeAddressService.handleRequest(PnNormalizeRequestEvent.Payload.builder().pnAddressManagerCxId("cxId").normalizeItemsRequest(new NormalizeItemsRequest()).build())).expectNextCount(0).verifyComplete();
    }

    @Test
    void handlePostelCallback(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setFlagCsv(false);
        normalizeAddressService = new NormalizeAddressService(addressUtils,eventService,sqsService,addressBatchRequestRepository,apiKeyRepository,pnAddressManagerConfig,postelBatchService);
        when(postelBatchService.findPostelBatch(any())).thenReturn(Mono.just(new PostelBatch()));
        when(postelBatchService.getResponse(any(),any())).thenReturn(Mono.empty());
        StepVerifier.create(normalizeAddressService.handlePostelCallback(PnPostelCallbackEvent.Payload.builder().build())).expectNextCount(0).verifyComplete();
    }
}