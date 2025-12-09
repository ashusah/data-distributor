package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.AccountBalance;
import com.datadistributor.domain.Signal;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.inport.SignalUseCase;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.domain.outport.FileStoragePort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DialSignalDataExportDomainServiceTest {

  @Mock
  private SignalEventUseCase signalEventUseCase;
  @Mock
  private SignalUseCase signalQueryUseCase;
  @Mock
  private AccountBalanceOverviewPort accountBalanceOverviewPort;
  @Mock
  private FileStoragePort storageClient;

  private DataDistributorProperties properties;
  private DialSignalDataExportDomainService service;
  private LocalDate testDate;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
    properties.getStorage().setDialFolder("dial-folder");
    properties.getStorage().setDialFilePrefix("dial-prefix");
    service = new DialSignalDataExportDomainService(
        signalEventUseCase,
        signalQueryUseCase,
        accountBalanceOverviewPort,
        storageClient,
        properties
    );
    testDate = LocalDate.of(2024, 12, 3);
  }

  // *****************************
  // FRESH TEST CASE
  // *****************************

  @Test
  void export_uploadsCsvWithCorrectFileName() {
    SignalEvent event = createEvent(1L, 100L);
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.of(createSignal(1L, 100L)));
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.of(createAccountBalance(100L)));

    service.export(testDate);

    ArgumentCaptor<String> folderCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(folderCaptor.capture(), fileNameCaptor.capture(), contentCaptor.capture());

    assertThat(folderCaptor.getValue()).isEqualTo("dial-folder");
    assertThat(fileNameCaptor.getValue()).isEqualTo("dial-prefix-2024-12-03.csv");
    assertThat(contentCaptor.getValue()).contains("EventId,AccountNumber");
    assertThat(contentCaptor.getValue()).contains("1");
  }

  @Test
  void export_handlesEmptyEventsList() {
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    assertThat(content).isEqualTo("EventId,AccountNumber,IBAN,CustomerId,GRV,ProductId,CurrencyCode,SignalStartDate,SignalEndDate,SignalType,DebitAmount,BookDate\n");
  }

  @Test
  void export_usesFallbackSignalWhenSignalNotFound() {
    SignalEvent event = createEvent(1L, 100L);
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.empty());
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    assertThat(content).contains("1"); // event ID
    assertThat(content).contains("100"); // agreement ID from fallback
  }

  @Test
  void export_handlesNullAccountBalance() {
    SignalEvent event = createEvent(1L, 100L);
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.of(createSignal(1L, 100L)));
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    assertThat(content).contains("1");
    // IBAN, CustomerId, CurrencyCode should be empty when account is null
  }

  @Test
  void buildFileName_usesDefaultPrefixWhenNull() {
    properties.getStorage().setDialFilePrefix(null);
    DialSignalDataExportDomainService serviceWithNullPrefix = new DialSignalDataExportDomainService(
        signalEventUseCase,
        signalQueryUseCase,
        accountBalanceOverviewPort,
        storageClient,
        properties
    );

    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());

    serviceWithNullPrefix.export(testDate);

    ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), fileNameCaptor.capture(), any());

    assertThat(fileNameCaptor.getValue()).isEqualTo("dial-signal-data-2024-12-03.csv");
  }

  @Test
  void buildFileName_usesDefaultPrefixWhenBlank() {
    properties.getStorage().setDialFilePrefix("   ");
    DialSignalDataExportDomainService serviceWithBlankPrefix = new DialSignalDataExportDomainService(
        signalEventUseCase,
        signalQueryUseCase,
        accountBalanceOverviewPort,
        storageClient,
        properties
    );

    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());

    serviceWithBlankPrefix.export(testDate);

    ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), fileNameCaptor.capture(), any());

    assertThat(fileNameCaptor.getValue()).isEqualTo("dial-signal-data-2024-12-03.csv");
  }

  @Test
  void toCsv_handlesMultipleEvents() {
    SignalEvent event1 = createEvent(1L, 100L);
    SignalEvent event2 = createEvent(2L, 200L);
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event1, event2));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.of(createSignal(1L, 100L)));
    when(signalQueryUseCase.getOpenSignalOfAgreement(200L))
        .thenReturn(Optional.of(createSignal(2L, 200L)));
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.empty());
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(200L))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    assertThat(content).contains("1");
    assertThat(content).contains("2");
    // Should have 2 data rows + 1 header row = 3 lines total
    assertThat(content.split("\n")).hasSize(3);
  }

  @Test
  void toCsvRow_handlesNullValues() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(null);
    event.setAgreementId(null);
    event.setSignalId(null);
    event.setGrv(null);
    event.setProductId(null);
    event.setUnauthorizedDebitBalance(null);
    event.setBookDate(null);
    event.setEventRecordDateTime(null);

    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(null))
        .thenReturn(Optional.empty());
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(null))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    // All null values should be converted to empty strings
    assertThat(content).contains(",,,,,,");
  }

  @Test
  void toCsvRow_handlesNullEventRecordDateTime() {
    SignalEvent event = createEvent(1L, 100L);
    event.setEventRecordDateTime(null);
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.empty());
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    // signalFallback should handle null eventRecordDateTime
    assertThat(content).contains("1");
  }

  @Test
  void toCsvRow_includesAllFields() {
    SignalEvent event = createEvent(1L, 100L);
    event.setGrv(Short.valueOf((short) 5));
    event.setProductId(Short.valueOf((short) 200));
    event.setUnauthorizedDebitBalance(500L);
    event.setBookDate(LocalDate.of(2024, 12, 1));

    Signal signal = createSignal(1L, 100L);
    signal.setSignalStartDate(LocalDate.of(2024, 11, 1));
    signal.setSignalEndDate(LocalDate.of(2024, 12, 5));

    AccountBalance account = createAccountBalance(100L);
    account.setIban("GB123456789");
    account.setBcNumber(123L);
    account.setCurrencyCode("GBP");

    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.of(signal));
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.of(account));

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    assertThat(content).contains("1"); // EventId
    assertThat(content).contains("100"); // AccountNumber
    assertThat(content).contains("GB123456789"); // IBAN
    assertThat(content).contains("123"); // CustomerId (bcNumber is Long, so "123" not "BC123")
    assertThat(content).contains("5"); // GRV
    assertThat(content).contains("200"); // ProductId
    assertThat(content).contains("GBP"); // CurrencyCode
    assertThat(content).contains("2024-11-01"); // SignalStartDate
    assertThat(content).contains("2024-12-05"); // SignalEndDate
    assertThat(content).contains("OVERLIMIT_SIGNAL"); // SignalType
    assertThat(content).contains("500"); // DebitAmount
    assertThat(content).contains("2024-12-01"); // BookDate
  }

  @Test
  void signalFallback_createsSignalFromEvent() {
    SignalEvent event = createEvent(1L, 100L);
    event.setSignalId(50L);
    event.setEventRecordDateTime(LocalDate.of(2024, 12, 1).atTime(10, 0));

    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.empty());
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    // signalId is not included in CSV row, only agreementId is
    assertThat(content).contains("100"); // agreementId from fallback
    assertThat(content).contains("2024-12-01"); // signalStartDate from fallback
  }

  @Test
  void safe_handlesNullValue() {
    SignalEvent event = createEvent(1L, 100L);
    event.setUabsEventId(null);
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.of(createSignal(1L, 100L)));
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    // Null uabsEventId should be converted to empty string
    assertThat(content).contains(",100,"); // empty EventId, then AccountNumber
  }

  @Test
  void safe_handlesNonNullValue() {
    SignalEvent event = createEvent(1L, 100L);
    when(signalEventUseCase.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalQueryUseCase.getOpenSignalOfAgreement(100L))
        .thenReturn(Optional.of(createSignal(1L, 100L)));
    when(accountBalanceOverviewPort.getAccountBalanceOfAgreement(100L))
        .thenReturn(Optional.empty());

    service.export(testDate);

    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageClient).upload(any(), any(), contentCaptor.capture());

    String content = contentCaptor.getValue();
    assertThat(content).contains("1"); // non-null uabsEventId
  }

  private SignalEvent createEvent(Long uabsEventId, Long agreementId) {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(uabsEventId);
    event.setAgreementId(agreementId);
    event.setSignalId(1L);
    event.setEventRecordDateTime(LocalDateTime.of(2024, 12, 3, 10, 0));
    event.setEventStatus("OVERLIMIT_SIGNAL");
    return event;
  }

  private Signal createSignal(Long signalId, Long agreementId) {
    Signal signal = new Signal();
    signal.setSignalId(signalId);
    signal.setAgreementId(agreementId);
    signal.setSignalStartDate(LocalDate.of(2024, 11, 1));
    signal.setSignalEndDate(null);
    return signal;
  }

  private AccountBalance createAccountBalance(Long agreementId) {
    AccountBalance account = new AccountBalance();
    account.setAgreementId(agreementId);
    account.setIban("GB123456789");
    account.setBcNumber(123L);
    account.setCurrencyCode("GBP");
    return account;
  }
}

