package com.datadistributor.application.config;

import com.datadistributor.domain.inport.AccountBalanceQueryUseCase;
import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.outport.FileStoragePort;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.service.AccountBalanceQueryService;
import com.datadistributor.domain.service.InitialCehMappingService;
import com.datadistributor.domain.service.InitialCehQueryService;
import com.datadistributor.domain.service.DialSignalDataExportService;
import com.datadistributor.domain.service.SignalEventDomainService;
import com.datadistributor.domain.service.SignalEventProcessingService;
import com.datadistributor.outadapter.report.AzureBlobReportPublisher;
import com.datadistributor.outadapter.report.AzureBlobStorageClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(DataDistributorProperties.class)
public class DistributorConfiguration {

    @Bean
    SignalEventUseCase getSignalEventUseCase(SignalEventRepository signalEventRepository,
                                            DataDistributorProperties properties) {
        return new SignalEventDomainService(signalEventRepository, properties.getProcessing().getPageSize());
    }

    @Bean
    JobProgressTracker jobProgressTracker() {
        return new JobProgressTracker();
    }

    @Bean
    SignalEventProcessingUseCase signalEventProcessingUseCase(
        SignalEventRepository repository,
        SignalEventBatchPort batchPort,
        SignalAuditQueryPort signalAuditQueryPort,
        JobProgressTracker jobProgressTracker,
        DataDistributorProperties properties,
        DeliveryReportPublisher deliveryReportPublisher
    ) {
        return new SignalEventProcessingService(
            repository,
            batchPort,
            signalAuditQueryPort,
            properties.getProcessing().getBatchSize(),
            jobProgressTracker,
            deliveryReportPublisher);
    }

    @Bean
    InitialCehMappingUseCase initialCehMappingUseCase(InitialCehMappingPort port) {
        return new InitialCehMappingService(port);
    }

    @Bean
    InitialCehQueryUseCase initialCehQueryUseCase(InitialCehMappingPort port) {
        return new InitialCehQueryService(port);
    }

    @Bean
    AccountBalanceQueryUseCase accountBalanceQueryUseCase(AccountBalanceOverviewPort port) {
        return new AccountBalanceQueryService(port);
    }

    @Bean
    DeliveryReportPublisher deliveryReportPublisher(DataDistributorProperties properties, FileStoragePort fileStoragePort) {
        return new AzureBlobReportPublisher(properties, fileStoragePort);
    }

    @Bean
    FileStoragePort fileStoragePort(DataDistributorProperties properties) {
        return new AzureBlobStorageClient(properties);
    }

    @Bean
    DialSignalDataExportService dialSignalDataExportService(SignalEventUseCase signalEventUseCase,
                                                           FileStoragePort storageClient,
                                                           DataDistributorProperties properties) {
        return new DialSignalDataExportService(signalEventUseCase, storageClient, properties);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
