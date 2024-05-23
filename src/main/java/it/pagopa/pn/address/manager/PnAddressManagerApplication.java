package it.pagopa.pn.address.manager;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PnAddressManagerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PnAddressManagerApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        app.run(args);
    }

}