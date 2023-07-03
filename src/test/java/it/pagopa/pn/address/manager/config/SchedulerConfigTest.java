package it.pagopa.pn.address.manager.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {SchedulerConfig.class})
@ExtendWith(SpringExtension.class)
class SchedulerConfigTest {
    @Autowired
    private SchedulerConfig schedulerConfig;

    /**
     * Method under test: {@link SchedulerConfig#scheduler()}
     */
    @Test
    void testScheduler() {
        Assertions.assertDoesNotThrow(() -> schedulerConfig.scheduler());
    }
}

