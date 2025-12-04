# Data Distributor

![Project logo](https://images.unsplash.com/photo-1520607162513-77705c0f0d4a?auto=format&fit=crop&w=960&q=80)

> A bright, event-driven Spring Boot 3 service that routes financial signal events to CEH, ships audit-friendly reports to Azure Blob Storage, and keeps strong boundaries between domain, adapters, and configuration.

## At a Glance
- **Hexagonal + SOLID**: Ports for storage, web, audit, and reporting; adapters stay thin.
- **Dual delivery paths**: Blocking Feign vs. non-blocking WebClient, both behind the same sender port.
- **Safety nets**: Circuit breakers, retries, audit persistence, and prerequisite checks to prevent out-of-order sends.
- **Scheduling + retry**: Primary schedules plus an hourly retry window (skips Mondays) with feature flags and cron controls.
- **Reporting**: CEH delivery reports and Dial exports pushed to Azure Blob Storage through a seam that can be faked in tests.

## Contents
- [Getting Started](#getting-started)
- [Configuration Quick Guide](#configuration-quick-guide)
- [Architecture](#architecture)
- [Testing](#testing)
- [Operations](#operations)
- [Release Notes](#release-notes)
- [Development Tips](#development-tips)
- [Useful Commands](#useful-commands)

## Getting Started
### Prerequisites
- Java 17+
- Maven 3.9+
- (Optional) Docker for any local dependencies you want to mirror

### Build & Test
```bash
mvn clean test
```

### Run the Application
```bash
mvn spring-boot:run
```

### Local Profiles (examples)
- `spring.profiles.active=dev`: local defaults, no Key Vault.
- `spring.profiles.active=test`: in-memory H2, stubbed external APIs.
- `spring.profiles.active=prod`: real CEH endpoints, Azure storage, Key Vault SSL.

## Configuration Quick Guide
Key properties (application.yml / env):
- `data-distributor.external-api.use-blocking-client` — `true` → Feign, `false` → WebClient.
- `data-distributor.external-api.base-url` — CEH endpoint root.
- `data-distributor.external-api.retry.*` — retry/backoff for WebClient path.
- `data-distributor.storage.*` — Azure Blob toggles, connection string, container, folder.
- `data-distributor.scheduler.*` — cron and enablement for main and retry schedulers.
- `data-distributor.processing.prereq-check-enabled` — block new batches if prior day events are not PASS.
- `data-distributor.azure.keyvault.*` — optional SSL context for Key Vault secrets.

## Architecture
- `domain/` — services and ports for signals, audits, balance lookups, dispatch selection, retries, and reporting.
- `application/` — Spring wiring, config properties, security/Key Vault helpers.
- `inadapter/` — REST endpoints, schedulers (primary + retry), DTO mappers.
- `outadapter/` — JPA repositories, web clients (Feign/WebClient), Azure Blob client, report writers.
- `src/test/java/UnitTest` and `src/test/java/IntegrationTest` — split suites; integration tests load Spring context and H2.

## Testing
- Run everything: `mvn clean test`
- Only unit tests (fast): `mvn -q -DskipITs=true test`
- Only a specific test: `mvn -q -Dtest=SignalEventBatchSenderTest test`
- Coverage tip: adapters are seam-friendly—prefer fakes over mocking finals.

## Operations
- **Prerequisite guard**: daily batch stops if any prior-day event has a last audit status other than PASS.
- **Retry window**: hourly from 13:00–23:00 (skips Monday) to resend failed events for the same audit day.
- **Storage uploads**: CEH delivery reports and Dial exports land in `container/folder` on Azure Blob; adapter is swappable for local testing.
- **Feature flags**: each scheduler and client path (Feign/WebClient) is gated by configuration for safe toggles.

## Release Notes

| Version | Date       | Highlights                                                                                               |
|---------|------------|----------------------------------------------------------------------------------------------------------|
| 0.1.0   | 2025-12-04 | Baseline: hexagonal layout, Feign/WebClient delivery, schedulers + retry, Azure Blob report seam, tests. |

Add future versions here (features, migrations, breaking changes).

## Development Tips
- Keep framework types out of the domain; introduce ports for new integrations.
- Constructor injection everywhere; avoid field injection.
- For new schedulers, mirror the existing flag + cron pattern and add integration coverage.
- When adding a new adapter, pair it with a fake implementation for fast unit tests.
- Document new config keys in this README and in `application.yml` defaults.

## Useful Commands
- Format/verify quickly: `mvn -q -DskipTests compile`
- Package the app: `mvn clean package`
- Inspect effective config: `mvn -q help:effective-pom`

## Support & Links
- Spring Boot reference: https://docs.spring.io/spring-boot/docs/3.5.6/reference/html/
- Resilience4j reference: https://resilience4j.readme.io/docs
- Azure Blob Storage docs: https://learn.microsoft.com/azure/storage/blobs/

---
Happy shipping! Keep the Release Notes fresh and record any new feature flags or ports as you add them.
