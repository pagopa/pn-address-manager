package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.LocalStackTestConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(LocalStackTestConfig.class)
@SpringBootTest
class AddressPnRequestRepositoryImplIT {

    @Autowired
    private AddressBatchRequestRepositoryImpl addressBatchRequestRepository;



    @Test
    void create() {
        PnRequest pnRequest = getBatchRequest();
        StepVerifier.create(addressBatchRequestRepository.create(pnRequest))
                .assertNext(request -> {
                    assertThat(request.getBatchId()).isEqualTo("test2");
                    assertThat(request.getCorrelationId()).isEqualTo("TEST_LOCALDATETIME");
                    assertThat(request.getClientId()).isEqualTo("cxId");
                    assertThat(request.getCreatedAt()).isNotNull();
                    assertThat(request.getLastReserved()).isNotNull();
                    assertThat(request.getStatus()).isEqualTo("WORKED");
                    assertThat(request.getRetry()).isZero();
                }).verifyComplete();
    }

    @Test
    void getBatchRequestByNotBatchId() {
        Map<String, AttributeValue> lastKey = new HashMap<>();
        StepVerifier.create(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(lastKey, "test", BatchStatus.WORKED))
                .assertNext(page -> page.items().forEach(request -> {
                    assertThat(page.items()).hasSize(2);
                    assertThat(request.getBatchId()).isEqualTo("test");
                    assertThat(request.getCorrelationId()).contains("TEST_LOCALDATETIME");
                    assertThat(request.getClientId()).isEqualTo("cxId");
                    assertThat(request.getCreatedAt()).isEqualTo("2024-03-06T15:03:10.073");
                    assertThat(request.getLastReserved()).isEqualTo("2024-03-06T15:05:23.591");
                    assertThat(request.getStatus()).isEqualTo("WORKED");
                    assertThat(request.getRetry()).isZero();
                }))
                .verifyComplete();
    }

    PnRequest getBatchRequest() {
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("TEST_LOCALDATETIME");
        pnRequest.setBatchId("test2");
        pnRequest.setRetry(0);
        pnRequest.setClientId("cxId");
        pnRequest.setStatus("WORKED");
        pnRequest.setLastReserved(LocalDateTime.parse("2024-03-06T15:05:23.591"));
        pnRequest.setCreatedAt(LocalDateTime.parse("2024-03-06T15:03:10.073"));
        pnRequest.setXApiKey("yourXApiKey");
        pnRequest.setAwsMessageId("yourAwsMessageId");
        return pnRequest;
    }
}
