package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import lombok.CustomLog;
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

import static it.pagopa.pn.address.manager.constant.PnRequestConstant.*;
import static it.pagopa.pn.address.manager.constant.NormalizzatoreBatchConstant.GSI_SWT;
@Component
@CustomLog
public class PostelBatchRepositoryImpl implements PostelBatchRepository {

    private final DynamoDbAsyncTable<NormalizzatoreBatch> table;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    private static final String LAST_RESERVED_ALIAS = "#lastReserved";
    private static final String LAST_RESERVED_PLACEHOLDER = ":lastReserved";
    private static final String LAST_RESERVED_EQ = LAST_RESERVED_ALIAS + " = " + LAST_RESERVED_PLACEHOLDER;

    public PostelBatchRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                     PnAddressManagerConfig pnAddressManagerConfig) {
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.table = dynamoDbEnhancedAsyncClient.table(pnAddressManagerConfig.getDao().getPostelBatchTableName(), TableSchema.fromClass(NormalizzatoreBatch.class));
    }

    @Override
    public Mono<NormalizzatoreBatch> create(NormalizzatoreBatch normalizzatoreBatch) {
        log.debug("Inserting data {} in DynamoDB table {}", normalizzatoreBatch, table);
        return Mono.fromFuture(table.putItem(normalizzatoreBatch))
                .doOnNext(unused -> log.info("Inserted data in DynamoDB table {}", table))
                .thenReturn(normalizzatoreBatch);
    }

    @Override
    public Mono<NormalizzatoreBatch> findByBatchId(String batchId) {
        return Mono.fromFuture(table.getItem(r -> r.key(keyBuilder(batchId))));
    }

    @Override
    public Mono<NormalizzatoreBatch> update(NormalizzatoreBatch normalizzatoreBatch) {
        return Mono.fromFuture(table.updateItem(normalizzatoreBatch));
    }

    private Key keyBuilder(String key) {
        return Key.builder().partitionValue(key).build();
    }

    @Override
    public Mono<NormalizzatoreBatch> resetPostelBatchForRecovery(NormalizzatoreBatch normalizzatoreBatch) {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(LAST_RESERVED_ALIAS, COL_LAST_RESERVED);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        AttributeValue lastReserved = AttributeValue.builder()
                .s(normalizzatoreBatch.getLastReserved() != null ? normalizzatoreBatch.getLastReserved().toInstant(ZoneOffset.UTC).toString() : "")
                .build();
        expressionValues.put(LAST_RESERVED_PLACEHOLDER, lastReserved);

        String expression = LAST_RESERVED_EQ + " OR attribute_not_exists(" + LAST_RESERVED_ALIAS + ")";
        UpdateItemEnhancedRequest<NormalizzatoreBatch> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(NormalizzatoreBatch.class)
                .item(normalizzatoreBatch)
                .conditionExpression(expressionBuilder(expression, expressionValues, expressionNames))
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<List<NormalizzatoreBatch>> getPostelBatchToRecover() {
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
    public Mono<Page<NormalizzatoreBatch>> getPostelBatchToClean(Map<String, AttributeValue> lastKey) {
        Key key = Key.builder()
                .partitionValue(BatchStatus.WORKING.getValue())
                .sortValue(AttributeValue.builder()
                        .s(LocalDateTime.now(ZoneOffset.UTC).toString())
                        .build())
                .build();

        QueryConditional queryConditional = QueryConditional.sortLessThan(key);

        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .limit(pnAddressManagerConfig.getNormalizer().getBatchRequest().getQueryMaxSize());

        if(!CollectionUtils.isEmpty(lastKey)){
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_SWT).query(queryEnhancedRequest));
    }

    @Override
    public Mono<NormalizzatoreBatch> deleteItem(String batchId) {
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
