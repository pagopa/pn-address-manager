package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.CAP_DOES_NOT_EXISTS;

@Component
@lombok.CustomLog
public class CapRepositoryImpl implements CapRepository {

    private final DynamoDbAsyncTable<CapModel> table;

    public CapRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                             PnAddressManagerConfig pnAddressManagerConfig) {
        this.table = dynamoDbEnhancedClient.table(pnAddressManagerConfig.getDao().getCapTableName(), TableSchema.fromBean(CapModel.class));
    }

    @Override
    public Mono<CapModel> findValidCap(String cap){
        Key key = Key.builder()
                .partitionValue(cap)
                .build();
        return Mono.fromFuture(table.getItem(key))
                .switchIfEmpty(Mono.defer(() -> {
                    var errorMessage = String.format(CAP_DOES_NOT_EXISTS, cap);
                    return Mono.error(new PnInternalAddressManagerException(errorMessage, errorMessage, HttpStatus.NOT_FOUND.value(), errorMessage));
                }));
    }
}
