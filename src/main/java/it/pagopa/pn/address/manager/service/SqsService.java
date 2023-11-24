package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.address.manager.model.PostelCallbackSqsDto;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.AM_NORMALIZE_INPUT_EVENTTYPE;
import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.AM_POSTEL_CALLBACK_EVENTTYPE;


@CustomLog
@Component
@RequiredArgsConstructor
public class SqsService {

    private static final String PUSHING_MESSAGE = "pushing message for clientId: [{}] with correlationId: {}";
    private static final String INSERTING_MSG_WITH_DATA = "Inserting data {} in SQS {}";
    private static final String INSERTING_MSG_WITHOUT_DATA = "Inserted data in SQS {}";

    private static final String STRING_DATA_TYPE = "String";

    private final SqsClient sqsClient;
    private final AddressUtils addressUtils;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public Mono<Void> sendToInputDlqQueue(PnRequest pnRequest) {
        InternalCodeSqsDto internalCodeSqsDto = toInternalCodeSqsDto(pnRequest);
        return pushToInputDlqQueue(internalCodeSqsDto, pnRequest.getClientId())
                .onErrorResume(throwable -> {
                    log.error("error during push message for correlationId: [{}] to DLQ: {}", pnRequest.getCorrelationId(), throwable.getMessage(), throwable);
                    return Mono.empty();
                })
                .then();
    }

    public Mono<Void> sendIfCallbackToDlqQueue(NormalizzatoreBatch normalizzatoreBatch) {
        //mando in DLQ solo se la chiamata che è andata in errore è quella della callback
        if(normalizzatoreBatch.getCallbackTimeStamp() != null) {
            PostelCallbackSqsDto postelCallbackSqsDto = toCallbackSqsDto(normalizzatoreBatch);
            return pushToCallbackDlqQueue(postelCallbackSqsDto)
                    .onErrorResume(throwable -> {
                        log.error("error during push message for batchId: [{}] to DLQ: {}", normalizzatoreBatch.getBatchId(), throwable.getMessage(), throwable);
                        return Mono.empty();
                    })
                    .then();
        }
        else {
            return Mono.empty();
        }

    }

    private PostelCallbackSqsDto toCallbackSqsDto(NormalizzatoreBatch normalizzatoreBatch) {
        return PostelCallbackSqsDto.builder()
                .requestId(normalizzatoreBatch.getBatchId())
                .outputFileKey(normalizzatoreBatch.getOutputFileKey())
                .error(normalizzatoreBatch.getError())
                .build();
    }

    private InternalCodeSqsDto toInternalCodeSqsDto(PnRequest pnRequest) {
        List<NormalizeRequest> requestList = addressUtils.getNormalizeRequestFromBatchRequest(pnRequest);
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setRequestItems(requestList);
        normalizeItemsRequest.setCorrelationId(pnRequest.getCorrelationId());

        return InternalCodeSqsDto.builder()
                .xApiKey(pnRequest.getXApiKey())
                .normalizeItemsRequest(normalizeItemsRequest)
                .pnAddressManagerCxId(pnRequest.getCxId())
                .build();
    }

    public Mono<SendMessageResponse> pushToInputQueue(InternalCodeSqsDto msg, String pnAddressManagerCxId) {
        log.info(PUSHING_MESSAGE, pnAddressManagerCxId, msg.getNormalizeItemsRequest().getCorrelationId());
        log.debug(INSERTING_MSG_WITH_DATA, msg, pnAddressManagerConfig.getSqs().getInputQueueName());
        log.info(INSERTING_MSG_WITHOUT_DATA, pnAddressManagerConfig.getSqs().getInputQueueName());
        return push(addressUtils.toJson(msg), pnAddressManagerCxId, pnAddressManagerConfig.getSqs().getInputQueueName(), AM_NORMALIZE_INPUT_EVENTTYPE, msg.getNormalizeItemsRequest().getCorrelationId());
    }

    public Mono<SendMessageResponse> pushToCallbackQueue(PostelCallbackSqsDto msg) {
        log.info("pushing message from Postel with BatchId: [{}] and OutputFileKey: [{}]", msg.getRequestId(), msg.getOutputFileKey());
        log.debug(INSERTING_MSG_WITH_DATA, msg, pnAddressManagerConfig.getSqs().getCallbackQueueName());
        log.info(INSERTING_MSG_WITHOUT_DATA, pnAddressManagerConfig.getSqs().getCallbackQueueName());
        return push(addressUtils.toJson(msg), null, pnAddressManagerConfig.getSqs().getCallbackQueueName(),AM_POSTEL_CALLBACK_EVENTTYPE, msg.getRequestId());
    }

    public Mono<SendMessageResponse> pushToInputDlqQueue(InternalCodeSqsDto msg, String pnAddressManagerCxId) {
        log.info(PUSHING_MESSAGE, pnAddressManagerCxId, msg.getNormalizeItemsRequest().getCorrelationId());
        log.debug(INSERTING_MSG_WITH_DATA, msg, pnAddressManagerConfig.getSqs().getInputDlqQueueName());
        log.info(INSERTING_MSG_WITHOUT_DATA, pnAddressManagerConfig.getSqs().getInputDlqQueueName());
        return push(addressUtils.toJson(msg), pnAddressManagerCxId, pnAddressManagerConfig.getSqs().getInputDlqQueueName(), AM_NORMALIZE_INPUT_EVENTTYPE, msg.getNormalizeItemsRequest().getCorrelationId());
    }

    public Mono<SendMessageResponse> pushToCallbackDlqQueue(PostelCallbackSqsDto msg) {
        log.info("pushing message from Postel with BatchId: [{}] and OutputFileKey: [{}]", msg.getRequestId(), msg.getOutputFileKey());
        log.debug(INSERTING_MSG_WITH_DATA, msg, pnAddressManagerConfig.getSqs().getCallbackDlqQueueName());
        log.info(INSERTING_MSG_WITHOUT_DATA, pnAddressManagerConfig.getSqs().getCallbackDlqQueueName());
        return push(addressUtils.toJson(msg), null, pnAddressManagerConfig.getSqs().getCallbackDlqQueueName(), AM_POSTEL_CALLBACK_EVENTTYPE, msg.getRequestId());
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
            attributes.put("clientId", MessageAttributeValue.builder().stringValue(pnAddressManagerCxId).dataType(STRING_DATA_TYPE).build());
        }
        attributes.put("eventType", MessageAttributeValue.builder().stringValue(eventType).dataType(STRING_DATA_TYPE).build());
        attributes.put("requestId", MessageAttributeValue.builder().stringValue(correlationId).dataType(STRING_DATA_TYPE).build());
        return attributes;
    }
}
