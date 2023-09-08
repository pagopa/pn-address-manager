package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Getter
@Configuration
@ConfigurationProperties( prefix = "pn.address.manager")
@Data
@Import(SharedAutoConfiguration.class)
public class PnAddressManagerConfig {

    private String validationPattern;
    private Boolean enableValidation;
    private Boolean flagCsv;
}
