package it.pagopa.pn.address.manager;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.address.manager.middleware.queue.producer.DeduplicateTracingProducer;
import it.pagopa.pn.address.manager.service.PendingRequestService;
import it.pagopa.pn.address.manager.service.PnRequestService;
import it.pagopa.pn.address.manager.service.RecoveryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class })
@Slf4j
public class MockServeConfig {

    protected ClientAndServer mockServer;

    @MockitoBean
    private DeduplicateTracingProducer deduplicateTracingProducer;

    @MockitoBean
    private PnRequestService pnRequestService;

    @MockitoBean
    private RecoveryService recoveryService;

    @MockitoBean
    private PendingRequestService pendingRequestService;

    @BeforeEach
    void setUp() {
        initializationExpection("mockserver/" + this.getClass().getSimpleName() + ".json");

    }

    @AfterEach
    void tearDown() {
        // Ferma il MockServer al termine di ogni test
        mockServer.stop();
    }

    private void initializationExpection(String file){
        log.info("- Initialize Mock Server Expection");
        Resource resource = new ClassPathResource(file);
        try {
            String path = resource.getFile().getAbsolutePath();
            log.info(" - Path : {} ", path);
            ConfigurationProperties.initializationJsonPath(path);
            this.mockServer = ClientAndServer.startClientAndServer(8082);
        } catch (IOException e) {
            log.warn(" - File webhook not found");
        }
    }
}
