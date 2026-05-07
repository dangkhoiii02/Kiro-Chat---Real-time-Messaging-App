# Kiro Chat

Kiro Chat is a real-time chat system built as a student capstone-style project. It includes a Spring Boot backend, Angular web app, Android app, and local Docker infrastructure for PostgreSQL, Keycloak, RabbitMQ, MinIO, and LiveKit.

![Real-time messaging demo](./assets/realtime-message-delivery.gif)

## Core Features

- Authentication and user profile management with Keycloak.
- 1:1 chat and group chat.
- Real-time message delivery through WebSocket/STOMP and RabbitMQ.
- Friend requests, contact list, and block list.
- Group creation and group member management.
- Media/file sharing through backend upload APIs and MinIO.
- User presence tracking.
- Video call token generation with LiveKit.
- Android client that connects to the same backend APIs.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security, Spring Data JPA, WebSocket/STOMP |
| Web | Angular, RxJS, RxStomp |
| Mobile | Android Java, XML layouts, Retrofit, OkHttp, ViewModel/LiveData |
| Auth | Keycloak |
| Storage | PostgreSQL, MinIO |
| Messaging | RabbitMQ |
| Calls | LiveKit |
| Local dev | Docker Compose |

## Project Structure

```text
.
├── kiro-backend/      # Spring Boot API, domain logic, WebSocket, auth, upload, calls
├── kiro-frontend/     # Angular web client
├── kiro-mobile/       # Android app for mobile use
├── kiro-api/          # Bruno/API request collection for testing backend endpoints
├── local-dev/         # Docker Compose and local service configuration
├── assets/            # Screenshots and demo GIFs for README/review
├── README.md
└── RUNNING_GUIDE.md
```

Generated files are intentionally excluded from the repo: `node_modules`, `.gradle`, `build`, `target`, logs, local IDE metadata, and local database backups.

## Quick Start

Read [RUNNING_GUIDE.md](./RUNNING_GUIDE.md) for the full step-by-step setup.

Short version:

```bash
cd local-dev
docker compose up -d
```

```bash
cd kiro-backend
cp .env.example .env
./gradlew bootRun
```

```bash
cd kiro-frontend
npm install
npm start
```

Open the web app at http://localhost:4200.

For Android, open `kiro-mobile` in Android Studio and run the app on an emulator. The mobile app uses `10.0.2.2` to reach services running on your computer.

## Useful URLs

| Service | URL | Account |
|---|---|---|
| Web app | http://localhost:4200 | Keycloak user |
| Backend API | http://localhost:8080 | Bearer token |
| Keycloak Admin | http://localhost:9093 | admin / admin |
| RabbitMQ | http://localhost:15672 | admin / admin |
| MinIO Console | http://localhost:9001 | admin / password |
| LiveKit | ws://localhost:7880 | dev key/secret in `local-dev/livekit.yaml` |

## Build Checks

```bash
cd kiro-backend
./gradlew compileJava
```

```bash
cd kiro-mobile
./gradlew assembleDebug
```

```bash
cd kiro-frontend
npm install
npm run build
```

## Feature Preview

**Messaging**

![Messaging](./assets/messaging.png)

**Group chat**

![Group messaging](./assets/group-messaging.gif)

**Contacts and friend requests**

![Contact manager](./assets/contact-manager.png)

**Media sharing**

![Media sharing](./assets/media-sharing.gif)

**Profile and authentication**

![Profile](./assets/profile.png)

## License

Licensed under the Apache License, Version 2.0.
# Kiro-Chat---Real-time-Messaging-App
# Kiro-Chat---Real-time-Messaging-App
