package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyLong;

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
}
