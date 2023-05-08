package it.pagopa.pn.address.manager.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.EqualToConditional;

class ExpressionUtilsTest {
    /**
     * Method under test: {@link ExpressionUtils#expressionBuilderNoBatchId()}
     */
    @Test
    void testExpressionBuilderNoBatchId() {
        Expression actualExpressionBuilderNoBatchIdResult = ExpressionUtils.expressionBuilderNoBatchId();
        assertEquals("#status = :status", actualExpressionBuilderNoBatchIdResult.expression());
        assertEquals(1, actualExpressionBuilderNoBatchIdResult.expressionValues().size());
        assertEquals(1, actualExpressionBuilderNoBatchIdResult.expressionNames().size());
    }

    /**
     * Method under test: {@link ExpressionUtils#conditionalBuilderNoBatchId()}
     */
    @Test
    void testConditionalBuilderNoBatchId() {
        assertTrue(ExpressionUtils.conditionalBuilderNoBatchId() instanceof EqualToConditional);
    }

    /**
     * Method under test: {@link ExpressionUtils#expressionBuilderSetNewBatchId()}
     */
    @Test
    void testExpressionBuilderSetNewBatchId() {
        Expression actualExpressionBuilderSetNewBatchIdResult = ExpressionUtils.expressionBuilderSetNewBatchId();
        assertEquals("#batchId = :batchId AND #status = :status",
                actualExpressionBuilderSetNewBatchIdResult.expression());
        assertEquals(2, actualExpressionBuilderSetNewBatchIdResult.expressionValues().size());
        assertEquals(2, actualExpressionBuilderSetNewBatchIdResult.expressionNames().size());
    }
}

