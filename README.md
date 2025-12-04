# Data Distributor

![Project logo](https://dummyimage.com/240x120/1f3c88/ffffff&text=Data+Distributor)

## Overview
Data Distributor is a Spring Boot 3 (Java 17) service built with hexagonal architecture. It ingests signal events, applies business selection rules, posts batches to external CEH endpoints via Feign/WebClient, persists audit records, and produces delivery reports to Azure Blob Storage. Scheduling, retry, and circuit-breaking are first-class features, with clear ports/adapters boundaries for storage, messaging, and web.

## Why you might like this project
- Hexagonal + SOLID structure: domain-first services with explicit ports for web, persistence, and reporting.
- Robust outbound delivery: Feign (blocking) and WebClient (reactive) paths with retries, circuit breakers, and audit trails.
- Reporting and storage: Delivery reports and dial exports uploaded to Azure Blob Storage through an adapter seam.
- Scheduling and retry: Primary and retry schedulers with configurable cron/flags; prerequisite guards to avoid out-of-order sends.
- Testability: Unit and integration suites exercising schedulers, dispatch selector, retry, CEH propagation, and Azure adapters.

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- (Optional) Docker if you want to run supporting services locally

### Build & Test
```bash
mvn clean test
```

### Run the application
```bash
mvn spring-boot:run
```

### Key Configuration (application.yml / env)
- `data-distributor.external-api.use-blocking-client` – `true` for Feign, `false` for WebClient.
- `data-distributor.external-api.base-url` – CEH endpoint.
- `data-distributor.storage.enabled` – toggle Azure Blob uploads.
- `data-distributor.storage.connection-string` – Azure Storage connection string.
- `data-distributor.storage.container` / `folder` – target container/folder for reports.
- `data-distributor.scheduler.*` – cron expressions and enablement flags for primary and retry schedulers.
- `data-distributor.azure.keyvault.*` – enable/disable and configure Key Vault SSL context.

### Project Structure (ports & adapters)
- `domain/` — business services and ports (repositories, senders, audit queries, file storage).
- `inadapter/` — REST controller, schedulers, retry orchestration.
- `outadapter/` — JPA repositories, web clients (Feign/WebClient), Azure Blob reporting.
- `application/` — Spring configuration, properties binding, security (Key Vault SSL).
- `src/test/java/UnitTest` & `src/test/java/IntegrationTest` — unit and integration suites.

### Running only unit tests
```bash
mvn -q -DskipITs=true test
```

### Running integration tests
```bash
mvn -q -Dgroups=integration test
```
(Adjust to your tagging; by default both run together.)

## Release Notes

| Version | Date       | Highlights                                                    |
|---------|------------|---------------------------------------------------------------|
| 0.1.0   | 2025-12-04 | Initial public baseline: hexagonal layout, schedulers, CEH delivery via Feign/WebClient, Azure Blob reporting seam, full unit/integration coverage pass. |

Add new entries here as you cut releases (features, migration notes, breaking changes).

## Development Tips
- Keep new adapters behind ports; avoid leaking framework classes into the domain.
- Prefer constructor injection and small services to respect SRP.
- For new outbound integrations, add a thin adapter + port and cover it with a stub-backed unit test (no final-class mocking needed).
- When adding schedulers, guard with feature flags and cron in config; mirror retry/prereq behaviour already present.

## Useful Commands
- Format/verify quickly: `mvn -q -DskipTests compile`
- Run a single test: `mvn -q -Dtest=SignalEventBatchSenderTest test`
- Package: `mvn clean package`

## Support & Links
- Spring Boot reference: https://docs.spring.io/spring-boot/docs/3.5.6/reference/html/
- Resilience4j reference: https://resilience4j.readme.io/docs
- Azure Blob Storage docs: https://learn.microsoft.com/azure/storage/blobs/

---
Happy shipping! If you add features, extend the Release Notes table and document any new flags or ports here.
