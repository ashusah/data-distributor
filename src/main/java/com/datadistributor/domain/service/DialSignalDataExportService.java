package com.datadistributor.domain.service;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.outadapter.report.AzureBlobStorageClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DialSignalDataExportService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private final SignalEventUseCase signalEventUseCase;
  private final AzureBlobStorageClient storageClient;
  private final DataDistributorProperties.Storage storage;

  public DialSignalDataExportService(SignalEventUseCase signalEventUseCase,
                                     AzureBlobStorageClient storageClient,
                                     DataDistributorProperties properties) {
    this.signalEventUseCase = signalEventUseCase;
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
    String header = "uabs_event_id,signal_id,agreement_id,event_record_date_time,event_type,event_status,unauthorized_debit_balance,book_date,grv,product_id";
    String rows = events.stream()
        .map(this::toCsvRow)
        .collect(Collectors.joining("\n"));
    return header + "\n" + rows;
  }

  private String toCsvRow(SignalEvent event) {
    return String.join(",",
        safe(event.getUabsEventId()),
        safe(event.getSignalId()),
        safe(event.getAgreementId()),
        safe(event.getEventRecordDateTime()),
        safe(event.getEventType()),
        safe(event.getEventStatus()),
        safe(event.getUnauthorizedDebitBalance()),
        safe(event.getBookDate()),
        safe(event.getGrv()),
        safe(event.getProductId()));
  }

  private String safe(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
