# Data Distributor
![Project Logo](https://via.placeholder.com/150?text=Data+Distributor)

Minimal Spring Boot service that fetches signal-related data, applies CEH/DIAL decision logic, writes audit batches, and uploads morning reports.

## Architecture
- **Domain**: pure business logic (services, ports, use cases, aggregates).  
- **App (Spring)**: configuration, `DataDistributorProperties`, scheduler wiring, bean factories (includes `GlobalTimeZoneConfig` now).  
- **Adapters**: `inadapter` (schedulers/controllers) + `outadapter` (JPA, web clients, file storage).

### Key additions
* `GlobalTimeZoneConfig` locks the JVM default zone to `Europe/Amsterdam` so any default `Clock` calls follow CET, while `DialSignalDataProviderScheduler` explicitly schedules with `zone = "Europe/Amsterdam"`.
* `CehResponseInitialEventEntity` replaces the previous entity name to make the naming consistent with other JPA artifacts; repositories and tests were updated accordingly.

## Running

```bash
mvn -pl data-distributor -am clean test
```

## Behavior notes
* Scheduler cron times are interpreted in CET regardless of the host zone thanks to the `zone` attribute and the global timezone override.  
* Domain logic still relies on hosted `Clock` injections for determinism; the global timezone is only a safety net for legacy code using `LocalDate.now()` without a clock.

## Release Notes
| Version | Date | Highlights |
|---------|------|------------|
| 0.1.0 | 2025-12-05 | Added CET-aware scheduler wiring & documented timezone behavior. Renamed `CehResponseInitialEvent` entity for clarity. |

Keeping this document updated ensures every module consumer knows where the time configuration lives and how the two-zone decisions work.
