package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.CAPModel;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.APIKEY_DOES_NOT_EXISTS;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.CAP_DOES_NOT_EXISTS;

@Component
@lombok.CustomLog
public class CapRepositoryImpl implements CapRepository {

    private final DynamoDbAsyncTable<CAPModel> table;

    public CapRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                             @Value("${pn.address.manager.dynamodb.tablename.cap}") String tableName) {
        this.table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(CAPModel.class));
    }

    @Override
    public Mono<CAPModel> findByCap(String id){
        Key key = Key.builder()
                .partitionValue(id)
                .build();
        return Mono.fromFuture(table.getItem(key))
                .switchIfEmpty(Mono.error(new PnAddressManagerException(CAP_DOES_NOT_EXISTS, CAP_DOES_NOT_EXISTS, HttpStatus.FORBIDDEN.value(), "Cap not found")));

    }
}
