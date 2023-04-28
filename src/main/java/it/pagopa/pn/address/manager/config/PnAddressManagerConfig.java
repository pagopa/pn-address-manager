package it.pagopa.pn.address.manager.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Getter
@Configuration
@Import(SharedAutoConfiguration.class)
public class PnAddressManagerConfig {

}
