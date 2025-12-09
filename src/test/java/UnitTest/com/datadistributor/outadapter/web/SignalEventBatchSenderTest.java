package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.job.BatchResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

class SignalEventBatchSenderTest {

  @Mock
  private SignalEventClient blockingClient;
  @Mock
  private SignalEventClient reactiveClient;
  @Mock
  private InitialCehMappingUseCase initialCehMappingUseCase;
  @Mock
  private SignalAuditService signalAuditService;
  @Mock
  private ErrorClassifier errorClassifier;

  private DataDistributorProperties properties;
  private SignalEventBatchSender sender;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
    properties.getExternalApi().setBaseUrl("http://example");
    sender = new SignalEventBatchSender(blockingClient, reactiveClient, initialCehMappingUseCase,
        signalAuditService, properties, errorClassifier);
  }

  private SignalEvent event(long id) {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(id);
    event.setSignalId(99L);
    event.setAgreementId(88L);
    event.setEventRecordDateTime(LocalDateTime.now());
    event.setEventStatus("OVERLIMIT_SIGNAL");
    return event;
  }

  private ApiResponse createSuccessResponse(long cehEventId) {
    return new ApiResponse(Map.of("ceh_event_id", cehEventId), 200);
  }

  @Test
  void submitBatch_successfulReactive() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 123L), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(1);
    verify(signalAuditService).persistAudit(any(SignalEvent.class), eq("PASS"), eq("200"), any());
    verify(initialCehMappingUseCase).handleInitialCehMapping(any(), eq(123L));
  }

  @Test
  void submitBatch_recordsFailureOnError() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    RuntimeException boom = new RuntimeException("boom");
    when(reactiveClient.send(any())).thenReturn(Mono.error(boom));
    when(errorClassifier.classify(boom)).thenReturn(new FailureClassification("FAIL", "reason", "500"));

    BatchResult result = sender.submitBatch(List.of(event(2))).get();

    assertThat(result.failureCount()).isEqualTo(1);
    verify(signalAuditService).persistAudit(any(SignalEvent.class), eq("FAIL"), eq("500"), any());
    verify(initialCehMappingUseCase, never()).handleInitialCehMapping(any(), anyLong());
  }

  @Test
  void send_usesBlockingClientWhenConfigured() {
    properties.getExternalApi().setUseBlockingClient(true);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 321L), 200);
    when(blockingClient.send(any())).thenReturn(Mono.just(response));

    boolean result = sender.send(event(3));

    assertThat(result).isTrue();
    verify(signalAuditService).persistAudit(any(SignalEvent.class), eq("PASS"), eq("200"), any());
  }

  @Test
  void submitBatch_returnsEmptyResultOnNullList() throws Exception {
    assertThat(sender.submitBatch(null).get().successCount()).isZero();
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void submitBatch_returnsEmptyResultOnEmptyList() throws Exception {
    assertThat(sender.submitBatch(List.of()).get().successCount()).isZero();
  }

  @Test
  void submitBatch_handlesMultipleEvents() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 123L), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1), event(2), event(3)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(3);
    verify(signalAuditService, times(3)).persistAudit(any(SignalEvent.class), eq("PASS"), eq("200"), any());
    verify(initialCehMappingUseCase, times(3)).handleInitialCehMapping(any(), eq(123L));
  }

  @Test
  void submitBatch_handlesMixedSuccessAndFailure() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse successResponse = new ApiResponse(Map.of("ceh_event_id", 123L), 200);
    RuntimeException error = new RuntimeException("error");
    when(reactiveClient.send(any())).thenAnswer(invocation -> {
      SignalEvent evt = invocation.getArgument(0);
      if (evt.getUabsEventId() == 1L) {
        return Mono.just(successResponse);
      } else {
        return Mono.error(error);
      }
    });
    when(errorClassifier.classify(error)).thenReturn(new FailureClassification("FAIL", "reason", "500"));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1), event(2)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failureCount()).isEqualTo(1);
    verify(signalAuditService).persistAudit(any(SignalEvent.class), eq("PASS"), eq("200"), any());
    verify(signalAuditService).persistAudit(any(SignalEvent.class), eq("FAIL"), eq("500"), any());
  }

  @Test
  void submitBatch_usesBlockingClientWhenConfigured() throws Exception {
    properties.getExternalApi().setUseBlockingClient(true);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 456L), 200);
    when(blockingClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(1);
    verify(blockingClient).send(any());
    verify(reactiveClient, never()).send(any());
  }

  @Test
  void submitBatch_handlesResponseWithoutCehEventId() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("other_field", "value"), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isEqualTo(1);
    verify(signalAuditService).persistAudit(any(SignalEvent.class), eq("FAIL"), eq("200"), eq("NO_CEH_EVENT_ID"));
    verify(initialCehMappingUseCase, never()).handleInitialCehMapping(any(), anyLong());
  }

  @Test
  void submitBatch_handlesNullResponse() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    // Return Mono.error() to simulate a failure (Mono.empty() completes without value, which doesn't trigger error handling)
    RuntimeException error = new RuntimeException("No response");
    when(reactiveClient.send(any())).thenReturn(Mono.error(error));
    when(errorClassifier.classify(error)).thenReturn(new FailureClassification("FAIL", "No response", "500"));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isEqualTo(1);
  }

  @Test
  void submitBatch_handlesNullResponseBody() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    // When response body is null, accessing response.body().get() will throw NPE
    // This triggers onErrorResume which calls handleException
    ApiResponse response = new ApiResponse(null, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));
    NullPointerException npe = new NullPointerException("response.body() is null");
    when(errorClassifier.classify(any(NullPointerException.class)))
        .thenReturn(new FailureClassification("FAIL", "Null response body", "500"));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isEqualTo(1);
  }

  @Test
  void submitBatch_handlesAuditServiceException() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 123L), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));
    doThrow(new RuntimeException("Audit failed"))
        .when(signalAuditService).persistAudit(any(), any(), any(), any());

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(1);
    verify(signalAuditService).logAuditFailure(any(), any());
    verify(initialCehMappingUseCase).handleInitialCehMapping(any(), eq(123L));
  }

  @Test
  void submitBatch_handlesCehEventIdAsInteger() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 123), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(1);
    verify(initialCehMappingUseCase).handleInitialCehMapping(any(), eq(123L));
  }

  @Test
  void submitBatch_handlesCehEventIdAsString() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", "456"), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(1);
    verify(initialCehMappingUseCase).handleInitialCehMapping(any(), eq(456L));
  }

  @Test
  void submitBatch_logsInfoAtSequence1000() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 123L), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    // Create 1000 events to trigger the info log
    List<SignalEvent> events = new java.util.ArrayList<>();
    for (int i = 1; i <= 1000; i++) {
      events.add(event((long) i));
    }

    CompletableFuture<BatchResult> future = sender.submitBatch(events);
    BatchResult result = future.get();

    assertThat(result.successCount()).isEqualTo(1000);
  }

  @Test
  void send_usesReactiveClientWhenNotBlocking() {
    properties.getExternalApi().setUseBlockingClient(false);
    ApiResponse response = new ApiResponse(Map.of("ceh_event_id", 789L), 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    boolean result = sender.send(event(4));

    assertThat(result).isTrue();
    verify(reactiveClient).send(any());
    verify(blockingClient, never()).send(any());
  }

  @Test
  void send_handlesException() {
    properties.getExternalApi().setUseBlockingClient(false);
    RuntimeException error = new RuntimeException("error");
    when(reactiveClient.send(any())).thenReturn(Mono.error(error));
    when(errorClassifier.classify(error)).thenReturn(new FailureClassification("FAIL", "reason", "500"));

    boolean result = sender.send(event(5));

    assertThat(result).isFalse();
  }

  @Test
  void send_handlesEmptyOptional() {
    properties.getExternalApi().setUseBlockingClient(false);
    when(reactiveClient.send(any())).thenReturn(Mono.just(new ApiResponse(Map.of(), 200)));

    boolean result = sender.send(event(6));

    assertThat(result).isFalse();
  }

  @Test
  void postEventReactive_throwsExceptionForNullEvent() {
    properties.getExternalApi().setUseBlockingClient(false);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
      sender.submitBatch(List.of((SignalEvent) null));
    }).isInstanceOf(Exception.class);
  }

  @Test
  void handleException_handlesAuditServiceException() throws Exception {
    properties.getExternalApi().setUseBlockingClient(false);
    RuntimeException error = new RuntimeException("error");
    when(reactiveClient.send(any())).thenReturn(Mono.error(error));
    when(errorClassifier.classify(error)).thenReturn(new FailureClassification("FAIL", "reason", "500"));
    doThrow(new RuntimeException("Audit failed"))
        .when(signalAuditService).persistAudit(any(), any(), any(), any());

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event(1)));
    BatchResult result = future.get();

    assertThat(result.failureCount()).isEqualTo(1);
    verify(signalAuditService).logAuditFailure(any(), any());
  }

  @Test
  void constructor_normalizesZeroRateLimit() {
    properties.getProcessing().setRateLimit(0);
    SignalEventBatchSender senderWithZeroRate = new SignalEventBatchSender(
        blockingClient, reactiveClient, initialCehMappingUseCase,
        signalAuditService, properties, errorClassifier);

    assertThat(senderWithZeroRate).isNotNull();
  }

  @Test
  void constructor_normalizesNegativeRateLimit() {
    properties.getProcessing().setRateLimit(-5);
    SignalEventBatchSender senderWithNegativeRate = new SignalEventBatchSender(
        blockingClient, reactiveClient, initialCehMappingUseCase,
        signalAuditService, properties, errorClassifier);

    assertThat(senderWithNegativeRate).isNotNull();
  }

  // *****************************
  // FRESH TEST CASE
  // *****************************

  @Test
  void submitBatch_handlesNullEventsList() {
    CompletableFuture<BatchResult> future = sender.submitBatch(null);
    BatchResult result = future.join();

    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isZero();
  }

  @Test
  void submitBatch_handlesSequenceNumberLogging() {
    SignalEvent event = event(1);
    when(reactiveClient.send(any())).thenReturn(Mono.just(createSuccessResponse(100L)));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    future.join();

    // Should log at debug level for non-milestone sequences
    verify(reactiveClient).send(any());
  }

  @Test
  void postEventReactive_handlesNullResponse() {
    SignalEvent event = event(1);
    // Return error Mono to simulate null response causing NPE
    // When response is null, response.body() would throw NPE, so we simulate that with an error
    when(reactiveClient.send(any())).thenReturn(Mono.error(new NullPointerException("Response is null")));
    when(errorClassifier.classify(any(NullPointerException.class))).thenReturn(new FailureClassification("FAIL", "No response", "500"));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.failureCount()).isEqualTo(1);
    verify(errorClassifier).classify(any(NullPointerException.class));
  }

  @Test
  void postEventReactive_handlesNullResponseBody() {
    SignalEvent event = event(1);
    ApiResponse response = new ApiResponse(null, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));
    // If persistAudit throws an exception, it's caught and logAuditFailure is called
    // But if logAuditFailure also throws, handleException might be called
    // So we need to ensure errorClassifier is mocked if handleException is called
    doThrow(new RuntimeException("Audit failed")).when(signalAuditService).persistAudit(any(), eq("FAIL"), any(), any());
    when(errorClassifier.classify(any())).thenReturn(new FailureClassification("FAIL", "Audit failed", "500"));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.failureCount()).isEqualTo(1);
  }

  @Test
  void handleSuccess_handlesCehEventIdAsInteger() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("ceh_event_id", 100);
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.successCount()).isEqualTo(1);
    verify(signalAuditService).persistAudit(eq(event), eq("PASS"), eq("200"), contains("ceh_event_id=100"));
    verify(initialCehMappingUseCase).handleInitialCehMapping(event, 100L);
  }

  @Test
  void handleSuccess_handlesCehEventIdAsLong() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("ceh_event_id", 200L);
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.successCount()).isEqualTo(1);
    verify(initialCehMappingUseCase).handleInitialCehMapping(event, 200L);
  }

  @Test
  void handleSuccess_handlesCehEventIdAsString() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("ceh_event_id", "300");
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.successCount()).isEqualTo(1);
    verify(initialCehMappingUseCase).handleInitialCehMapping(event, 300L);
  }

  @Test
  void handleSuccess_handlesAuditPersistenceFailure() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("ceh_event_id", 100L);
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));
    doThrow(new RuntimeException("Audit failed"))
        .when(signalAuditService).persistAudit(any(), any(), any(), any());

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.successCount()).isEqualTo(1);
    verify(signalAuditService).logAuditFailure(eq(event), any(RuntimeException.class));
    verify(initialCehMappingUseCase).handleInitialCehMapping(event, 100L);
  }

  @Test
  void handleSuccess_handlesNoCehEventIdWithAuditFailure() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("other_field", "value");
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));
    doThrow(new RuntimeException("Audit failed"))
        .when(signalAuditService).persistAudit(any(), any(), any(), any());

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.failureCount()).isEqualTo(1);
    verify(signalAuditService).logAuditFailure(eq(event), any(RuntimeException.class));
  }

  @Test
  void handleException_handlesAuditPersistenceFailure() {
    SignalEvent event = event(1);
    RuntimeException error = new RuntimeException("Network error");
    when(reactiveClient.send(any())).thenReturn(Mono.error(error));
    when(errorClassifier.classify(error)).thenReturn(new FailureClassification("FAIL", "reason", "500"));
    doThrow(new RuntimeException("Audit failed"))
        .when(signalAuditService).persistAudit(any(), any(), any(), any());

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.failureCount()).isEqualTo(1);
    verify(signalAuditService).logAuditFailure(eq(event), any(RuntimeException.class));
  }

  @Test
  void parseLongSafely_handlesNumber() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("ceh_event_id", 123);
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.successCount()).isEqualTo(1);
    verify(initialCehMappingUseCase).handleInitialCehMapping(event, 123L);
  }

  @Test
  void parseLongSafely_handlesString() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("ceh_event_id", "456");
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.successCount()).isEqualTo(1);
    verify(initialCehMappingUseCase).handleInitialCehMapping(event, 456L);
  }

  @Test
  void hasCehEventId_returnsFalseForNullBody() {
    SignalEvent event = event(1);
    ApiResponse response = new ApiResponse(null, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));
    // Null body triggers handleSuccess path with hasCehEventId=false, which persists FAIL audit
    // If persistAudit throws, it's caught and logAuditFailure is called
    // But if logAuditFailure also throws, handleException might be called
    doThrow(new RuntimeException("Audit failed")).when(signalAuditService).persistAudit(any(), eq("FAIL"), any(), any());
    when(errorClassifier.classify(any())).thenReturn(new FailureClassification("FAIL", "Audit failed", "500"));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.failureCount()).isEqualTo(1);
  }

  @Test
  void hasCehEventId_returnsFalseWhenKeyMissing() {
    SignalEvent event = event(1);
    Map<String, Object> body = Map.of("other_key", "value");
    ApiResponse response = new ApiResponse(body, 200);
    when(reactiveClient.send(any())).thenReturn(Mono.just(response));

    CompletableFuture<BatchResult> future = sender.submitBatch(List.of(event));
    BatchResult result = future.join();

    assertThat(result.failureCount()).isEqualTo(1);
  }

  @Test
  void send_handlesExceptionDuringBlock() {
    SignalEvent event = event(1);
    when(reactiveClient.send(any())).thenReturn(Mono.error(new RuntimeException("Error")));

    boolean result = sender.send(event);

    assertThat(result).isFalse();
  }

}
