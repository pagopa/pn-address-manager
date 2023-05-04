package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.entity.BatchAddress;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface BatchAddressRepository {

    Mono<BatchAddress> create(BatchAddress batchAddress);
    Mono<Page<BatchAddress>> getBatchAddressByNotBatchId(Map<String, AttributeValue> lastKey, int limit);
    Mono<BatchAddress> setNewBatchIdToBatchAddress(BatchAddress batchAddress);
    Mono<BatchAddress> update(BatchAddress batchAddress);


}
