package com.datadistributor.application.config;

import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.service.InitialCehMappingService;
import com.datadistributor.domain.service.SignalEventDomainService;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.service.SignalEventProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DistributorConfiguration {

    @Bean
    SignalEventUseCase getSignalEventUseCase(SignalEventRepository signalEventRepository,
                                            @Value("${data-distributor.processing.page-size:1000}") int pageSize) {
        return new SignalEventDomainService(signalEventRepository, pageSize);
    }

    @Bean
    JobProgressTracker jobProgressTracker() {
        return new JobProgressTracker();
    }

    @Bean
    SignalEventProcessingUseCase signalEventProcessingUseCase(
        SignalEventRepository repository,
        SignalEventBatchPort batchPort,
        JobProgressTracker jobProgressTracker,
        @Value("${data-distributor.processing.batch-size:100}") int batchSize
    ) {
        return new SignalEventProcessingService(repository, batchPort, batchSize, jobProgressTracker);
    }

    @Bean
    InitialCehMappingUseCase initialCehMappingUseCase(InitialCehMappingPort port) {
        return new InitialCehMappingService(port);
    }
}
