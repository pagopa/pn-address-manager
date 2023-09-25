package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.model.InternalCodeSqsDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_JSON_PROCESSING;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_JSON_PROCESSING;


@Slf4j
@Component
public class SqsService {

    private static final String PUSHING_MESSAGE = "pushing message for clientId: [{}] with correlationId: {}";
    private static final String INSERTING_MSG_WITH_DATA = "Inserting data {} in SQS {}";
    private static final String INSERTING_MSG_WITHOUT_DATA = "Inserted data in SQS {}";

    private final SqsClient sqsClient;
    private final ObjectMapper mapper;
    private final String inputQueueName;
    private final String inputDlqQueueName;

    public SqsService(@Value("${pn.address.manager.sqs.input.queue.name}") String inputQueueName,
                      @Value("${pn.address.manager.sqs.input.dlq.queue.name}") String inputDlqQueueName,
                      SqsClient sqsClient,
                      ObjectMapper mapper) {
        this.sqsClient = sqsClient;
        this.mapper = mapper;
        this.inputQueueName = inputQueueName;
        this.inputDlqQueueName = inputDlqQueueName;
    }

    public Mono<SendMessageResponse> pushToInputQueue(InternalCodeSqsDto msg, String pnAddressManagerCxId) {
        log.info(PUSHING_MESSAGE, pnAddressManagerCxId, msg.getNormalizeItemsRequest().getCorrelationId());
        log.debug(INSERTING_MSG_WITH_DATA, msg, inputQueueName);
        log.info(INSERTING_MSG_WITHOUT_DATA, inputQueueName);
        return push(toJson(msg), pnAddressManagerCxId, inputQueueName, "AM_NORMALIZE_INPUT");
    }

    public Mono<SendMessageResponse> pushToInputDlqQueue(InternalCodeSqsDto msg, String pnAddressManagerCxId) {
        log.info(PUSHING_MESSAGE, pnAddressManagerCxId, msg.getNormalizeItemsRequest().getCorrelationId());
        log.debug(INSERTING_MSG_WITH_DATA, msg, inputDlqQueueName);
        log.info(INSERTING_MSG_WITHOUT_DATA, inputDlqQueueName);
        return push(toJson(msg), pnAddressManagerCxId, inputDlqQueueName, "AM_NORMALIZE_DLQ");
    }

    public Mono<SendMessageResponse> push(String msg, String pnAddressManagerCxId, String queueName, String eventType) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageAttributes(buildMessageAttributeMap(pnAddressManagerCxId, eventType)) // TODO: passare anche correlationID
                .messageBody(msg)
                .build();

        return Mono.just(sqsClient.sendMessage(sendMsgRequest));
    }

    private Map<String, MessageAttributeValue> buildMessageAttributeMap(String pnAddressManagerCxId, String eventType) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        if (StringUtils.hasText(pnAddressManagerCxId)) {
            attributes.put("clientId", MessageAttributeValue.builder().stringValue(pnAddressManagerCxId).dataType("String").build());
        }
        attributes.put("eventType", MessageAttributeValue.builder().stringValue(eventType).dataType("String").build());
        return attributes;
    }

    protected String toJson(Object codeSqsDto) {
        try {
            return mapper.writeValueAsString(codeSqsDto);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_JSON_PROCESSING, ERROR_CODE_JSON_PROCESSING, e);
        }
    }

    protected <T>T toObject(String msg, Class<T> targetClass) {
        try {
            return mapper.readValue(msg, targetClass);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_JSON_PROCESSING, ERROR_CODE_JSON_PROCESSING, e);
        }
    }
}
