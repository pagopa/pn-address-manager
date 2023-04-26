package it.pagopa.pn.template.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.eventbridge.AmazonEventBridgeAsyncClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {EventBridgeConfiguration.class})
@ExtendWith(SpringExtension.class)
class EventBridgeConfigurationTest {
    @Autowired
    private EventBridgeConfiguration eventBridgeConfiguration;

    /**
     * Method under test: {@link EventBridgeConfiguration#amazonEventBridgeAsync()}
     */
    @Test
    void testAmazonEventBridgeAsync() {
        assertTrue(eventBridgeConfiguration.amazonEventBridgeAsync() instanceof AmazonEventBridgeAsyncClient);
    }
}

