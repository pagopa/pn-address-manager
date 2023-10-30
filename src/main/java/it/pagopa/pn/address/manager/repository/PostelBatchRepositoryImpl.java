package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import org.springframework.stereotype.Component;
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

import static it.pagopa.pn.address.manager.constant.BatchRequestConstant.*;
import static it.pagopa.pn.address.manager.constant.PostelBatchConstant.GSI_SWT;
@Component
public class PostelBatchRepositoryImpl implements PostelBatchRepository {

    private final DynamoDbAsyncTable<PostelBatch> table;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    private static final String LAST_RESERVED_ALIAS = "#lastReserved";
    private static final String LAST_RESERVED_PLACEHOLDER = ":lastReserved";
    private static final String LAST_RESERVED_EQ = LAST_RESERVED_ALIAS + " = " + LAST_RESERVED_PLACEHOLDER;

    public PostelBatchRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                     PnAddressManagerConfig pnAddressManagerConfig) {
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.table = dynamoDbEnhancedAsyncClient.table(pnAddressManagerConfig.getDao().getPostelBatchTableName(), TableSchema.fromClass(PostelBatch.class));
    }

    @Override
    public Mono<PostelBatch> create(PostelBatch postelBatch) {
        return Mono.fromFuture(table.putItem(postelBatch)).thenReturn(postelBatch);
    }

    @Override
    public Mono<PostelBatch> findByBatchId(String batchId) {
        return Mono.fromFuture(table.getItem(r -> r.key(keyBuilder(batchId))));
    }

    @Override
    public Mono<PostelBatch> update(PostelBatch postelBatch) {
        return Mono.fromFuture(table.updateItem(postelBatch));
    }

    private Key keyBuilder(String key) {
        return Key.builder().partitionValue(key).build();
    }

    @Override
    public Mono<PostelBatch> resetPostelBatchForRecovery(PostelBatch postelBatch) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(LAST_RESERVED_ALIAS, COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        AttributeValue lastReserved = AttributeValue.builder()
                .s(postelBatch.getLastReserved() != null ? postelBatch.getLastReserved().toString() : "")
                .build();
        expressionValues.put(LAST_RESERVED_PLACEHOLDER, lastReserved);

        String expression = LAST_RESERVED_EQ + " OR attribute_not_exists(" + LAST_RESERVED_ALIAS + ")";
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
        expressionNames.put(LAST_RESERVED_ALIAS, COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":retry", AttributeValue.builder().n(Integer.toString(pnAddressManagerConfig.getNormalizer().getPostel().getMaxRetry())).build());
        expressionValues.put(LAST_RESERVED_PLACEHOLDER, AttributeValue.builder()
                .s(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(pnAddressManagerConfig.getNormalizer().getPostel().getRecoveryAfter()).toString())
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
    public Mono<Page<PostelBatch>> getPostelBatchToClean() {
        Key key = Key.builder()
                .partitionValue(BatchStatus.WORKING.getValue())
                .sortValue(AttributeValue.builder()
                        .s(LocalDateTime.now(ZoneOffset.UTC).toString())
                        .build())
                .build();

        QueryConditional queryConditional = QueryConditional.sortLessThan(key);

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional);

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_SWT).query(queryEnhancedRequest));
    }

    @Override
    public Mono<PostelBatch> deleteItem(String batchId) {
        return Mono.fromFuture(table.deleteItem(Key.builder().partitionValue(batchId).build()));
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
