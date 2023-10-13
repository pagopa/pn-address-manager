package it.pagopa.pn.address.manager.config;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class PnAddressManagerSchedulingConfigurationTest {

    private PnAddressManagerSchedulingConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new PnAddressManagerSchedulingConfiguration();
    }

    @Test
    @Disabled
    void lockProvider() {
        DynamoDbClient dynamoDB = DynamoDbClient.builder()
                .region(Region.EU_SOUTH_1)
                .build();
        PnAddressManagerConfig cfg = new PnAddressManagerConfig();
        PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
        dao.setBatchRequestTableName("Lock");
        cfg.setDao(dao);
        LockProvider provider = configuration.lockProvider(dynamoDB, cfg);
        Assertions.assertNotNull(provider);
    }
}