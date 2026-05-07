# Running Guide

This guide is for running Kiro Chat locally on a student laptop.

## Requirements

- Java 17+
- Node.js 20 LTS recommended
- Docker Desktop
- Android Studio, if you want to run the mobile app

## 1. Start Local Services

```bash
cd local-dev
docker compose up -d
```

This starts:

- PostgreSQL on `localhost:5432`
- Keycloak on `localhost:9093`
- RabbitMQ on `localhost:5672` and management UI on `localhost:15672`
- MinIO on `localhost:9000` and console on `localhost:9001`
- LiveKit on `localhost:7880`

## 2. Enable RabbitMQ STOMP

Run once after RabbitMQ starts:

```bash
docker exec kiro-mq rabbitmq-plugins enable rabbitmq_stomp rabbitmq_web_stomp rabbitmq_management
docker restart kiro-mq
```

## 3. Create MinIO Bucket

Open http://localhost:9001.

Login:

```text
Username: admin
Password: password
```

Create this bucket:

```text
kiro-bucket-01
```

## 4. Configure Backend

```bash
cd kiro-backend
cp .env.example .env
```

For Android emulator testing, set these values in `kiro-backend/.env`:

```env
KEYCLOAK_ISSUER_URI_HOST=http://10.0.2.2:9093
KEYCLOAK_JWK_SET_URI_HOST=http://localhost:9093
UPLOAD_MINIO_URL_PREFIX=http://10.0.2.2:9000
LIVEKIT_URL=ws://10.0.2.2:7880
```

For web-only local testing, the default `.env.example` values are enough.

## 5. Run Backend

```bash
cd kiro-backend
./gradlew bootRun
```

Backend URL:

```text
http://localhost:8080
```

## 6. Run Frontend

```bash
cd kiro-frontend
npm install
npm start
```

Frontend URL:

```text
http://localhost:4200
```

## 7. Run Android App

1. Open Android Studio.
2. Choose `Open` and select `kiro-mobile`.
3. Wait for Gradle sync.
4. Start an Android emulator.
5. Run the app.

The Android app should call backend services through:

```text
Backend:  http://10.0.2.2:8080
Keycloak: http://10.0.2.2:9093
MinIO:    http://10.0.2.2:9000
LiveKit:  ws://10.0.2.2:7880
```

## Common Commands

Backend compile:

```bash
cd kiro-backend
./gradlew compileJava
```

Mobile debug build:

```bash
cd kiro-mobile
./gradlew assembleDebug
```

Stop local services:

```bash
cd local-dev
docker compose down
```

Reset local Docker data:

```bash
cd local-dev
docker compose down -v
docker compose up -d
```

## Service Accounts

| Service | URL | Account |
|---|---|---|
| Keycloak Admin | http://localhost:9093 | admin / admin |
| RabbitMQ | http://localhost:15672 | admin / admin |
| MinIO | http://localhost:9001 | admin / password |


Terminal 1 — Docker (local services)

cd local-dev
docker compose up -d
Sau khi Docker khởi động xong, chạy một lần để enable STOMP:

docker exec kiro-mq rabbitmq-plugins enable rabbitmq_stomp rabbitmq_web_stomp rabbitmq_management
docker restart kiro-mq
Terminal 2 — Backend
lsof -ti :8080 | xargs kill -9

cd kiro-backend
./gradlew bootRun
Terminal 3 — Frontend

cd kiro-frontend
npm start
Mở trình duyệt tại http://localhost:4200