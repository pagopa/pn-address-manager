package it.pagopa.pn.address.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class BatchSchedulerConfig {

    @Bean("addressManagerBatchScheduler")
    public Scheduler schedulerBatch() {
        return Schedulers.boundedElastic();
    }

}
