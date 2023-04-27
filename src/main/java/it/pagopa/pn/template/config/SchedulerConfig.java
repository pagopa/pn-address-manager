package it.pagopa.pn.template.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class SchedulerConfig {

    @Bean("addressManagerScheduler")
    public Scheduler scheduler() {
        return Schedulers.boundedElastic();
    }

}
