package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.address.manager.model.PostelCallbackSqsDto;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SqsServiceTest {

    SqsService sqsService;

    @MockBean SqsClient sqsClient;
    @MockBean AddressUtils addressUtils;
    @MockBean PnAddressManagerConfig pnAddressManagerConfig;

    @Test
    void sendListToDlqQueue(){
        pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Sqs sqs = new PnAddressManagerConfig.Sqs();
        sqs.setInputDlqQueueName("input");
        sqs.setInputQueueName("input");
        pnAddressManagerConfig.setSqs(sqs);
        sqsService = new SqsService(sqsClient, addressUtils, pnAddressManagerConfig);

        PnRequest pnRequest = getBatchRequest();
        when(addressUtils.toObject("yourAddresses", NormalizeItemsRequest.class)).thenReturn(new NormalizeItemsRequest());
        when(sqsClient.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(GetQueueUrlResponse.builder().queueUrl("url").build());
        when(sqsClient.sendMessage((SendMessageRequest) any())).thenReturn(SendMessageResponse.builder().build());
        StepVerifier.create(sqsService.sendToDlqQueue(pnRequest)).expectNextCount(0).verifyComplete();
        InternalCodeSqsDto internalCodeSqsDto = mock(InternalCodeSqsDto.class);
        NormalizeItemsRequest normalizeItemsRequest = mock(NormalizeItemsRequest.class);

        when(internalCodeSqsDto.getNormalizeItemsRequest()).thenReturn(normalizeItemsRequest);
        when(normalizeItemsRequest.getCorrelationId()).thenReturn("id");
        StepVerifier.create(sqsService.pushToInputQueue(internalCodeSqsDto,"cxId")).expectNext(SendMessageResponse.builder().build()).verifyComplete();

        PostelCallbackSqsDto postelCallbackSqsDto = mock(PostelCallbackSqsDto.class);
        StepVerifier.create(sqsService.pushToCallbackQueue(postelCallbackSqsDto)).expectNext(SendMessageResponse.builder().build()).verifyComplete();
        StepVerifier.create(sqsService.pushToCallbackDlqQueue(postelCallbackSqsDto)).expectNext(SendMessageResponse.builder().build()).verifyComplete();
    }


    PnRequest getBatchRequest(){
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("yourCorrelationId");
        pnRequest.setAddresses("yourAddresses");
        pnRequest.setBatchId("NO_BATCH_ID");
        pnRequest.setRetry(1);
        pnRequest.setTtl(3600L); // Your TTL value in seconds
        pnRequest.setClientId("yourClientId");
        pnRequest.setStatus(BatchStatus.NO_BATCH_ID.toString());
        pnRequest.setLastReserved(LocalDateTime.now()); // Your LocalDateTime value
        pnRequest.setCreatedAt(LocalDateTime.now()); // Your LocalDateTime value
        pnRequest.setSendStatus("yourSendStatus");
        pnRequest.setMessage("yourMessage");
        pnRequest.setXApiKey("yourXApiKey");
        pnRequest.setCxId("yourCxId");
        pnRequest.setAwsMessageId("yourAwsMessageId");
        return pnRequest;
    }



}