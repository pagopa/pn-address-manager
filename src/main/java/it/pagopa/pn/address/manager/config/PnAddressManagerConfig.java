package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Getter
@Configuration
@ConfigurationProperties( prefix = "pn.address-manager")
@Data
@Import(SharedAutoConfiguration.class)
public class PnAddressManagerConfig {

    private String validationPattern;
    private Boolean enableValidation;
    private Boolean flagCsv;
    private EventBus eventBus;
    private Csv csv;
    private Sqs sqs;
    private Dao dao;
    private Normalizer normalizer;
    private String pagoPaCxId;
    private String apiKey;
    private String postelBasePath;
    private String postelCxId;
    private String healthCheckPath;
    private String safeStorageBasePath;

    @Data
    public static class Normalizer{
        private BatchRequest batchRequest;
        private Postel postel;
        private String postelAuthKey;
    }

    @Data
    public static class Postel{
        private Integer ttl;
        private Integer maxRetry;
        private Integer maxSize;
        private Integer delay;
        private Integer recoveryAfter;
        private Integer recoveryDelay;
        private String requestPrefix;
    }

    @Data
    public static class BatchRequest{
        private Integer ttl;
        private Integer maxRetry;
        private Integer maxSize;
        private Integer delay;
        private Integer recoveryAfter;
        private Integer recoveryDelay;
        private Integer eventBridgeRecoveryDelay;
    }

    @Data
    public static class Dao{
        private String apiKeyTableName;
        private String capTableName;
        private String postelBatchTableName;
        private String batchRequestTableName;
        private String countryTableName;
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
    public static class EventBus {
        private String name;
        private String detailType;
        private String source;
    }
}
