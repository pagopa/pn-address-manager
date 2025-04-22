package it.pagopa.pn.address.manager.repository;

import it.pagopa.pn.address.manager.LocalStackTestConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.entity.PostelResponseCodeRecipient;
import it.pagopa.pn.address.manager.entity.PostelResponseCodes;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(LocalStackTestConfig.class)
@SpringBootTest
class AddressPnRequestRepositoryImplIT {

    @Autowired
    private AddressBatchRequestRepositoryImpl addressBatchRequestRepository;

    @MockBean
    private AddressUtils addressUtils;

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

    @Test
    void update() {
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
                    assertThat(request.getPostelResponseCodes()).isNull();
                }).verifyComplete();

        StepVerifier.create(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(null, "test2", BatchStatus.WORKED))
                .assertNext(pnRequestPage -> {
                    assertThat(pnRequestPage.items()).hasSize(1);
                    assertThat(pnRequestPage.items().get(0).getBatchId()).isEqualTo("test2");
                    assertThat(pnRequestPage.items().get(0).getCorrelationId()).isEqualTo("TEST_LOCALDATETIME");
                    assertThat(pnRequestPage.items().get(0).getClientId()).isEqualTo("cxId");
                    assertThat(pnRequestPage.items().get(0).getCreatedAt()).isNotNull();
                    assertThat(pnRequestPage.items().get(0).getLastReserved()).isNotNull();
                    assertThat(pnRequestPage.items().get(0).getStatus()).isEqualTo("WORKED");
                    assertThat(pnRequestPage.items().get(0).getRetry()).isZero();
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes()).isNull();
                }).verifyComplete();

        pnRequest.setPostelResponseCodes(getPostelResponseCodesList());

        StepVerifier.create(addressBatchRequestRepository.update(pnRequest))
                .assertNext(request -> {
                    assertThat(request.getBatchId()).isEqualTo("test2");
                    assertThat(request.getCorrelationId()).isEqualTo("TEST_LOCALDATETIME");
                    assertThat(request.getClientId()).isEqualTo("cxId");
                    assertThat(request.getCreatedAt()).isNotNull();
                    assertThat(request.getLastReserved()).isNotNull();
                    assertThat(request.getStatus()).isEqualTo("WORKED");
                    assertThat(request.getRetry()).isZero();
                    assertThat(request.getPostelResponseCodes()).hasSize(2);
                    assertThat(request.getPostelResponseCodes().get(0).getId()).isEqualTo("1");
                    assertThat(request.getPostelResponseCodes().get(0).getPostelResponseCodeRecipient().getNErroreNorm()).isEqualTo(901);
                    assertThat(request.getPostelResponseCodes().get(0).getPostelResponseCodeRecipient().getNRisultatoNorm()).isEqualTo(42);
                    assertThat(request.getPostelResponseCodes().get(0).getPostelResponseCodeRecipient().getFPostalizzabile()).isZero();
                    assertThat(request.getPostelResponseCodes().get(1).getId()).isEqualTo("2");
                    assertThat(request.getPostelResponseCodes().get(1).getPostelResponseCodeRecipient().getNErroreNorm()).isEqualTo(800);
                    assertThat(request.getPostelResponseCodes().get(1).getPostelResponseCodeRecipient().getNRisultatoNorm()).isEqualTo(30);
                    assertThat(request.getPostelResponseCodes().get(1).getPostelResponseCodeRecipient().getFPostalizzabile()).isEqualTo(2);
                }).verifyComplete();

        StepVerifier.create(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(null, "test2", BatchStatus.WORKED))
                .assertNext(pnRequestPage -> {
                    assertThat(pnRequestPage.items()).hasSize(1);
                    assertThat(pnRequestPage.items().get(0).getBatchId()).isEqualTo("test2");
                    assertThat(pnRequestPage.items().get(0).getCorrelationId()).isEqualTo("TEST_LOCALDATETIME");
                    assertThat(pnRequestPage.items().get(0).getClientId()).isEqualTo("cxId");
                    assertThat(pnRequestPage.items().get(0).getCreatedAt()).isNotNull();
                    assertThat(pnRequestPage.items().get(0).getLastReserved()).isNotNull();
                    assertThat(pnRequestPage.items().get(0).getStatus()).isEqualTo("WORKED");
                    assertThat(pnRequestPage.items().get(0).getRetry()).isZero();
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes()).hasSize(2);
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(0).getId()).isEqualTo("1");
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(0).getPostelResponseCodeRecipient().getNErroreNorm()).isEqualTo(901);
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(0).getPostelResponseCodeRecipient().getNRisultatoNorm()).isEqualTo(42);
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(0).getPostelResponseCodeRecipient().getFPostalizzabile()).isZero();
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(1).getId()).isEqualTo("2");
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(1).getPostelResponseCodeRecipient().getNErroreNorm()).isEqualTo(800);
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(1).getPostelResponseCodeRecipient().getNRisultatoNorm()).isEqualTo(30);
                    assertThat(pnRequestPage.items().get(0).getPostelResponseCodes().get(1).getPostelResponseCodeRecipient().getFPostalizzabile()).isEqualTo(2);
                }).verifyComplete();
    }

    private List<PostelResponseCodes> getPostelResponseCodesList() {
        PostelResponseCodes postelResponseCodes = new PostelResponseCodes();
        postelResponseCodes.setId("1");
        PostelResponseCodeRecipient postelResponseCodeRecipient = new PostelResponseCodeRecipient();
        postelResponseCodeRecipient.setNRisultatoNorm(42);
        postelResponseCodeRecipient.setNErroreNorm(901);
        postelResponseCodeRecipient.setFPostalizzabile(0);
        postelResponseCodes.setPostelResponseCodeRecipient(postelResponseCodeRecipient);
        PostelResponseCodes postelResponseCodes2 = new PostelResponseCodes();
        postelResponseCodes2.setId("2");
        PostelResponseCodeRecipient postelResponseCodeRecipient2 = new PostelResponseCodeRecipient();
        postelResponseCodeRecipient2.setNRisultatoNorm(30);
        postelResponseCodeRecipient2.setNErroreNorm(800);
        postelResponseCodeRecipient2.setFPostalizzabile(2);
        postelResponseCodes2.setPostelResponseCodeRecipient(postelResponseCodeRecipient2);
        return List.of(postelResponseCodes, postelResponseCodes2);
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
