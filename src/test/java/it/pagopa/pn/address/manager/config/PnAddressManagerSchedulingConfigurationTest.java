package it.pagopa.pn.address.manager.config;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class PnAddressManagerSchedulingConfigurationTest {

    private PnAddressManagerSchedulingConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new PnAddressManagerSchedulingConfiguration();
    }

    @Test
    void lockProvider() {
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        PnAddressManagerConfig cfg = new PnAddressManagerConfig();
        PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
        dao.setBatchRequestTableName("Lock");
        cfg.setDao(dao);
        LockProvider provider = configuration.lockProvider(dynamoDB, cfg);
        Assertions.assertNotNull(provider);
    }
}