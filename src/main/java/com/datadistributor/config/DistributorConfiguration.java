package com.datadistributor.config;

import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.service.SignalEventDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DistributorConfiguration {

    @Bean
    SignalEventUseCase getSignalEventUseCase(SignalEventRepository signalEventRepository) {
        return new SignalEventDomainService(signalEventRepository);
    }
}
