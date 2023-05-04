package it.pagopa.pn.address.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PnAddressManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PnAddressManagerApplication.class, args);
    }

}