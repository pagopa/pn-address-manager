package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.CountryModel;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.COUNTRY_DOES_NOT_EXISTS;

@Component
@lombok.CustomLog
public class CountryRepositoryImpl implements CountryRepository{

    private final DynamoDbAsyncTable<CountryModel> table;

    public CountryRepositoryImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                             PnAddressManagerConfig pnAddressManagerConfig) {
        this.table = dynamoDbEnhancedClient.table(pnAddressManagerConfig.getDao().getCountryTableName(), TableSchema.fromBean(CountryModel.class));
    }

    @Override
    public Mono<CountryModel> findByName(String country) {
        Key key = Key.builder()
                .partitionValue(country)
                .build();
        return Mono.fromFuture(table.getItem(key))
                .switchIfEmpty(Mono.defer(() -> {
                    var errorMessage = String.format(COUNTRY_DOES_NOT_EXISTS, country);
                    return Mono.error(new PnInternalAddressManagerException(errorMessage, errorMessage, HttpStatus.NOT_FOUND.value(), errorMessage));
                }));
    }
}
