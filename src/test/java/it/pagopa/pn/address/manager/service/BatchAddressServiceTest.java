package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.entity.BatchAddress;
import it.pagopa.pn.address.manager.repository.BatchAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class BatchAddressServiceTest {

    @Mock
    private BatchAddressRepository batchAddressRepository;
    @Mock
    private CsvService csvService;
    @Mock
    private NormalizeAddressService normalizeAddressService;




    @Test
    void testBatchPecRequest() {
        BatchAddressService batchAddressService = new BatchAddressService(batchAddressRepository,csvService, normalizeAddressService,3);
        BatchAddress batchAddress1 = new BatchAddress();
        batchAddress1.setAddressId("address1");
        batchAddress1.setId("id1");
        batchAddress1.setPr("pr1");
        batchAddress1.setCorrelationId("correlationId1");
        batchAddress1.setCxId("cxId1");
        BatchAddress batchAddress2 = new BatchAddress();
        batchAddress2.setAddressId("address2");
        batchAddress2.setId("id2");
        batchAddress2.setPr("pr2");
        batchAddress2.setCorrelationId("correlationId2");
        batchAddress2.setCxId("cxId2");

        Page<BatchAddress> page1 = Page.create(List.of(batchAddress1), Map.of("key", AttributeValue.builder().s("value").build()));
        Page<BatchAddress> page2 = Page.create(List.of(batchAddress2));

        when(batchAddressRepository.getBatchAddressByNotBatchId(anyMap(), anyInt()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2))
                .thenThrow(RuntimeException.class);

        when(batchAddressRepository.setNewBatchIdToBatchAddress(same(batchAddress1)))
                .thenReturn(Mono.just(batchAddress1));
        when(batchAddressRepository.setNewBatchIdToBatchAddress(same(batchAddress2)))
                .thenReturn(Mono.just(batchAddress2));

        assertDoesNotThrow(batchAddressService::batchAddress);
    }

}
