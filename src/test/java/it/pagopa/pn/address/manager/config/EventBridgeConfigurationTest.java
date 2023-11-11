package it.pagopa.pn.address.manager.config;

import com.amazonaws.services.eventbridge.AmazonEventBridgeAsyncClient;
import it.pagopa.pn.address.manager.config.springbootcfg.AwsConfigsActivation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {EventBridgeConfiguration.class, AwsConfigsActivation.class})
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

