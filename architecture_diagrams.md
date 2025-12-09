# Data Distributor - Architecture Diagrams

## Table of Contents
1. [High-Level Hexagonal Architecture](#high-level-hexagonal-architecture)
2. [Detailed Component Diagram](#detailed-component-diagram)
3. [Data Flow Diagrams](#data-flow-diagrams)
4. [Package Structure](#package-structure)
5. [Database Schema Relationships](#database-schema-relationships)

---

## High-Level Hexagonal Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          EXTERNAL SYSTEMS                                    │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │ Schedulers   │    │  REST APIs   │    │  Dial Data   │                  │
│  │ (Cron Jobs)  │    │  (Manual)    │    │  Provider    │                  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                  │
│         │                   │                   │                           │
└─────────┼───────────────────┼───────────────────┼───────────────────────────┘
          │                   │                   │
          │              IN ADAPTERS              │
          ↓                   ↓                   ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                          INADAPTER LAYER                                     │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  • DialSignalDataProviderScheduler                                 │    │
│  │  • RetryScheduler                                                  │    │
│  │  • SignalEventController (REST)                                    │    │
│  │  • DTO Mappers (Request/Response)                                  │    │
│  └────────────────┬───────────────────────────────────────────────────┘    │
│                   │                                                          │
└───────────────────┼──────────────────────────────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER (Core Business Logic)                │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  SERVICES (Use Cases)                                              │    │
│  │  • SignalEventBatchSender                                          │    │
│  │  • RetrySender                                                     │    │
│  │  • PrerequisiteChecker                                             │    │
│  │  • BalanceLookup                                                   │    │
│  │  • DispatchSelector                                                │    │
│  │  • ReportGenerator                                                 │    │
│  └────────────────┬───────────────────────────────────────────────────┘    │
│                   │                                                          │
│  ┌────────────────┴───────────────────────────────────────────────────┐    │
│  │  PORTS (Interfaces - Define contracts)                             │    │
│  │                                                                     │    │
│  │  OUT Ports:                                                         │    │
│  │  • SignalEventRepository                                           │    │
│  │  • ProductRiskMonitoringRepository (NEW)                           │    │
│  │  • AuditRepository                                                 │    │
│  │  • AccountBalanceRepository                                        │    │
│  │  • CEHSender                                                       │    │
│  │  • StorageWriter (Azure Blob)                                      │    │
│  │  • ReportWriter                                                    │    │
│  └────────────────┬───────────────────────────────────────────────────┘    │
└───────────────────┼──────────────────────────────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                        OUTADAPTER LAYER (Infrastructure)                     │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  JPA ADAPTERS (Database)                                           │    │
│  │  • SignalEventJpaAdapter                                           │    │
│  │  • ProductRiskMonitoringJpaAdapter (NEW)                           │    │
│  │  • AuditJpaAdapter                                                 │    │
│  │  • AccountBalanceJpaAdapter                                        │    │
│  │  • CehResponseJpaAdapter                                           │    │
│  │                                                                     │    │
│  │  JPA ENTITIES & REPOSITORIES                                       │    │
│  │  • SignalEventJpaEntity + SignalEventJpaRepository                 │    │
│  │  • ProductRiskMonitoringJpaEntity + Repository (NEW)               │    │
│  │  • AuditJpaEntity + AuditJpaRepository                             │    │
│  │  • AccountBalanceJpaEntity + Repository                            │    │
│  │  • CehResponseInitialEventEntity + Repository                      │    │
│  └────────────────┬───────────────────────────────────────────────────┘    │
│                   │                                                          │
│  ┌────────────────┴───────────────────────────────────────────────────┐    │
│  │  WEB ADAPTERS (External APIs)                                      │    │
│  │  • CEHFeignAdapter (Blocking - Feign)                              │    │
│  │  • CEHWebClientAdapter (Non-blocking - WebClient)                  │    │
│  │  • CircuitBreakerConfig (Resilience4j)                             │    │
│  └────────────────┬───────────────────────────────────────────────────┘    │
│                   │                                                          │
│  ┌────────────────┴───────────────────────────────────────────────────┐    │
│  │  STORAGE ADAPTERS (Azure)                                          │    │
│  │  • AzureBlobStorageAdapter                                         │    │
│  │  • CEHDeliveryReportWriter                                         │    │
│  │  • DialExportWriter                                                │    │
│  └────────────────┬───────────────────────────────────────────────────┘    │
└───────────────────┼──────────────────────────────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                          EXTERNAL SYSTEMS                                    │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │   Database   │    │     CEH      │    │ Azure Blob   │                  │
│  │  (SQL Server)│    │  (External   │    │   Storage    │                  │
│  │              │    │   Event Hub) │    │   (Reports)  │                  │
│  └──────────────┘    └──────────────┘    └──────────────┘                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Detailed Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                                  INADAPTER                                          │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  DialSignalDataProviderScheduler                                            │  │
│  │  @Scheduled(cron = "0 30 8 * * *", zone = "Europe/Amsterdam")               │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  • Triggers daily at 08:30 CET                                              │  │
│  │  • Calls: SignalEventBatchSender                                            │  │
│  │  • Calls: PrerequisiteChecker (checks prior day events)                     │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
│                                             │                                       │
│  ┌─────────────────────────────────────────▼───────────────────────────────────┐  │
│  │  RetryScheduler                                                             │  │
│  │  @Scheduled(cron = "0 0 13-23 * * TUE-SUN", zone = "Europe/Amsterdam")     │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  • Hourly retry 13:00-23:00 (skips Monday)                                  │  │
│  │  • Calls: RetrySender                                                       │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
│                                             │                                       │
└─────────────────────────────────────────────┼───────────────────────────────────────┘
                                              │
                                              ↓
┌────────────────────────────────────────────────────────────────────────────────────┐
│                                    DOMAIN                                           │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  PrerequisiteChecker                                                        │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  validatePrerequisites(LocalDate auditDay)                                  │  │
│  │  • Checks if all events from prior day have status = PASS                   │  │
│  │  • Throws exception if any event failed                                     │  │
│  │  • Uses: AuditRepository                                                    │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
│                                             │                                       │
│  ┌─────────────────────────────────────────▼───────────────────────────────────┐  │
│  │  SignalEventBatchSender                                                     │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  sendBatch(LocalDateTime start, LocalDateTime end)                          │  │
│  │  1. Fetch eligible events → SignalEventRepository                           │  │
│  │  2. Enrich with balance → AccountBalanceRepository                          │  │
│  │  3. Select dispatch method → DispatchSelector                               │  │
│  │  4. Send to CEH → CEHSender (Feign/WebClient)                               │  │
│  │  5. Create audit record → AuditRepository                                   │  │
│  │  6. Generate report → ReportWriter                                          │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
│                                             │                                       │
│  ┌─────────────────────────────────────────▼───────────────────────────────────┐  │
│  │  RetrySender                                                                │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  retryFailed(LocalDate auditDay)                                            │  │
│  │  • Finds events with last audit status != PASS                              │  │
│  │  • Re-sends to CEH                                                          │  │
│  │  • Updates audit records                                                    │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
│                                             │                                       │
│  ┌─────────────────────────────────────────▼───────────────────────────────────┐  │
│  │  DOMAIN PORTS (Interfaces)                                                  │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  • SignalEventRepository                                                    │  │
│  │     - findEligibleForCEH(start, end, minBalance)                            │  │
│  │     - countEligibleForCEH(start, end, minBalance)                           │  │
│  │                                                                             │  │
│  │  • ProductRiskMonitoringRepository (NEW)                                    │  │
│  │     - findByGrv(Short grv)                                                  │  │
│  │     - findAllReportable()                                                   │  │
│  │                                                                             │  │
│  │  • AccountBalanceRepository                                                 │  │
│  │     - findByAccountAndDate(account, date)                                   │  │
│  │                                                                             │  │
│  │  • AuditRepository                                                          │  │
│  │     - save(audit)                                                           │  │
│  │     - findByAuditDay(date)                                                  │  │
│  │                                                                             │  │
│  │  • CEHSender                                                                │  │
│  │     - send(event) : Response                                                │  │
│  │                                                                             │  │
│  │  • StorageWriter                                                            │  │
│  │     - write(filename, content)                                              │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
└─────────────────────────────────────────────┼───────────────────────────────────────┘
                                              │
                                              ↓
┌────────────────────────────────────────────────────────────────────────────────────┐
│                                  OUTADAPTER                                         │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  JPA ADAPTERS                                                               │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  SignalEventJpaAdapter implements SignalEventRepository                     │  │
│  │  • Translates domain models ↔ JPA entities                                  │  │
│  │  • Delegates to SignalEventJpaRepository (Spring Data)                      │  │
│  │                                                                             │  │
│  │  @Query("""                                                                 │  │
│  │    select e from SignalEventJpaEntity e                                     │  │
│  │    where e.eventRecordDateTime between :start and :end                      │  │
│  │      and e.unauthorizedDebitBalance >= :minBalance                          │  │
│  │      and e.grv.reportCW014ToCEH = 'Y'  ← NEW FILTER                         │  │
│  │  """)                                                                       │  │
│  │                                                                             │  │
│  │  ProductRiskMonitoringJpaAdapter implements ProductRiskMonitoringRepository │  │
│  │  • Manages reference data (GRV codes)                                       │  │
│  │  • NO CASCADE - reference data managed separately                           │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
│                                             │                                       │
│  ┌─────────────────────────────────────────▼───────────────────────────────────┐  │
│  │  WEB ADAPTERS                                                               │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  CEHFeignAdapter implements CEHSender                                       │  │
│  │  • Blocking HTTP client using Feign                                         │  │
│  │  • Simple, synchronous calls                                                │  │
│  │  • Configured with: data-distributor.external-api.use-blocking-client=true  │  │
│  │                                                                             │  │
│  │  CEHWebClientAdapter implements CEHSender                                   │  │
│  │  • Non-blocking HTTP client using WebClient                                 │  │
│  │  • Reactive, supports retries with backoff                                  │  │
│  │  • Circuit breaker via Resilience4j                                         │  │
│  │  • Configured with: use-blocking-client=false                               │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
│                                             │                                       │
│  ┌─────────────────────────────────────────▼───────────────────────────────────┐  │
│  │  STORAGE ADAPTERS                                                           │  │
│  │  ────────────────────────────────────────────────────────────────────────   │  │
│  │  AzureBlobStorageAdapter implements StorageWriter                           │  │
│  │  • Writes CEH delivery reports                                              │  │
│  │  • Writes Dial export files                                                 │  │
│  │  • Uses managed identity for authentication                                 │  │
│  │  • Swappable for testing (use FakeStorageAdapter)                           │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                     │
└────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagrams

### 1. Daily Batch Processing Flow

```
START: 08:30 CET Daily
│
▼
┌──────────────────────────────────────────┐
│  DialSignalDataProviderScheduler         │
│  (InAdapter)                              │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  PrerequisiteChecker                      │
│  (Domain Service)                         │
│  • Check prior day audits                │
└──────────────┬───────────────────────────┘
               │
               │ All PASS?
               │ ┌─────No──────► [Stop & Alert]
               │ │
               ▼ Yes
┌──────────────────────────────────────────┐
│  SignalEventBatchSender                   │
│  (Domain Service)                         │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  1. SignalEventRepository                 │
│     .findEligibleForCEH()                 │
│  ────────────────────────────────────────│
│  Query filters:                           │
│  • Date range: yesterday 00:00-23:59     │
│  • Balance >= minUnauthorizedBalance     │
│  • grv.reportCW014ToCEH = 'Y' ← NEW      │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  2. AccountBalanceRepository              │
│     .findByAccountAndDate()               │
│  ────────────────────────────────────────│
│  Enrich events with balance details       │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  3. DispatchSelector                      │
│  ────────────────────────────────────────│
│  Choose: Feign or WebClient adapter      │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  4. CEHSender (Feign/WebClient)          │
│  ────────────────────────────────────────│
│  POST to CEH External API                │
│  • Circuit breaker                        │
│  • Retry with backoff (WebClient only)   │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  5. AuditRepository.save()                │
│  ────────────────────────────────────────│
│  Record: eventId, status (PASS/FAIL),    │
│  timestamp, response                      │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  6. ReportWriter                          │
│  ────────────────────────────────────────│
│  Generate CEH delivery report            │
│  Upload to Azure Blob Storage            │
└──────────────┬───────────────────────────┘
               │
               ▼
              END
```

### 2. Retry Flow

```
START: Hourly 13:00-23:00 (Tue-Sun)
│
▼
┌──────────────────────────────────────────┐
│  RetryScheduler                           │
│  (InAdapter)                              │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  RetrySender                              │
│  (Domain Service)                         │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  AuditRepository                          │
│  .findFailedForAuditDay()                 │
│  ────────────────────────────────────────│
│  Find events with last status != PASS    │
└──────────────┬───────────────────────────┘
               │
               │ Any failed?
               │ ┌─────No──────► [Skip]
               │ │
               ▼ Yes
┌──────────────────────────────────────────┐
│  SignalEventRepository                    │
│  .findByIds(failedEventIds)               │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  CEHSender.send(events)                   │
│  ────────────────────────────────────────│
│  Retry sending to CEH                    │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  AuditRepository.save()                   │
│  ────────────────────────────────────────│
│  Update audit records with new status    │
└──────────────┬───────────────────────────┘
               │
               ▼
              END
```

### 3. Database Query Flow (NEW Filter)

```
Application Request
│
▼
┌────────────────────────────────────────────────────────┐
│  SignalEventRepository.findEligibleForCEH()            │
│  (Domain Port - Interface)                             │
└────────────────────┬───────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────┐
│  SignalEventJpaAdapter                                 │
│  (OutAdapter - Implementation)                         │
└────────────────────┬───────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────┐
│  SignalEventJpaRepository                              │
│  @Query("""                                            │
│    SELECT e                                            │
│    FROM SignalEventJpaEntity e                         │
│    WHERE e.eventRecordDateTime BETWEEN :start AND :end │
│      AND e.unauthorizedDebitBalance >= :minBalance    │
│      AND e.grv.reportCW014ToCEH = 'Y' ← NEW FILTER    │
│    ORDER BY e.uabsEventId ASC                          │
│  """)                                                  │
└────────────────────┬───────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────┐
│  Database (SQL Server)                                 │
│                                                        │
│  SELECT se.*                                           │
│  FROM signal_event se                                  │
│  JOIN product_risk_monitoring prm                      │
│    ON se.grv = prm.grv                                │
│  WHERE se.event_record_date_time BETWEEN ? AND ?      │
│    AND se.unauthorized_debit_balance >= ?             │
│    AND prm.report_cw014_to_ceh = 'Y' ← NEW            │
│  ORDER BY se.uabs_event_id ASC                         │
└────────────────────┬───────────────────────────────────┘
                     │
                     ▼
               JPA Entities
                     │
                     ▼
          Domain Models (via Adapter)
                     │
                     ▼
            Return to Application
```

---

## Package Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/yourpackage/datadistributor/
│   │       │
│   │       ├── application/                    ← Configuration & Spring Wiring
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
│   │       ├── domain/                         ← Business Logic (Pure Java)
│   │       │   ├── model/                      ← Domain Entities
│   │       │   │   ├── SignalEvent.java
│   │       │   │   ├── ProductRiskMonitoring.java (NEW)
│   │       │   │   ├── Audit.java
│   │       │   │   └── AccountBalance.java
│   │       │   │
│   │       │   ├── service/                    ← Use Cases
│   │       │   │   ├── SignalEventBatchSender.java
│   │       │   │   ├── RetrySender.java
│   │       │   │   ├── PrerequisiteChecker.java
│   │       │   │   ├── BalanceLookup.java
│   │       │   │   ├── DispatchSelector.java
│   │       │   │   └── ReportGenerator.java
│   │       │   │
│   │       │   └── port/                       ← Interfaces
│   │       │       └── out/
│   │       │           ├── SignalEventRepository.java
│   │       │           ├── ProductRiskMonitoringRepository.java (NEW)
│   │       │           ├── AuditRepository.java
│   │       │           ├── AccountBalanceRepository.java
│   │       │           ├── CEHSender.java
│   │       │           ├── StorageWriter.java
│   │       │           └── ReportWriter.java
│   │       │
│   │       ├── inadapter/                      ← Input Adapters (Inbound)
│   │       │   ├── rest/
│   │       │   │   ├── SignalEventController.java
│   │       │   │   └── dto/
│   │       │   │       ├── SignalEventRequest.java
│   │       │   │       └── SignalEventResponse.java
│   │       │   │
│   │       │   ├── scheduler/
│   │       │   │   ├── DialSignalDataProviderScheduler.java
│   │       │   │   └── RetryScheduler.java
│   │       │   │
│   │       │   └── mapper/
│   │       │       ├── SignalEventMapper.java
│   │       │       └── AuditMapper.java
│   │       │
│   │       └── outadapter/                     ← Output Adapters (Outbound)
│   │           ├── jpa/                        ← Database Adapters
│   │           │   ├── SignalEventJpaAdapter.java
│   │           │   ├── ProductRiskMonitoringJpaAdapter.java (NEW)
│   │           │   ├── AuditJpaAdapter.java
│   │           │   ├── AccountBalanceJpaAdapter.java
│   │           │   │
│   │           │   ├── entity/                 ← JPA Entities
│   │           │   │   ├── SignalEventJpaEntity.java (MODIFIED)
│   │           │   │   ├── ProductRiskMonitoringJpaEntity.java (NEW)
│   │           │   │   ├── AuditJpaEntity.java
│   │           │   │   ├── AccountBalanceJpaEntity.java
│   │           │   │   └── CehResponseInitialEventEntity.java
│   │           │   │
│   │           │   └── repository/             ← Spring Data Repos
│   │           │       ├── SignalEventJpaRepository.java (MODIFIED)
│   │           │       ├── ProductRiskMonitoringJpaRepository.java (NEW)
│   │           │       ├── AuditJpaRepository.java
│   │           │       ├── AccountBalanceJpaRepository.java
│   │           │       └── CehResponseJpaRepository.java
│   │           │
│   │           ├── web/                        ← External API Adapters
│   │           │   ├── CEHFeignAdapter.java
│   │           │   ├── CEHWebClientAdapter.java
│   │           │   └── dto/
│   │           │       ├── CEHRequest.java
│   │           │       └── CEHResponse.java
│   │           │
│   │           └── storage/                    ← Azure Storage Adapters
│   │               ├── AzureBlobStorageAdapter.java
│   │               ├── CEHDeliveryReportWriter.java
│   │               └── DialExportWriter.java
│   │
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-test.yml
│       └── application-prod.yml
│
└── test/
    ├── java/
    │   ├── UnitTest/                           ← Fast Unit Tests (No Spring)
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   │   ├── SignalEventTest.java
    │   │   │   │   └── ProductRiskMonitoringTest.java
    │   │   │   └── service/
    │   │   │       ├── SignalEventBatchSenderTest.java
    │   │   │       ├── PrerequisiteCheckerTest.java
    │   │   │       └── RetrySenderTest.java
    │   │   │
    │   │   └── outadapter/
    │   │       └── mapper/
    │   │           └── SignalEventMapperTest.java
    │   │
    │   └── IntegrationTest/                    ← Integration Tests (With Spring & DB)
    │       ├── outadapter/
    │       │   ├── jpa/
    │       │   │   ├── SignalEventJpaAdapterTest.java
    │       │   │   ├── ProductRiskMonitoringJpaAdapterTest.java
    │       │   │   └── repository/
    │       │   │       ├── SignalEventJpaRepositoryTest.java
    │       │   │       └── ProductRiskMonitoringJpaRepositoryTest.java
    │       │   └── web/
    │       │       ├── CEHFeignAdapterTest.java
    │       │       └── CEHWebClientAdapterTest.java
    │       │
    │       └── inadapter/
    │           └── scheduler/
    │               ├── DialSignalDataProviderSchedulerTest.java
    │               └── RetrySchedulerTest.java
    │
    └── resources/
        ├── application-test.yml
        └── test-data.sql
```

---

## Database Schema Relationships

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATABASE SCHEMA                                     │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────┐
│ product_risk_monitoring             │  ← Reference Data (GRV Codes)
├────────────────────────────────────┤
│ PK  grv                   SMALLINT │
│     report_cw014_to_ceh   CHAR(1)  │  ← 'Y' or 'N' (NEW FILTER)
│     product_code          VARCHAR  │
│     product_description   VARCHAR  │
│     risk_category         VARCHAR  │
└────────────────┬───────────────────┘
                 │
                 │ Referenced by (Many-to-One)
                 │ NO CASCADE
                 │
                 ▼
┌────────────────────────────────────┐
│ signal_event                        │  ← Transactional Data
├────────────────────────────────────┤
│ PK  uabs_event_id         BIGINT   │
│     event_record_date_time         │
│     unauthorized_debit_balance     │
│ FK  grv                   SMALLINT │ ────┐
│     account_number        VARCHAR  │     │
│     customer_id           VARCHAR  │     │
└────────────────┬───────────────────┘     │
                 │                         │
                 │ One-to-Many              │
                 │                         │
                 ▼                         │
┌────────────────────────────────────┐     │
│ audit                               │     │
├────────────────────────────────────┤     │
│ PK  audit_id              BIGINT   │     │
│ FK  uabs_event_id         BIGINT   │     │
│     audit_day             DATE     │     │
│     status                VARCHAR  │  ← 'PASS' / 'FAIL'
│     attempt_timestamp     TIMESTAMP│     │
│     response_code         VARCHAR  │     │
│     response_message      TEXT     │     │
└────────────────────────────────────┘     │
                                           │
┌────────────────────────────────────┐     │
│ account_balance                     │     │
├────────────────────────────────────┤     │
│ PK  balance_id            BIGINT   │     │
│     account_number        VARCHAR  │     │
│     balance_date          DATE     │     │
│     current_balance       DECIMAL  │     │
│ FK  grv                   SMALLINT │ ────┘ (Same reference)
└────────────────────────────────────┘

┌────────────────────────────────────┐
│ ceh_response_initial_event          │
├────────────────────────────────────┤
│ PK  response_id           BIGINT   │
│ FK  uabs_event_id         BIGINT   │
│     ceh_event_id          VARCHAR  │
│     response_timestamp    TIMESTAMP│
│     response_status       VARCHAR  │
└────────────────────────────────────┘


KEY RELATIONSHIPS:
==================

1. signal_event → product_risk_monitoring
   - Type: @ManyToOne (NO CASCADE)
   - Foreign Key: grv
   - Reason: PRM is reference data, managed separately
   
2. signal_event → audit
   - Type: One-to-Many
   - Multiple audit records per event (retries)
   
3. account_balance → product_risk_monitoring
   - Type: @ManyToOne (with CASCADE)
   - Foreign Key: grv
   - Used for balance enrichment

NEW QUERY FILTER:
=================
SELECT se.*
FROM signal_event se
JOIN product_risk_monitoring prm ON se.grv = prm.grv
WHERE se.event_record_date_time BETWEEN ? AND ?
  AND se.unauthorized_debit_balance >= ?
  AND prm.report_cw014_to_ceh = 'Y'  ← NEW: Only reportable GRV codes
ORDER BY se.uabs_event_id ASC;
```

---

## Component Interaction Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                     SIGNAL EVENT PROCESSING FLOW                          │
└───────────────────────────────────────────────────────────────────────────┘

     Scheduler                Domain Services              Adapters
     ────────                ────────────────              ────────

  ┌──────────┐
  │ Dial     │
  │ Signal   │
  │ Provider │
  │ Scheduler│
  └────┬─────┘
       │ trigger
       │
       ▼
  ┌──────────────────┐
  │ Prerequisite     │──────► AuditJpaAdapter ──────► Database
  │ Checker          │        (Check prior day)       (audit table)
  └────┬─────────────┘
       │ ✓ All PASS
       │
       ▼
  ┌──────────────────┐
  │ Signal Event     │
  │ Batch Sender     │
  └────┬─────────────┘
       │
       ├──1──► SignalEventJpaAdapter ────► Database
       │        findEligibleForCEH()        (signal_event + PRM JOIN)
       │                                    WHERE reportCW014ToCEH = 'Y'
       │
       ├──2──► AccountBalanceJpaAdapter ──► Database
       │        findByAccountAndDate()      (account_balance)
       │
       ├──3──► DispatchSelector
       │        (Choose Feign/WebClient)
       │
       ├──4──► CEHSender
       │        │
       │        ├─► CEHFeignAdapter ───────► CEH External API
       │        │    (Blocking)              (POST /events)
       │        │
       │        └─► CEHWebClientAdapter ───► CEH External API
       │             (Non-blocking)          (POST /events)
       │             • Circuit Breaker
       │             • Retry with Backoff
       │
       ├──5──► AuditJpaAdapter ───────────► Database
       │        save(audit)                  (audit table)
       │
       └──6──► ReportWriter
                │
                └─► AzureBlobStorageAdapter ──► Azure Blob Storage
                     write(report)               (CEH_Delivery_Report.csv)


   ┌──────────┐
   │ Retry    │
   │ Scheduler│
   └────┬─────┘
        │ hourly (13:00-23:00)
        │
        ▼
   ┌──────────────────┐
   │ Retry Sender     │
   └────┬─────────────┘
        │
        ├──1──► AuditJpaAdapter ───────────► Database
        │        findFailedForAuditDay()     (Find status != PASS)
        │
        ├──2──► SignalEventJpaAdapter ─────► Database
        │        findByIds(failedIds)        (Get failed events)
        │
        ├──3──► CEHSender ─────────────────► CEH External API
        │        send(events)                (Retry)
        │
        └──4──► AuditJpaAdapter ───────────► Database
                 save(newAudit)               (Update status)
```

---

## Dual Client Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CEH SENDER IMPLEMENTATIONS                            │
└─────────────────────────────────────────────────────────────────────────┘

Configuration Flag: data-distributor.external-api.use-blocking-client

┌────────────────────────────────┐        ┌────────────────────────────────┐
│     CEHFeignAdapter            │        │    CEHWebClientAdapter         │
│                                │        │                                │
│ use-blocking-client = TRUE     │        │ use-blocking-client = FALSE    │
├────────────────────────────────┤        ├────────────────────────────────┤
│ Technology: Feign              │        │ Technology: Spring WebClient   │
│ Style: Blocking/Synchronous    │        │ Style: Non-blocking/Reactive   │
│ Thread Model: One per request  │        │ Thread Model: Event loop       │
│                                │        │                                │
│ Pros:                          │        │ Pros:                          │
│ ✓ Simple to understand         │        │ ✓ Better resource utilization  │
│ ✓ Easier debugging             │        │ ✓ Built-in retry mechanism     │
│ ✓ Familiar Spring pattern      │        │ ✓ Circuit breaker integration  │
│                                │        │ ✓ Backoff strategies           │
│ Cons:                          │        │                                │
│ ✗ Thread per request           │        │ Cons:                          │
│ ✗ No built-in retry            │        │ ✗ Steeper learning curve       │
│ ✗ Basic error handling         │        │ ✗ Reactive paradigm            │
│                                │        │                                │
│ Use When:                      │        │ Use When:                      │
│ • Simple use cases             │        │ • High throughput needed       │
│ • Low traffic volume           │        │ • Need sophisticated retry     │
│ • Team prefers blocking        │        │ • Circuit breaker required     │
└────────────────────────────────┘        └────────────────────────────────┘
                 │                                      │
                 │                                      │
                 └──────────┬───────────────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │   CEHSender     │
                   │  (Domain Port)  │
                   └─────────────────┘
                            │
                            ▼
                   Domain Services use
                   same interface regardless
                   of implementation
```

---

## Configuration Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CONFIGURATION HIERARCHY                               │
└─────────────────────────────────────────────────────────────────────────┘

application.yml (Base)
├── data-distributor:
│   ├── external-api:
│   │   ├── use-blocking-client: true/false
│   │   ├── base-url: "https://ceh.example.com"
│   │   ├── timeout: 30s
│   │   └── retry:
│   │       ├── max-attempts: 3
│   │       ├── initial-delay: 1s
│   │       └── multiplier: 2.0
│   │
│   ├── scheduler:
│   │   ├── dial-signal-provider:
│   │   │   ├── enabled: true
│   │   │   └── cron: "0 30 8 * * *"
│   │   └── retry:
│   │       ├── enabled: true
│   │       └── cron: "0 0 13-23 * * TUE-SUN"
│   │
│   ├── storage:
│   │   ├── azure-blob:
│   │   │   ├── enabled: true
│   │   │   ├── container: "reports"
│   │   │   ├── folder: "ceh-delivery"
│   │   │   └── managed-identity:
│   │   │       ├── endpoint: "..."
│   │   │       └── client-id: "..."
│   │
│   └── processing:
│       ├── prereq-check-enabled: true
│       ├── min-unauthorized-balance: 0
│       └── batch-size: 100

application-dev.yml (Development)
└── Overrides for local development

application-test.yml (Testing)
└── H2 database, stubbed APIs

application-prod.yml (Production)
└── Real endpoints, Azure Key Vault
```

---

## Error Handling & Resilience

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    RESILIENCE PATTERNS                                   │
└─────────────────────────────────────────────────────────────────────────┘

1. CIRCUIT BREAKER (Resilience4j)
   ────────────────────────────────
   
   CLOSED ──fail──► OPEN ──timeout──► HALF_OPEN ──success──► CLOSED
     │                                      │
     └──────────success───────────────────┘
   
   Configuration:
   • Failure rate threshold: 50%
   • Wait duration in open: 60s
   • Permitted calls in half-open: 10

2. RETRY WITH EXPONENTIAL BACKOFF
   ────────────────────────────────
   
   Attempt 1 ──fail──► Wait 1s ──► Attempt 2 ──fail──► Wait 2s ──► Attempt 3
   
   Configuration:
   • Max attempts: 3
   • Initial delay: 1s
   • Multiplier: 2.0
   • Max delay: 10s

3. TIMEOUT
   ────────
   
   Request ──► [Wait max 30s] ──► Response or TimeoutException
   
4. PREREQUISITE CHECK
   ──────────────────
   
   Before processing new batch:
   • Check if all prior day events have status = PASS
   • If any failed, block new batch and alert
   • Prevents out-of-order processing

5. AUDIT TRAIL
   ────────────
   
   Every send attempt creates audit record:
   • Timestamp
   • Event ID
   • Status (PASS/FAIL)
   • Response code/message
   • Enables retry logic
```

---

## Testing Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      TESTING PYRAMID                                     │
└─────────────────────────────────────────────────────────────────────────┘

                            ▲
                           ╱│╲
                          ╱ │ ╲
                         ╱  │  ╲
                        ╱   │   ╲
                       ╱    │    ╲
                      ╱─────┼─────╲      E2E Tests (Few)
                     ╱      │      ╲     • Full system
                    ╱───────┼───────╲    • Real database
                   ╱        │        ╲   • Stubbed CEH
                  ╱─────────┼─────────╲
                 ╱──────────┼──────────╲ Integration Tests (Some)
                ╱───────────┼───────────╲ • Spring context
               ╱────────────┼────────────╲ • H2 database
              ╱─────────────┼─────────────╲ • Adapter tests
             ╱──────────────┼──────────────╲
            ╱───────────────┼───────────────╲ Unit Tests (Many)
           ╱────────────────┼────────────────╲ • Domain logic
          ╱─────────────────┼─────────────────╲ • No Spring
         ╱──────────────────┼──────────────────╲ • Mock ports
        ╱═══════════════════╧═══════════════════╲


LAYER           │ WHAT TO TEST                  │ TOOLS
────────────────┼───────────────────────────────┼──────────────────
Domain          │ • Business logic              │ JUnit 5
(Unit)          │ • Model validation            │ Mockito
                │ • Use case orchestration      │ AssertJ
                │ Fast, no dependencies         │
────────────────┼───────────────────────────────┼──────────────────
Adapter         │ • JPA queries                 │ @DataJpaTest
(Integration)   │ • Entity mappings             │ H2
                │ • Repository operations       │ TestEntityManager
                │ • WebClient/Feign calls       │ WireMock
────────────────┼───────────────────────────────┼──────────────────
Scheduler       │ • Job execution               │ @SpringBootTest
(Integration)   │ • Service coordination        │ @MockBean
                │ • Error scenarios             │ TestClock
────────────────┼───────────────────────────────┼──────────────────
End-to-End      │ • Complete workflows          │ TestContainers
(E2E)           │ • Scheduler triggers          │ Awaitility
                │ • Data persistence            │ RestAssured
```

---

## Key Design Decisions

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURAL DECISIONS                               │
└─────────────────────────────────────────────────────────────────────────┘

1. HEXAGONAL ARCHITECTURE
   Why: Keeps business logic independent of frameworks
   Result: Domain layer is pure Java, easily testable

2. NO CASCADE ON PRODUCT_RISK_MONITORING
   Why: It's reference data, not owned by signal events
   Result: Must seed PRM before creating events

3. DUAL CLIENT STRATEGY (Feign/WebClient)
   Why: Different teams have different preferences
   Result: Flexible deployment, same domain interface

4. QUERY-LEVEL FILTERING (reportCW014ToCEH)
   Why: Business rule enforced at database level
   Result: Better performance, consistent filtering

5. PREREQUISITE CHECKING
   Why: Prevent out-of-order processing
   Result: Data integrity maintained

6. SEPARATE RETRY SCHEDULER
   Why: Different schedule than main batch
   Result: Failed events reprocessed without affecting main flow

7. AUDIT TRAIL FOR EVERY ATTEMPT
   Why: Full traceability of sends
   Result: Easy debugging, retry logic possible

8. TIMEZONE-AWARE SCHEDULING (CET)
   Why: Business operates in European timezone
   Result: Predictable execution times

9. AZURE BLOB FOR REPORTS
   Why: Durable storage, accessible for analysis
   Result: Reports retained long-term

10. FEATURE FLAGS FOR SCHEDULERS
    Why: Safe deployment, easy rollback
    Result: Can disable features in production instantly
```

---

## Summary: Key Components & Their Roles

| Component | Layer | Responsibility | Dependencies |
|-----------|-------|----------------|--------------|
| **DialSignalDataProviderScheduler** | InAdapter | Triggers daily batch at 08:30 CET | SignalEventBatchSender |
| **RetryScheduler** | InAdapter | Retries failed events hourly | RetrySender |
| **SignalEventBatchSender** | Domain | Orchestrates event processing | All domain ports |
| **PrerequisiteChecker** | Domain | Validates prior day completion | AuditRepository |
| **SignalEventJpaAdapter** | OutAdapter | Implements data access | Spring Data JPA |
| **ProductRiskMonitoringJpaAdapter** | OutAdapter | Manages GRV reference data | Spring Data JPA |
| **CEHFeignAdapter** | OutAdapter | Sends events (blocking) | Feign |
| **CEHWebClientAdapter** | OutAdapter | Sends events (non-blocking) | WebClient, Resilience4j |
| **AzureBlobStorageAdapter** | OutAdapter | Writes reports to cloud | Azure SDK |

---

## Legend

```
Symbol Legend:
──────  Dependency / Call
───►    Data Flow
├──     Branching
│       Connection
┌─┐     Component Box
╱ ╲     Pyramid/Triangle
═══     Solid Base
```

---

**This architecture enables:**
- ✅ Testable business logic (domain is pure Java)
- ✅ Swappable implementations (ports & adapters)
- ✅ Clear separation of concerns (hexagonal layers)
- ✅ Resilient external communication (circuit breakers, retries)
- ✅ Audit trail for compliance (every attempt recorded)
- ✅ Flexible deployment (feature flags)