# Running Guide
## Service Accounts

| Service | URL | Account |
|---|---|---|
| Keycloak Admin | http://localhost:9093 | admin / admin |
| RabbitMQ | http://localhost:15672 | admin / admin |
| MinIO | http://localhost:9001 | admin / password |

Bước 1:
Terminal 1 — Docker (local services)

cd local-dev
docker compose up -d

Sau khi Docker khởi động xong, chạy một lần để enable STOMP:

docker exec kiro-mq rabbitmq-plugins enable rabbitmq_stomp rabbitmq_web_stomp rabbitmq_management
docker restart kiro-mq

Bước 2:
Terminal 2 — Backend
lsof -ti :8080 | xargs kill -9

cd kiro-backend
./gradlew bootRun

Bước 3:
Terminal 3 — Frontend

cd kiro-frontend
npm start
Mở trình duyệt tại http://localhost:4200

Bước 4:
cp /Users/dangkhoii/Downloads/kiro-chat-main/config.yml ~/.cloudflared/config.yml

cloudflared tunnel run kiro-tunnel
