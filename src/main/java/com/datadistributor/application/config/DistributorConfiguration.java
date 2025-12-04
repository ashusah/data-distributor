package com.datadistributor.application.config;

import com.datadistributor.domain.inport.AccountBalanceUseCase;
import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.inport.SignalEventRetryUseCase;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.inport.SignalUseCase;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.outport.FileStoragePort;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventPort;
import com.datadistributor.domain.outport.SignalEventSenderPort;
import com.datadistributor.domain.outport.SignalPort;
import com.datadistributor.domain.service.AccountBalanceDomainService;
import com.datadistributor.domain.service.InitialCehMappingDomainService;
import com.datadistributor.domain.service.InitialCehQueryDomainService;
import com.datadistributor.domain.service.DialSignalDataExportDomainService;
import com.datadistributor.domain.service.SignalQueryDomainService;
import com.datadistributor.domain.service.SignalEventDomainService;
import com.datadistributor.domain.service.SignalEventProcessingDomainService;
import com.datadistributor.domain.service.SignalEventRetryDomainService;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.domain.service.SignalDispatchDomainSelector;
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
    SignalEventUseCase getSignalEventUseCase(SignalEventPort signalEventRepository,
                                            DataDistributorProperties properties) {
        return new SignalEventDomainService(signalEventRepository, properties.getProcessing().getPageSize());
    }

    @Bean
    JobProgressTracker jobProgressTracker() {
        return new JobProgressTracker();
    }

    @Bean
    SignalEventProcessingUseCase signalEventProcessingUseCase(
        SignalEventPort repository,
        SignalEventBatchPort batchPort,
        SignalAuditQueryPort signalAuditQueryPort,
        SignalDispatchSelectorUseCase signalDispatchSelectorUseCase,
        JobProgressTracker jobProgressTracker,
        DataDistributorProperties properties,
        DeliveryReportPublisher deliveryReportPublisher
    ) {
        return new SignalEventProcessingDomainService(
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
                                                    SignalEventPort signalEventRepository,
                                                    SignalEventSenderPort signalEventSenderPort) {
        return new SignalEventRetryDomainService(
            signalAuditQueryPort,
            signalEventRepository,
            signalEventSenderPort);
    }

    @Bean
    InitialCehMappingUseCase initialCehMappingUseCase(InitialCehMappingPort port) {
        return new InitialCehMappingDomainService(port);
    }

    @Bean
    InitialCehQueryUseCase initialCehQueryUseCase(InitialCehMappingPort port) {
        return new InitialCehQueryDomainService(port);
    }

    @Bean
    AccountBalanceUseCase accountBalanceQueryUseCase(AccountBalanceOverviewPort port) {
        return new AccountBalanceDomainService(port);
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
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    SignalUseCase signalQueryUseCase(SignalPort signalPort) {
        return new SignalQueryDomainService(signalPort);
    }

    @Bean
    SignalDispatchSelectorUseCase signalDispatchSelectorUseCase(SignalEventPort signalEventRepository,
                                                                SignalPort signalPort,
                                                                SignalAuditQueryPort signalAuditQueryPort,
                                                                InitialCehMappingPort initialCehMappingPort,
                                                                DataDistributorProperties properties) {
        return new SignalDispatchDomainSelector(
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
