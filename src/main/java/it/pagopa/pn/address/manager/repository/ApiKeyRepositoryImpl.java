package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.APIKEY_DOES_NOT_EXISTS;

@Component
@lombok.CustomLog
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    private final DynamoDbAsyncTable<ApiKeyModel> table;

    public ApiKeyRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                PnAddressManagerConfig pnAddressManagerConfig) {
        this.table = dynamoDbEnhancedClient.table(pnAddressManagerConfig.getDao().getApiKeyTableName(), TableSchema.fromBean(ApiKeyModel.class));
    }

    @Override
    public Mono<ApiKeyModel> findById(String cxId){
        Key key = Key.builder()
                .partitionValue(cxId)
                .build();
        return Mono.fromFuture(table.getItem(key));
    }
}
