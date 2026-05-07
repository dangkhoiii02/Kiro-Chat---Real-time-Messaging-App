# Local Development Services

This folder contains only the services needed to run the project locally.

## Files

```text
local-dev/
├── compose.yaml              # PostgreSQL, Keycloak, RabbitMQ, MinIO, LiveKit
├── livekit.yaml              # LiveKit dev key/secret
├── keycloak-data/import/     # Kiro realm and users imported by Keycloak
├── scripts/                  # Optional SQL/import helpers
└── seed/                     # Optional database seed helpers
```

## Start

```bash
docker compose up -d
```

## Stop

```bash
docker compose down
```

## Reset

```bash
docker compose down -v
docker compose up -d
```
