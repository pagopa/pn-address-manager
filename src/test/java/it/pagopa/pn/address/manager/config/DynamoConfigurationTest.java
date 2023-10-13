package it.pagopa.pn.address.manager.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@ContextConfiguration(classes = {DynamoConfiguration.class, String.class})
@ExtendWith(SpringExtension.class)
class DynamoConfigurationTest {
    @Autowired
    private DynamoConfiguration dynamoConfiguration;

    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Test
    void testDynamoDb() {
        dynamoConfiguration.dynamoDb();
    }

}

