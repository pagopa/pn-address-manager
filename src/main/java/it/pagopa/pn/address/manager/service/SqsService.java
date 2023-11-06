package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.address.manager.model.PostelCallbackSqsDto;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@CustomLog
@Component
public class SqsService {

    private static final String PUSHING_MESSAGE = "pushing message for clientId: [{}] with correlationId: {}";
    private static final String INSERTING_MSG_WITH_DATA = "Inserting data {} in SQS {}";
    private static final String INSERTING_MSG_WITHOUT_DATA = "Inserted data in SQS {}";

    private final SqsClient sqsClient;
    private final AddressUtils addressUtils;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public SqsService(SqsClient sqsClient,
                      AddressUtils addressUtils, PnAddressManagerConfig pnAddressManagerConfig) {
        this.sqsClient = sqsClient;
        this.addressUtils = addressUtils;
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    public Mono<Void> sendListToDlqQueue(List<BatchRequest> batchRequests) {
        return Flux.fromIterable(batchRequests)
                .map(this::sendToDlqQueue)
                .then();
    }

    public Mono<Void> sendToDlqQueue(BatchRequest batchRequest) {
        InternalCodeSqsDto internalCodeSqsDto = toInternalCodeSqsDto(batchRequest);
        return pushToInputDlqQueue(internalCodeSqsDto, batchRequest.getClientId())
                .onErrorResume(throwable -> {
                    log.error("error during push message for correlationId: [{}] to DLQ: {}", batchRequest.getCorrelationId(), throwable.getMessage(), throwable);
                    return Mono.empty();
                })
                .then();
    }

    private InternalCodeSqsDto toInternalCodeSqsDto(BatchRequest batchRequest) {
        List<NormalizeRequest> requestList = addressUtils.getNormalizeRequestFromBatchRequest(batchRequest);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setRequestItems(requestList);
        normalizeItemsRequest.setCorrelationId(batchRequest.getCorrelationId());

        return InternalCodeSqsDto.builder()
                .xApiKey(batchRequest.getXApiKey())
                .normalizeItemsRequest(normalizeItemsRequest)
                .pnAddressManagerCxId(batchRequest.getCxId())
                .build();
    }

    public Mono<SendMessageResponse> pushToInputQueue(InternalCodeSqsDto msg, String pnAddressManagerCxId, String eventType) {
        log.info(PUSHING_MESSAGE, pnAddressManagerCxId, msg.getNormalizeItemsRequest().getCorrelationId());
        log.debug(INSERTING_MSG_WITH_DATA, msg, pnAddressManagerConfig.getSqs().getInputQueueName());
        log.info(INSERTING_MSG_WITHOUT_DATA, pnAddressManagerConfig.getSqs().getInputQueueName());
        return push(addressUtils.toJson(msg), pnAddressManagerCxId, pnAddressManagerConfig.getSqs().getInputQueueName(),eventType, msg.getNormalizeItemsRequest().getCorrelationId());
    }

    public Mono<SendMessageResponse> pushToInputQueue(PostelCallbackSqsDto msg, String eventType) {
        log.info("pushing message from Postel with BatchId: [{}] and OutputFileKey: [{}]", msg.getRequestId(), msg.getOutputFileKey());
        log.debug(INSERTING_MSG_WITH_DATA, msg, pnAddressManagerConfig.getSqs().getInputQueueName());
        log.info(INSERTING_MSG_WITHOUT_DATA, pnAddressManagerConfig.getSqs().getInputQueueName());
        return push(addressUtils.toJson(msg), "postel", pnAddressManagerConfig.getSqs().getInputQueueName(),eventType, msg.getRequestId());
    }

    public Mono<SendMessageResponse> pushToInputDlqQueue(InternalCodeSqsDto msg, String pnAddressManagerCxId) {
        log.info(PUSHING_MESSAGE, pnAddressManagerCxId, msg.getNormalizeItemsRequest().getCorrelationId());
        log.debug(INSERTING_MSG_WITH_DATA, msg, pnAddressManagerConfig.getSqs().getInputDlqQueueName());
        log.info(INSERTING_MSG_WITHOUT_DATA, pnAddressManagerConfig.getSqs().getInputDlqQueueName());
        return push(addressUtils.toJson(msg), pnAddressManagerCxId, pnAddressManagerConfig.getSqs().getInputDlqQueueName(), "AM_NORMALIZE_INPUT", msg.getNormalizeItemsRequest().getCorrelationId());
    }

    public Mono<SendMessageResponse> push(String msg, String pnAddressManagerCxId, String queueName, String eventType, String correlationId) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageAttributes(buildMessageAttributeMap(pnAddressManagerCxId, eventType, correlationId))
                .messageBody(msg)
                .build();

        return Mono.just(sqsClient.sendMessage(sendMsgRequest));
    }

    private Map<String, MessageAttributeValue> buildMessageAttributeMap(String pnAddressManagerCxId, String eventType, String correlationId) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        if (StringUtils.hasText(pnAddressManagerCxId)) {
            attributes.put("clientId", MessageAttributeValue.builder().stringValue(pnAddressManagerCxId).dataType("String").build());
        }
        attributes.put("eventType", MessageAttributeValue.builder().stringValue(eventType).dataType("String").build());
        attributes.put("requestId", MessageAttributeValue.builder().stringValue(correlationId).dataType("String").build());
        return attributes;
    }
}
