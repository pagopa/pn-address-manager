package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.BatchAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static it.pagopa.pn.address.manager.constant.BatchAddressConstant.GSI_BL;
import static it.pagopa.pn.address.manager.utils.ExpressionUtils.*;

@Component
public class BatchAddressRepositoryImpl implements BatchAddressRepository {

    private final DynamoDbAsyncTable<BatchAddress> table;

    private final int maxRetry;
    private final int retryAfter;


    public BatchAddressRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                      @Value("${pn.address.manager.batch.max-retry}") int maxRetry,
                                      @Value("${pn.address.manager.batch.recovery.after}") int retryAfter) {
        this.table = dynamoDbEnhancedAsyncClient.table("pn-batchAddress", TableSchema.fromClass(BatchAddress.class));
        this.maxRetry = maxRetry;
        this.retryAfter = retryAfter;
    }

    @Override
    public Mono<BatchAddress> create(BatchAddress batchAddress) {
        return Mono.fromFuture(table.putItem(batchAddress)).thenReturn(batchAddress);
    }

    @Override
    public Mono<Page<BatchAddress>> getBatchAddressByNotBatchId(Map<String, AttributeValue> lastKey, int limit) {
        QueryEnhancedRequest.Builder queryEnhancedRequestBuilder = QueryEnhancedRequest.builder()
                .filterExpression(expressionBuilderNoBatchId())
                .queryConditional(conditionalBuilderNoBatchId())
                .limit(limit);

        if (!CollectionUtils.isEmpty(lastKey)) {
            queryEnhancedRequestBuilder.exclusiveStartKey(lastKey);
        }

        QueryEnhancedRequest queryEnhancedRequest = queryEnhancedRequestBuilder.build();

        return Mono.from(table.index(GSI_BL).query(queryEnhancedRequest));
    }

    @Override
    public Mono<BatchAddress> setNewBatchIdToBatchAddress(BatchAddress batchAddress){
        UpdateItemEnhancedRequest<BatchAddress> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(BatchAddress.class)
                .item(batchAddress)
                .conditionExpression(expressionBuilderSetNewBatchId())
                .build();

        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest));
    }

    @Override
    public Mono<BatchAddress> update(BatchAddress batchAddress) {
        return Mono.fromFuture(table.updateItem(batchAddress));
    }


}
