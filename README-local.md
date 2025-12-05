# Local Development Stack with Docker

This repository ships with a self-contained Docker stack that brings up:

1. **SQL Server** with Flyway-driven schema + seed data matching the production tables.
2. **A dummy CEH service** that listens on `/create-signal/write-signal` and returns a random `ceh_event_id`.
3. **The Spring Boot `data-distributor` application** wired to the SQL Server + dummy CEH.

## Prerequisites

- Docker & Docker Compose installed locally.
- At least 4GB free memory for the containers.

## How to start the stack

```bash
cd data-distributor
docker compose up --build
```

The Compose file will:

- Start SQL Server and let a helper container create the `data_distributor` database.
- Run Flyway (from `docker/sqlserver/migration/V1__init.sql`) to build the tables + insert five signals with four events each.
- Build and start the dummy CEH service (`/create-signal/write-signal` returns random IDs).
- Build the app image and run it with `SPRING_DATASOURCE_URL` pointing to the SQL Server and `iagPath` pointing to the CEH service inside the Docker network.

The Spring app will be exposed on **http://localhost:8080** and the dummy CEH on **http://localhost:8081**.

## What data is seeded?

- `product_risk_monitoring` / `product_configuration`: two products (GRV 1 and 2) with every flag set to `Y`.
- `account_balance_overview`: five agreements (1001–1005) referencing the GRVs.
- `signal`: five signals spanning 1 Jan–4 Jan 2025.
- `signal_events`: each signal has four events (`OVERLIMIT_SIGNAL`, `FINANCIAL_UPDATE`, `PRODUCT_SWAP`, `OUT_OF_OVERLIMIT`) with incremental timestamps.

After the stack is up, you can inspect the seeded data with `sqlcmd` (e.g., from inside the SQL Server container):

```bash
docker exec -it data-distributor-sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P YourStrong!Passw0rd -d data_distributor -Q "SELECT TOP 5 signal_id, agreement_id, signal_start_date FROM signal ORDER BY signal_id"
```

## Trigger the app flow manually

You can call the application endpoint that kicks off the signal delivery (same as in the tests), or you can just let the scheduler run. The dummy CEH service will log incoming payloads with a random response.

## Troubleshooting

- Flyway errors: check the volume under `docker/sqlserver/migration` for the SQL script and rerun with `docker compose up flyway`.
- Need to reset DB? Tear down the stack and remove the volume with:
  ```bash
docker compose down && rm -rf docker/sqlserver/data/*
```

## Cleaning up

```bash
docker compose down
docker volume prune
```

This local stack keeps production code untouched while making it easy to run the app + DB locally.
