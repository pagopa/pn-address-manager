package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.endpoints.internal.Value;

@Getter
@Configuration
@ConfigurationProperties( prefix = "pn.address.manager")
@Data
@Import(SharedAutoConfiguration.class)
public class PnAddressManagerConfig {

    private String validationPattern;
    private Boolean enableValidation;
    private Boolean flagCsv;
    private WebClient webClient;
    private EventBus eventBus;
    private Csv csv;
    private Sqs sqs;
    private DynamoDB dynamoDB;
    private Postel postel;
    private String pagoPaCxId;
    private String healthCheckPath;

    @Data
    public static class Postel{
        private Integer batchTtl;
        private String authKey;
        private Integer batchRequestMaxRetry;
        private Integer batchRequestMaxSize;
        private Integer batchRequestDelay;
        private Integer batchRequestRecoveryAfter;
        private Integer batchSecondaryTableMaxRetry;
        private Integer batchSecondaryTableRecoveryAfter;
        private Integer batchRequestTableMaxRetry;
        private Integer batchRequestRecoveryDelay;
    }

    @Data
    public static class DynamoDB{
        private String tableNameApiKey;
        private String tableNameCap;
    }

    @Data
    public static class Sqs{
        private String inputQueueName;
        private String inputDlqQueueName;
    }

    @Data
    public static class Csv{
        private String pathCap;
        private String pathCountry;
    }

    @Data
    public static class WebClient{
        private Integer tcpMaxPoolsize;
        private Integer tcpMaxQueuedConnections;
        private Long tcpPendingAcquiredTimeout;
        private Long tcpPoolIdleTimeout;
        private String basePath;
    }

    @Data
    public static class EventBus {
        private String name;
        private String detailType;
        private String source;
    }
}
