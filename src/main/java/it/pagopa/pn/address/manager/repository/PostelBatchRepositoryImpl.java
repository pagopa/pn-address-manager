package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.address.manager.constant.PostelBatchConstant.*;

@Component
public class PostelBatchRepositoryImpl implements PostelBatchRepository {

    private final DynamoDbAsyncTable<PostelBatch> table;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public PostelBatchRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                     PnAddressManagerConfig pnAddressManagerConfig) {
        this.table = dynamoDbEnhancedAsyncClient.table("pn-address-manager-postel-batch", TableSchema.fromClass(PostelBatch.class));
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    @Override
    public Mono<PostelBatch> create(PostelBatch postelBatch) {
        return Mono.fromFuture(table.putItem(postelBatch)).thenReturn(postelBatch);
    }

    @Override
    public Mono<PostelBatch> findByFileKey(String fileKey) {
        return Mono.fromFuture(table.getItem(r -> r.key(keyBuilder(fileKey))));
    }


@Override
    public Mono<PostelBatch> update(PostelBatch postelBatch) {
        return Mono.fromFuture(table.updateItem(postelBatch));
    }

    @Override
    public Mono<Page<PostelBatch>> getPostelBatchWithoutReservationIdAndStatusNotWorked(Map<String, AttributeValue> lastKey, int limit) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId", COL_RESERVATION_ID);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":zero", AttributeValue.builder().n("0").build());

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NOT_WORKED.getValue()));

        String expression = "attribute_not_exists(#reservationId) OR size(#reservationId) = :zero";
        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_S).query(queryEnhancedRequest));
    }

    @Override
    public Mono<PostelBatch> setNewReservationIdToPostelBatch(PostelBatch postelBatch) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#reservationId", COL_RESERVATION_ID);
        expressionNames.put("#status", COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":status", AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());
        expressionValues.put(":zero", AttributeValue.builder().n("0").build());

        String expression = "(attribute_not_exists(#reservationId) OR size(#reservationId) = :zero) AND #status = :status";
        UpdateItemEnhancedRequest<PostelBatch> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(PostelBatch.class)
                .item(postelBatch)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<PostelBatch> resetPostelBatchForRecovery(PostelBatch postelBatch) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#lastReserved", COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        AttributeValue lastReserved = AttributeValue.builder()
                .s(postelBatch.getLastReserved() != null ? postelBatch.getLastReserved().toString() : "")
                .build();
        expressionValues.put(":lastReserved", lastReserved);

        String expression = "#lastReserved = :lastReserved OR attribute_not_exists(#lastReserved)";
        UpdateItemEnhancedRequest<PostelBatch> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(PostelBatch.class)
                .item(postelBatch)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<List<PostelBatch>> getPostelBatchToRecover() {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#retry", COL_RETRY);
        expressionNames.put("#lastReserved", COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":retry", AttributeValue.builder().n(Integer.toString(pnAddressManagerConfig.getPostel().getBatchSecondaryTableMaxRetry())).build());
        expressionValues.put(":lastReserved", AttributeValue.builder()
                .s(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(pnAddressManagerConfig.getPostel().getBatchSecondaryTableRecoveryAfter()).toString())
                .build());

        String expression = "#retry < :retry AND (:lastReserved > #lastReserved OR attribute_not_exists(#lastReserved))";

        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder(BatchStatus.WORKING.getValue()));

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Flux.from(table.index(GSI_S).query(queryEnhancedRequest).flatMapIterable(Page::items))
                .collectList();
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
