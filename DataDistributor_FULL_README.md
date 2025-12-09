# Data Distributor – Full Architecture Documentation

This document describes the **architecture**, **components**, **data flows**, **database interactions**, and **package structure** of the **Data Distributor** service.  
It is ready to be used as a `README.md` in your GitHub repository.

---

## Table of Contents

1. High-Level Hexagonal Architecture
2. Detailed Component Diagram
3. Data Flow Diagrams  
   3.1 Daily Batch Processing Flow  
   3.2 Retry Flow  
   3.3 Database Query Flow (NEW Filter)
4. Package Structure
5. Database Schema & Relationships
6. Technology Overview

---

## 1. High-Level Hexagonal Architecture

The application is structured using **Hexagonal Architecture (Ports & Adapters)**.  
External systems interact with the application via **InAdapters**, which delegate to the **Domain layer**. The domain uses **OutAdapters** to communicate with databases, external APIs, and storage.

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL SYSTEMS                               │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐                     │
│  │  Schedulers  │   │  REST APIs   │   │ Dial Provider│                     │
│  │ (Cron Jobs)  │   │ (Manual)     │   │  (Input Data)│                     │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘                     │
└─────────┼──────────────────┼───────────────────┼─────────────────────────────┘
          │                  │                   │
          ▼                  ▼                   ▼
     INADAPTERS (Inbound) – Schedulers, REST Controllers, DTO Mappers
          │
          ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                             DOMAIN (Core Logic)                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  SERVICES (Use Cases)                                               │   │
│  │  • SignalEventBatchSender                                           │   │
│  │  • RetrySender                                                      │   │
│  │  • PrerequisiteChecker                                              │   │
│  │  • BalanceLookup                                                    │   │
│  │  • DispatchSelector                                                 │   │
│  │  • ReportGenerator                                                  │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  PORTS (Interfaces)                                                 │   │
│  │  OUT Ports:                                                         │   │
│  │  • SignalEventRepository                                            │   │
│  │  • ProductRiskMonitoringRepository                                  │   │
│  │  • AuditRepository                                                  │   │
│  │  • AccountBalanceRepository                                         │   │
│  │  • CEHSender                                                        │   │
│  │  • StorageWriter (Azure Blob)                                       │   │
│  │  • ReportWriter                                                     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                       OUTADAPTERS (Infrastructure)                           │
│  • JPA Adapters (SignalEvent, ProductRiskMonitoring, Audit, AccountBalance)  │
│  • Web Adapters (Feign, WebClient, Resilience4j)                             │
│  • Storage Adapters (Azure Blob exporters, report writers)                   │
└──────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL SYSTEMS                               │
│  • SQL Server Database                                                      │
│  • CEH (External Event Hub / API)                                           │
│  • Azure Blob Storage                                                       │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Detailed Component Diagram

This diagram shows how **InAdapters**, **Domain services**, and **OutAdapters** collaborate.

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                                 INADAPTER                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│  • DialSignalDataProviderScheduler                                          │
│      - Triggers daily batch sends                                           │
│  • RetryScheduler                                                           │
│      - Triggers hourly retries                                              │
│  • SignalEventController (REST)                                             │
│      - Exposes endpoints for manual triggers / inspection                   │
│  • DTO Mappers                                                              │
│      - Map REST DTOs ⇆ Domain models                                        │
└───────────────────────┬──────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                  DOMAIN                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│  SERVICES                                                                   │
│  • PrerequisiteChecker                                                      │
│      - validatePrerequisites(LocalDate auditDay)                            │
│      - uses AuditRepository                                                 │
│  • SignalEventBatchSender                                                   │
│      - sendBatch(LocalDateTime start, LocalDateTime end)                    │
│      - uses SignalEventRepository, AccountBalanceRepository,                │
│        DispatchSelector, CEHSender, AuditRepository, ReportWriter           │
│  • RetrySender                                                              │
│      - retryFailed(LocalDate auditDay)                                      │
│      - uses AuditRepository, SignalEventRepository, CEHSender               │
│                                                                             │
│  PORTS (OUT)                                                                │
│  • SignalEventRepository                                                    │
│  • ProductRiskMonitoringRepository                                          │
│  • AccountBalanceRepository                                                 │
│  • AuditRepository                                                          │
│  • CEHSender                                                                │
│  • StorageWriter                                                            │
│  • ReportWriter                                                             │
└───────────────────────┬──────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                               OUTADAPTER                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│  JPA ADAPTERS                                                               │
│  • SignalEventJpaAdapter → SignalEventRepository                            │
│  • ProductRiskMonitoringJpaAdapter → ProductRiskMonitoringRepository        │
│  • AuditJpaAdapter → AuditRepository                                        │
│  • AccountBalanceJpaAdapter → AccountBalanceRepository                      │
│                                                                             │
│  WEB ADAPTERS                                                               │
│  • CEHFeignAdapter → CEHSender (blocking)                                   │
│  • CEHWebClientAdapter → CEHSender (non-blocking, Resilience4j)             │
│                                                                             │
│  STORAGE ADAPTERS                                                           │
│  • AzureBlobStorageAdapter → StorageWriter                                  │
│  • CEHDeliveryReportWriter, DialExportWriter → ReportWriter / StorageWriter │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Data Flow Diagrams

### 3.1 Daily Batch Processing Flow

```text
START (08:30 CET daily)
│
▼
┌──────────────────────────────────────────────────────────────┐
│ DialSignalDataProviderScheduler (InAdapter)                  │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ PrerequisiteChecker (Domain Service)                         │
│ • validatePrerequisites(auditDay = yesterday)               │
│ • Uses AuditRepository to check all prior-day events PASS    │
└───────────────┬──────────────────────────────────────────────┘
                │
          All PASS?
          ┌─────────────── No ────────────────┐
          │                                   │
          ▼                                   │
 [STOP PROCESSING]                            │
 [Raise alert / log failure]                  │
                                              │
                                              ▼ Yes
┌──────────────────────────────────────────────────────────────┐
│ SignalEventBatchSender (Domain Service)                      │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ 1. SignalEventRepository.findEligibleForCEH(start, end,      │
│    minUnauthorizedBalance)                                   │
│    - Filters by:                                             │
│      • eventRecordDateTime between yesterday 00:00–23:59     │
│      • unauthorizedDebitBalance ≥ minUnauthorizedBalance     │
│      • GRV is reportable (via ProductRiskMonitoring)         │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ 2. AccountBalanceRepository.findByAccountAndDate(...)        │
│    - Enrich events with balance data                         │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ 3. DispatchSelector                                          │
│    - Chooses CEH adapter: Feign or WebClient                 │
│    - Based on configuration                                  │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ 4. CEHSender (Feign/WebClient)                               │
│    - POST events to CEH external API                         │
│    - For WebClient: retry & circuit breaker via Resilience4j │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ 5. AuditRepository.save(...)                                 │
│    - Records eventId, auditDay, status (PASS/FAIL),          │
│      response metadata, timestamps                           │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ 6. ReportWriter / StorageWriter                              │
│    - Generates CEH delivery report                           │
│    - Writes to Azure Blob Storage                            │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
END (Daily batch completed)
```

---

### 3.2 Retry Flow

```text
START (Hourly: 13:00–23:00, Tue–Sun)
│
▼
┌──────────────────────────────────────────────────────────────┐
│ RetryScheduler (InAdapter)                                   │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ RetrySender (Domain Service)                                 │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ AuditRepository.findFailedForAuditDay(auditDay)              │
│ • Finds events with last status != PASS                      │
└───────────────┬──────────────────────────────────────────────┘
                │
           Any failed?
           ┌───────────── No ────────────┐
           │                              │
           ▼                              │
     [Skip retry]                         │
                                          │
                                          ▼ Yes
┌──────────────────────────────────────────────────────────────┐
│ SignalEventRepository.findByIds(failedEventIds)              │
│ • Load corresponding events                                  │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ CEHSender.send(events)                                      │
│ • Re-send events to CEH via Feign/WebClient                  │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ AuditRepository.save(...)                                    │
│ • Update audit records with new status / response             │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
END (Retry cycle completed)
```

---

### 3.3 Database Query Flow (NEW Filter)

```text
Application Request
│
▼
┌──────────────────────────────────────────────────────────────┐
│ SignalEventRepository.findEligibleForCEH(start, end,         │
│                                          minBalance)         │
│ (Domain Port - Interface)                                   │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ SignalEventJpaAdapter                                       │
│ (OutAdapter - Implementation)                               │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ SignalEventJpaRepository                                    │
│ (Spring Data JPA)                                           │
│ @Query(                                                     │
│   "SELECT e FROM SignalEventJpaEntity e                     │
│    WHERE e.eventRecordDateTime BETWEEN :start AND :end      │
│      AND e.unauthorizedDebitBalance >= :minBalance          │
│      AND e.grv.reportCW014ToCEH = 'Y'                       │
│    ORDER BY e.uabsEventId ASC"                              │
│ )                                                           │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│ Database (SQL Server)                                       │
│                                                             │
│ SELECT se.*                                                 │
│ FROM   signal_event se                                      │
│ JOIN   product_risk_monitoring prm                          │
│   ON   se.grv = prm.grv                                     │
│ WHERE  se.event_record_date_time BETWEEN ? AND ?            │
│   AND  se.unauthorized_debit_balance >= ?                   │
│   AND  prm.report_cw014_to_ceh = 'Y'                        │
│ ORDER BY se.uabs_event_id ASC;                              │
└───────────────┬──────────────────────────────────────────────┘
                │
                ▼
           JPA Entities
                │
                ▼
           Domain Models
                │
                ▼
       Returned to Application
```

---

## 4. Package Structure

The codebase is organized by **hexagonal layers** and **responsibilities**.

```text
src/
├── main/
│   ├── java/
│   │   └── com/yourpackage/datadistributor/
│   │       │
│   │       ├── application/                      ← Spring wiring & config
│   │       │   ├── config/
│   │       │   │   ├── GlobalTimeZoneConfig.java
│   │       │   │   ├── WebClientConfig.java
│   │       │   │   ├── FeignConfig.java
│   │       │   │   └── CircuitBreakerConfig.java
│   │       │   ├── properties/
│   │       │   │   ├── ExternalApiProperties.java
│   │       │   │   ├── StorageProperties.java
│   │       │   │   ├── SchedulerProperties.java
│   │       │   │   └── ProcessingProperties.java
│   │       │   └── security/
│   │       │       └── KeyVaultConfig.java
│   │       │
│   │       ├── domain/                           ← Core business logic
│   │       │   ├── model/                        ← Domain entities
│   │       │   │   ├── SignalEvent.java
│   │       │   │   ├── ProductRiskMonitoring.java
│   │       │   │   ├── Audit.java
│   │       │   │   └── AccountBalance.java
│   │       │   ├── service/                      ← Use cases
│   │       │   │   ├── SignalEventBatchSender.java
│   │       │   │   ├── RetrySender.java
│   │       │   │   ├── PrerequisiteChecker.java
│   │       │   │   ├── BalanceLookup.java
│   │       │   │   ├── DispatchSelector.java
│   │       │   │   └── ReportGenerator.java
│   │       │   └── port/
│   │       │       └── out/                      ← Ports (interfaces)
│   │       │           ├── SignalEventRepository.java
│   │       │           ├── ProductRiskMonitoringRepository.java
│   │       │           ├── AuditRepository.java
│   │       │           ├── AccountBalanceRepository.java
│   │       │           ├── CEHSender.java
│   │       │           ├── StorageWriter.java
│   │       │           └── ReportWriter.java
│   │       │
│   │       ├── inadapter/                        ← Input adapters
│   │       │   ├── rest/
│   │       │   │   ├── SignalEventController.java
│   │       │   │   └── dto/
│   │       │   │       ├── SignalEventRequest.java
│   │       │   │       └── SignalEventResponse.java
│   │       │   ├── scheduler/
│   │       │   │   ├── DialSignalDataProviderScheduler.java
│   │       │   │   └── RetryScheduler.java
│   │       │   └── mapper/
│   │       │       ├── SignalEventMapper.java
│   │       │       └── AuditMapper.java
│   │       │
│   │       └── outadapter/                       ← Output adapters
│   │           ├── jpa/                          ← Database adapters
│   │           │   ├── SignalEventJpaAdapter.java
│   │           │   ├── ProductRiskMonitoringJpaAdapter.java
│   │           │   ├── AuditJpaAdapter.java
│   │           │   ├── AccountBalanceJpaAdapter.java
│   │           │   ├── entity/
│   │           │   │   ├── SignalEventJpaEntity.java
│   │           │   │   ├── ProductRiskMonitoringJpaEntity.java
│   │           │   │   ├── AuditJpaEntity.java
│   │           │   │   ├── AccountBalanceJpaEntity.java
│   │           │   │   └── CehResponseInitialEventEntity.java
│   │           │   └── repository/
│   │           │       ├── SignalEventJpaRepository.java
│   │           │       ├── ProductRiskMonitoringJpaRepository.java
│   │           │       ├── AuditJpaRepository.java
│   │           │       └── AccountBalanceJpaRepository.java
│   │           ├── web/                          ← External API clients
│   │           │   ├── CEHFeignAdapter.java
│   │           │   └── CEHWebClientAdapter.java
│   │           └── storage/                      ← Azure Blob adapters
│   │               ├── AzureBlobStorageAdapter.java
│   │               ├── CEHDeliveryReportWriter.java
│   │               └── DialExportWriter.java
```

---

## 5. Database Schema & Relationships

```text
Tables (simplified):
- signal_event
- product_risk_monitoring
- audit
- account_balance
- ceh_response_initial_event

Key relationships:
- signal_event.grv → product_risk_monitoring.grv
    • product_risk_monitoring.report_cw014_to_ceh = 'Y' controls whether
      a given GRV is reportable to CEH.

- audit links events to their delivery status and responses.

- account_balance enriches events with balance information (used during
  eligibility checks and reporting).

- ceh_response_initial_event stores raw CEH responses for traceability.
```

---

## 6. Technology Overview

- **Language**: Java
- **Framework**: Spring Boot
- **Architecture**: Hexagonal (Ports & Adapters)
- **Persistence**: Spring Data JPA, SQL Server
- **HTTP Clients**: Feign (blocking), WebClient (non-blocking, reactive)
- **Resilience**: Resilience4j (circuit breakers, retries)
- **Cloud Storage**: Azure Blob Storage
- **Authentication**: Managed Identity (for Azure resources)
- **Scheduling**: Spring Scheduler (cron-based jobs)

---

_End of file._
