package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.CountryModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
class CountryRepositoryImplTest {

    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @MockBean
    private DynamoDbAsyncTable<Object> dynamoDbAsyncTable;

    private CountryRepositoryImpl countryRepository;


    @BeforeEach
    public void setUp() {
        PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
        dao.setBatchRequestTableName("table");
        PnAddressManagerConfig addressManagerConfig = new PnAddressManagerConfig();
        addressManagerConfig.setDao(dao);
        PnAddressManagerConfig.BatchRequest bR = new PnAddressManagerConfig.BatchRequest();
        bR.setMaxRetry(0);
        bR.setRecoveryAfter(1);
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(bR);
        addressManagerConfig.setNormalizer(normalizer);
        when(dynamoDbEnhancedAsyncClient.table(any(), any())).thenReturn(dynamoDbAsyncTable);
        countryRepository = new CountryRepositoryImpl(dynamoDbEnhancedAsyncClient, addressManagerConfig);
    }

    @Test
    void findById(){
        CountryModel capModel = new CountryModel();
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        completableFuture.completeAsync(() -> capModel);
        when(dynamoDbAsyncTable.getItem((Key) any())).thenReturn(completableFuture);
        StepVerifier.create(countryRepository.findByName("id")).expectNext(capModel).verifyComplete();
    }


}
