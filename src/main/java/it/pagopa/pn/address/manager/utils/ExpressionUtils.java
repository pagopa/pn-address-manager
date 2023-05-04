package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.address.manager.constant.BatchAddressConstant.COL_BATCH_ID;
import static it.pagopa.pn.address.manager.constant.BatchAddressConstant.COL_STATUS;
import static it.pagopa.pn.address.manager.constant.ExpressionConstant.*;

@Component
@Slf4j
public class ExpressionUtils {

    public static Expression expressionBuilderNoBatchId(){
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(STATUS_ALIAS, COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(STATUS_PLACEHOLDER, AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

        return expressionBuilder(STATUS_EQ, expressionValues, expressionNames);
    }

    public static QueryConditional conditionalBuilderNoBatchId(){
        return QueryConditional.keyEqualTo(keyBuilder(BatchStatus.NO_BATCH_ID.getValue()));
    }

    public static Expression expressionBuilderSetNewBatchId(){
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put(BATCH_ALIAS, COL_BATCH_ID);
        expressionNames.put(STATUS_ALIAS, COL_STATUS);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(BATCH_PLACEHOLDER, AttributeValue.builder().s(BatchStatus.NO_BATCH_ID.getValue()).build());
        expressionValues.put(STATUS_PLACEHOLDER, AttributeValue.builder().s(BatchStatus.NOT_WORKED.getValue()).build());

        String expression = BATCH_EQ + " AND " + STATUS_EQ;

        return expressionBuilder(expression, expressionValues, expressionNames);
    }

    private static Key keyBuilder(String key) {
        return Key.builder().partitionValue(key).build();
    }

    private static Expression expressionBuilder(String expression, Map<String, AttributeValue> expressionValues, Map<String, String> expressionNames) {
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
