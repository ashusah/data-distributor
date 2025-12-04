package com.datadistributor.application.config;

import com.datadistributor.domain.inport.AccountBalanceQueryUseCase;
import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.inport.SignalEventRetryUseCase;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.outport.FileStoragePort;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.outport.SignalEventSenderPort;
import com.datadistributor.domain.outport.SignalPort;
import com.datadistributor.domain.service.AccountBalanceQueryService;
import com.datadistributor.domain.service.InitialCehMappingService;
import com.datadistributor.domain.service.InitialCehQueryService;
import com.datadistributor.domain.service.DialSignalDataExportService;
import com.datadistributor.domain.service.SignalQueryService;
import com.datadistributor.domain.service.SignalEventDomainService;
import com.datadistributor.domain.service.SignalEventProcessingService;
import com.datadistributor.domain.service.SignalEventRetryService;
import com.datadistributor.domain.inport.SignalQueryUseCase;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.domain.service.SignalDispatchSelector;
import com.datadistributor.outadapter.report.AzureBlobReportPublisher;
import com.datadistributor.outadapter.report.AzureBlobStorageClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Wires domain services, ports, and adapters together while keeping configuration in one place.
 * This is the primary assembly root for the hexagonal architecture.
 */
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
        SignalDispatchSelectorUseCase signalDispatchSelectorUseCase,
        JobProgressTracker jobProgressTracker,
        DataDistributorProperties properties,
        DeliveryReportPublisher deliveryReportPublisher
    ) {
        return new SignalEventProcessingService(
            repository,
            batchPort,
            signalAuditQueryPort,
            signalDispatchSelectorUseCase,
            properties.getProcessing().getBatchSize(),
            jobProgressTracker,
            deliveryReportPublisher);
    }

    @Bean
    SignalEventRetryUseCase signalEventRetryUseCase(SignalAuditQueryPort signalAuditQueryPort,
                                                    SignalEventRepository signalEventRepository,
                                                    SignalEventSenderPort signalEventSenderPort) {
        return new SignalEventRetryService(
            signalAuditQueryPort,
            signalEventRepository,
            signalEventSenderPort);
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
                                                           SignalQueryUseCase signalQueryUseCase,
                                                           AccountBalanceOverviewPort accountBalanceOverviewPort,
                                                           FileStoragePort storageClient,
                                                           DataDistributorProperties properties) {
        return new DialSignalDataExportService(signalEventUseCase, signalQueryUseCase, accountBalanceOverviewPort, storageClient, properties);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    SignalQueryUseCase signalQueryUseCase(SignalPort signalPort) {
        return new SignalQueryService(signalPort);
    }

    @Bean
    SignalDispatchSelectorUseCase signalDispatchSelectorUseCase(SignalEventRepository signalEventRepository,
                                                                SignalPort signalPort,
                                                                SignalAuditQueryPort signalAuditQueryPort,
                                                                InitialCehMappingPort initialCehMappingPort,
                                                                DataDistributorProperties properties) {
        return new SignalDispatchSelector(
            signalEventRepository,
            signalPort,
            signalAuditQueryPort,
            initialCehMappingPort,
            properties.getProcessing().getMinUnauthorizedDebitBalance(),
            properties.getProcessing().getBookDateLookbackDays(),
            properties.getAudit().getConsumerId()
        );
    }
}
