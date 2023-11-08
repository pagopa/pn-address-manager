package it.pagopa.pn.address.manager.config;

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

    @Bean("addressManagerbatchScheduler")
    public Scheduler schedulerBatch() {
        return Schedulers.boundedElastic();
    }

}
