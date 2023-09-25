package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pn.address.manager")
@Import(SharedAutoConfiguration.class)
public class UploadDownloadClientConfig {

        private String uploadDownloadBasePath;
}
