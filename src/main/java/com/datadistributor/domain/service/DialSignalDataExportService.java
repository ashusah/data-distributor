package com.datadistributor.domain.service;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.AccountBalance;
import com.datadistributor.domain.Signal;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.inport.SignalUseCase;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.domain.outport.FileStoragePort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Builds and uploads the DIAL signal data export as CSV using domain ports for data enrichment.
 */
@Service
public class DialSignalDataExportService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private final SignalEventUseCase signalEventUseCase;
  private final SignalUseCase signalQueryUseCase;
  private final AccountBalanceOverviewPort accountBalanceOverviewPort;
  private final FileStoragePort storageClient;
  private final DataDistributorProperties.Storage storage;

  public DialSignalDataExportService(SignalEventUseCase signalEventUseCase,
                                     SignalUseCase signalQueryUseCase,
                                     AccountBalanceOverviewPort accountBalanceOverviewPort,
                                     FileStoragePort storageClient,
                                     DataDistributorProperties properties) {
    this.signalEventUseCase = signalEventUseCase;
    this.signalQueryUseCase = signalQueryUseCase;
    this.accountBalanceOverviewPort = accountBalanceOverviewPort;
    this.storageClient = storageClient;
    this.storage = properties.getStorage();
  }

  public void export(LocalDate date) {
    List<SignalEvent> events = signalEventUseCase.getAllSignalEventsOfThisDate(date);
    String content = toCsv(events);
    String fileName = buildFileName(date);
    storageClient.upload(storage.getDialFolder(), fileName, content);
  }

  private String buildFileName(LocalDate date) {
    String prefix = storage.getDialFilePrefix();
    if (prefix == null || prefix.isBlank()) {
      prefix = "dial-signal-data";
    }
    return "%s-%s.csv".formatted(prefix, DATE_FORMAT.format(date));
  }

  private String toCsv(List<SignalEvent> events) {
    String header = "AccountNumber,IBAN,CustomerId,GRV,ProductId,CurrencyCode,SignalStartDate,SignalEndDate,SignalType,DebitAmount,BookDate";
    String rows = events.stream()
        .map(this::toCsvRow)
        .collect(Collectors.joining("\n"));
    return header + "\n" + rows;
  }

  private String toCsvRow(SignalEvent event) {
    Signal signal = signalQueryUseCase.getOpenSignalOfAgreement(event.getAgreementId())
        .orElseGet(() -> signalFallback(event));
    AccountBalance account = accountBalanceOverviewPort.findByAgreementId(event.getAgreementId())
        .orElse(null);

    return String.join(",",
        safe(signal.getAgreementId()), // AccountNumber from signal
        safe(account == null ? null : account.getIban()),
        safe(account == null ? null : account.getBcNumber()), // treating bcNumber as CustomerId
        safe(event.getGrv()),
        safe(event.getProductId()),
        safe(account == null ? null : account.getCurrencyCode()),
        safe(signal.getSignalStartDate()),
        safe(signal.getSignalEndDate()),
        "OVERLIMIT_SIGNAL",
        safe(event.getUnauthorizedDebitBalance()),
        safe(event.getBookDate()));
  }

  private Signal signalFallback(SignalEvent event) {
    Signal s = new Signal();
    s.setSignalId(event.getSignalId());
    s.setAgreementId(event.getAgreementId());
    s.setSignalStartDate(event.getEventRecordDateTime() == null ? null : event.getEventRecordDateTime().toLocalDate());
    s.setSignalEndDate(null);
    return s;
  }

  private String safe(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
