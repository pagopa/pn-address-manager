package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchSendStatus;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.address.manager.constant.BatchRequestConstant.*;

@Component
public class AddressBatchRequestRepositoryImpl implements AddressBatchRequestRepository {

    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final DynamoDbAsyncTable<BatchRequest> table;

    private static final String STATUS_ALIAS = "#status";
    private static final String STATUS_PLACEHOLDER = ":status";
    private static final String STATUS_EQ = STATUS_ALIAS + " = " + STATUS_PLACEHOLDER;

    private static final String LAST_RESERVED_ALIAS = "#lastReserved";
    private static final String LAST_RESERVED_PLACEHOLDER = ":lastReserved";
    private static final String LAST_RESERVED_EQ = LAST_RESERVED_ALIAS + " = " + LAST_RESERVED_PLACEHOLDER;

    public AddressBatchRequestRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                             PnAddressManagerConfig pnAddressManagerConfig) {
        this.table = dynamoDbEnhancedAsyncClient.table(pnAddressManagerConfig.getDao().getBatchRequestTableName(), TableSchema.fromClass(BatchRequest.class));
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    @Override
    public Mono<BatchRequest> update(BatchRequest batchRequest) {
        return Mono.fromFuture(table.updateItem(batchRequest));
    }

    @Override
    public Mono<BatchRequest> create(BatchRequest batchRequest) {
        return Mono.fromFuture(table.putItem(batchRequest)).thenReturn(batchRequest);
    }

    @Override
    public Mono<Page<BatchRequest>> getBatchRequestByNotBatchId(Map<String, AttributeValue> lastKey, int limit) {
        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NO_BATCH_ID.getValue())))
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();
        return Mono.from(table.index(GSI_BL).query(queryEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestByBatchIdAndStatus(String batchId, BatchStatus status) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(STATUS_ALIAS, COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(STATUS_PLACEHOLDER, AttributeValue.builder().s(status.getValue()).build());

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .filterExpression(expressionBuilder(STATUS_EQ, expressionValues, expressionNames))
                .queryConditional(QueryConditional.keyEqualTo(keyBuilder(batchId)))
                .build();

        return Flux.from(table.index(GSI_BL).query(queryEnhancedRequest).flatMapIterable(Page::items))
                .collectList();
    }

    @Override
    public Mono<BatchRequest> setNewBatchIdToBatchRequest(BatchRequest batchRequest) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#batchId", COL_BATCH_ID);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":batchId", AttributeValue.builder().s(BatchStatus.NO_BATCH_ID.getValue()).build());

        String expression = "#batchId = :batchId";
        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<BatchRequest> setNewReservationIdToBatchRequest(BatchRequest batchRequest) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId", COL_RESERVATION_ID);
        expressionNames.put("#sendStatus", COL_SEND_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":sendStatus", AttributeValue.builder().s(BatchSendStatus.NOT_SENT.getValue()).build());
        expressionValues.put(":zero", AttributeValue.builder().n("0").build());

        String expression = "(attribute_not_exists(#reservationId) OR size(#reservationId) = :zero) AND #sendStatus = :sendStatus";
        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<BatchRequest> resetBatchRequestForRecovery(BatchRequest batchRequest) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(LAST_RESERVED_ALIAS, COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        AttributeValue lastReserved = AttributeValue.builder()
                .s(batchRequest.getLastReserved() != null ? batchRequest.getLastReserved().toString() : "")
                .build();
        expressionValues.put(LAST_RESERVED_PLACEHOLDER, lastReserved);

        String expression = LAST_RESERVED_EQ + " OR attribute_not_exists(" + LAST_RESERVED_ALIAS + ")";
        UpdateItemEnhancedRequest<BatchRequest> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchRequest.class)
                .item(batchRequest)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<List<BatchRequest>> getBatchRequestToRecovery() {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#retry", COL_RETRY);
        expressionNames.put(LAST_RESERVED_ALIAS, COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":retry", AttributeValue.builder().n(Integer.toString(pnAddressManagerConfig.getNormalizer().getBatchRequest().getMaxRetry())).build());
        expressionValues.put(LAST_RESERVED_PLACEHOLDER, AttributeValue.builder()
                .s(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(pnAddressManagerConfig.getNormalizer().getBatchRequest().getRecoveryAfter()).toString())
                .build());

        String expression = "#retry < :retry AND (:lastReserved > #lastReserved OR attribute_not_exists(#lastReserved))";

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.TAKEN_CHARGE.getValue()));

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Flux.from(table.index(GSI_S).query(queryEnhancedRequest).flatMapIterable(Page::items))
                .collectList();
    }

    @Override
    public Mono<Page<BatchRequest>> getBatchRequestToSend(Map<String, AttributeValue> lastKey, int limit) {
        Key key = Key.builder()
                .partitionValue(BatchSendStatus.NOT_SENT.getValue())
                .sortValue(AttributeValue.builder()
                        .s(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(pnAddressManagerConfig.getNormalizer().getBatchRequest().getRecoveryAfter()).toString())
                        .build())
                .build();

        QueryConditional queryConditional = QueryConditional.sortLessThan(key);

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_SSL).query(queryEnhancedRequest));
    }

    private Key keyBuilder(String key) {
        return Key.builder().partitionValue(key).build();
    }

    private Expression expressionBuilder(String expression, Map<String, AttributeValue> expressionValues, Map<String, String> expressionNames) {
        Expression.Builder expressionBuilder = Expression.builder();
        if (expression != null) {
            expressionBuilder.expression(expression);
        }
        if (expressionValues != null) {
            expressionBuilder.expressionValues(expressionValues);
        }
        if (expressionNames != null) {
            expressionBuilder.expressionNames(expressionNames);
        }
        return expressionBuilder.build();
    }
}