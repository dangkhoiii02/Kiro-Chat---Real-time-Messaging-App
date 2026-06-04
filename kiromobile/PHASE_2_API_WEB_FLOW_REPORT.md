# Phase 2 API/Web Flow Report

Tài liệu này ghi lại flow chat hiện tại của web/backend và các model mobile cần dùng cho Phase 2. Mục tiêu là để mobile không tự đoán API, mà bám theo contract đang chạy ở web Angular và backend Spring Boot.

## 1. Phạm Vi Phase 2

Phase 2 tập trung vào chat cơ bản:

- Lấy danh sách conversations.
- Mở một conversation và lấy messages.
- Gửi tin nhắn 1-1.
- Gửi tin nhắn group.
- Nhận tin nhắn realtime qua STOMP/WebSocket.
- Gửi tín hiệu đã nhận tin nhắn.
- Gửi tín hiệu đã xem tin nhắn.

Chưa xử lý sâu trong phase này:

- Media upload UI hoàn chỉnh.
- Presence online/offline hoàn chỉnh.
- Typing indicator.
- Message edit/delete.
- Call message.

## 2. Nguồn Đối Chiếu

Các nguồn đã đọc:

- Web Angular:
  - `kiro-frontend/src/app/features/chat/service/conversation-api.service.ts`
  - `kiro-frontend/src/app/features/chat/components/direct-conversation/direct-message.api.ts`
  - `kiro-frontend/src/app/features/chat/components/direct-conversation/direct-message.subscriber.ts`
  - `kiro-frontend/src/app/features/chat/components/group-conversation/group-message.api.ts`
  - `kiro-frontend/src/app/features/chat/components/group-conversation/group-message.subscriber.ts`
  - `kiro-frontend/src/app/features/chat/models/chat.model.ts`
  - `kiro-frontend/src/app/features/chat/models/message.model.ts`
- Backend Spring:
  - `ConversationMessageResource`
  - `DirectMessageResource`
  - `GroupMessageResource`
  - `WsPaths`
  - payloads `RestChat`, `RestMessage`, `RestConversationMessageList`, `RestSendMessageRequest`, `RestAttachment`
- OpenAPI:
  - `GET /conversations`
  - `POST /conversations`
  - `POST /conversations/group`
  - `GET /conversations/user/{userId}`

Ghi chú: `kiro-api/open-api.json` hiện chưa expose đầy đủ message endpoints, nên phần message flow được xác nhận thêm từ web Angular và backend resource classes.

## 3. OpenAPI JSON Là Gì?

OpenAPI JSON là một file mô tả API theo format chuẩn OpenAPI. Có thể hiểu đơn giản nó là "bản hợp đồng API" mà backend công bố cho frontend/mobile biết:

- Có endpoint nào.
- Endpoint dùng method gì: `GET`, `POST`, `PATCH`, `DELETE`.
- Endpoint cần path/query/body gì.
- Response trả về dạng nào.
- Có cần token không.

Trong đồ án này, file đó nằm ở backend và được dùng như tài liệu API tự động. Nếu backend cấu hình đầy đủ Swagger/OpenAPI, mobile có thể nhìn vào đó để biết API phải gọi như thế nào.

Tuy nhiên hiện tại file OpenAPI của project chưa có đủ message endpoints. Vì vậy với Phase 2, mình không chỉ đọc OpenAPI mà còn đọc thêm:

- Web Angular đang gọi API nào.
- Backend controller/resource đang expose endpoint nào.
- Payload backend đang trả field gì.

Nói cách khác: OpenAPI là nguồn tham khảo tốt, nhưng trong project này chưa đủ cho phần chat message, nên web/backend code mới là nguồn chính xác hơn.

## 4. REST API Flow

### 4.1. Chat List

Endpoint:

```http
GET /api/v1/conversations?pageable=...&q=...
Authorization: Bearer <access_token>
```

Web dùng trong `ConversationApi` thông qua backend `getUserChatList`.

Response chính:

```text
RestUserChatList
  conversations: Page<RestChat>
```

`RestChat` trả các field quan trọng:

```text
conversationId
conversationName
avatarUrl
lastMessage
unreadCount
isOnline
isGroup
remoteUserId
isFollowingUp
isArchived
```

Mobile model tương ứng:

```text
UserChatList
PageResponse<Conversation>
Conversation
```

### 4.2. Get Messages

Endpoint:

```http
GET /api/v1/conversations/{conversationId}/messages?before={messageId}&limit=20
Authorization: Bearer <access_token>
```

Web gọi trong:

```text
ConversationApi.getMessages()
```

Backend xử lý trong:

```text
ConversationMessageResource.getConversationMessages()
```

Response:

```text
RestConversationMessageList
  messages: List<RestConversationMessage>
  hasMore: boolean
  nextBeforeMessageId: UUID?
```

Mobile model tương ứng:

```text
ConversationMessageList
ChatMessage
```

### 4.3. Send Direct Message

Endpoint:

```http
POST /api/v1/messages/individual/send
Authorization: Bearer <access_token>
Content-Type: application/json
```

Body:

```json
{
  "conversationId": "uuid",
  "content": "Hello",
  "type": "text",
  "replyToMessageId": null,
  "attachment": null
}
```

Web gọi trong:

```text
DirectMessageApi.sendMessage()
```

Backend xử lý trong:

```text
DirectMessageResource.sendIndividualMessage()
```

Response:

```text
RestMessage
```

Mobile model tương ứng:

```text
SendMessageRequest
ChatMessage
```

### 4.4. Send Group Message

Endpoint:

```http
POST /api/v1/messages/group/send
Authorization: Bearer <access_token>
Content-Type: application/json
```

Body giống direct message:

```json
{
  "conversationId": "uuid",
  "content": "Hello group",
  "type": "text",
  "replyToMessageId": null,
  "attachment": null
}
```

Web gọi trong:

```text
GroupMessageApi.sendMessage()
```

Backend xử lý trong:

```text
GroupMessageResource.sendGroupMessage()
```

Response:

```text
RestMessage
```

### 4.5. Ack Received

Direct:

```http
PATCH /api/v1/messages/individual/receive
Authorization: Bearer <access_token>
Content-Type: application/json
```

Group:

```http
PATCH /api/v1/messages/group/receive
Authorization: Bearer <access_token>
Content-Type: application/json
```

Body:

```json
{
  "messageId": "uuid"
}
```

Ack Received nghĩa là app báo cho server biết: "Tôi đã nhận được message này rồi".

Nó khác với "seen":

- `received`: tin nhắn đã tới thiết bị/app của người nhận.
- `seen`: người dùng đã mở cuộc trò chuyện và nhìn thấy tin nhắn.

Ví dụ đời thường:

- Người A gửi tin nhắn.
- Server đẩy tin nhắn qua WebSocket tới người B.
- App của người B nhận được message.
- App của người B gọi API ack received.
- Server biết message đã được giao tới app người B và có thể cập nhật trạng thái `delivered`.

Web gọi ack sau khi nhận realtime message:

```text
DirectMessageSubscriber.ackReceiveMessage()
GroupMessageSubscriber.ackReceiveMessage()
```

Mobile model tương ứng:

```text
AcknowledgeReceiveMessageRequest
```

### 4.6. Mark Seen

Endpoint:

```http
PATCH /api/v1/conversations/{conversationId}/messages/seen
Authorization: Bearer <access_token>
Content-Type: application/json
```

Body:

```json
{
  "conversationId": "uuid",
  "lastSeenMessageId": "uuid"
}
```

Mark Seen nghĩa là app báo cho server biết: "Người dùng đã xem tới message này rồi".

Thông thường mobile sẽ gọi API này khi user đang mở màn hình chat detail. Không nên gọi chỉ vì app nhận message realtime, vì nhận được chưa chắc user đã nhìn thấy.

Web gọi trong:

```text
DirectMessageApi.markAsSeen()
GroupMessageApi.markAsSeen()
```

Mobile model tương ứng:

```text
MarkMessageSeenRequest
```

## 5. STOMP/WebSocket Flow

Web STOMP endpoint:

```text
ws://localhost:8080/api/v1/ws
```

Mobile emulator:

```text
ws://10.0.2.2:8080/api/v1/ws
```

Connect header cần có:

```text
Authorization: Bearer <access_token>
```

Web đang inject token trong:

```text
RxStompAdapterService.configure()
RxStompAdapterService.publish()
```

### 5.1. Direct Message Subscriptions

Direct incoming message:

```text
/user/queue/messages.receive
```

Message sent notification:

```text
/user/queue/messages.sent
```

Delivered notification:

```text
/user/queue/messages.delivered
```

Seen notification:

```text
/user/queue/messages.seen
```

Web filters by `conversationId` when it needs messages for one room.

### 5.2. Group Message Subscriptions

Group incoming message:

```text
/topic/messages.receive-{conversationId}
```

Group sent/delivered/seen still use user queue:

```text
/user/queue/messages.sent
/user/queue/messages.delivered
/user/queue/messages.seen
```

Important detail from web:

- For group receive, web ignores messages where `senderId == currentUserId`.
- After receiving a group message, web calls `/messages/group/receive`.

## 6. Giải Thích Các Thực Thể Model

### 6.1. `Conversation`

`Conversation` là một cuộc trò chuyện.

Trong app chat, màn hình danh sách chat không hiển thị từng message riêng lẻ trước. Nó hiển thị từng cuộc trò chuyện, ví dụ:

- Chat với Nguyễn Văn A.
- Nhóm "Mobile Team".
- Nhóm "Kiro Chat".

Mỗi item đó chính là một `Conversation`.

Model này giúp mobile biết:

- Tên cuộc trò chuyện là gì.
- Avatar hiển thị là gì.
- Đây là chat 1-1 hay group.
- Tin nhắn cuối là gì.
- Có bao nhiêu tin chưa đọc.
- Nếu là chat 1-1 thì người bên kia là ai.

### 6.2. `ChatMessage`

`ChatMessage` là một tin nhắn cụ thể trong một conversation.

Ví dụ trong conversation "Mobile Team", có nhiều message:

- "Anh em làm phase 2 nhé"
- "Ok anh"
- "Em push code rồi"

Mỗi dòng như vậy là một `ChatMessage`.

Model này dùng cho:

- Render bubble trong màn hình chat detail.
- Render last message ở màn hình chat list.
- Parse message nhận qua WebSocket.
- Cập nhật trạng thái sent/delivered/seen.

### 6.3. `ConversationMessageList`

`ConversationMessageList` là response khi mobile mở một conversation và lấy danh sách message.

Nó không chỉ chứa `messages`, mà còn chứa thông tin phân trang:

- `hasMore`: còn message cũ hơn không.
- `nextBeforeMessageId`: nếu muốn load thêm message cũ hơn thì lấy id này truyền vào query `before`.

Ví dụ:

- Lần đầu mở chat: lấy 20 message mới nhất.
- User kéo lên trên: gọi API lần nữa với `before=nextBeforeMessageId`.
- Server trả thêm 20 message cũ hơn.

### 6.4. `UserChatList`

`UserChatList` là response của API lấy danh sách conversation của user hiện tại.

Nó bọc danh sách conversation trong `PageResponse<Conversation>` vì backend dùng Spring Data Page.

Hiểu đơn giản:

- `UserChatList` là cái hộp lớn backend trả về.
- Bên trong có `conversations`.
- `conversations` không chỉ có list data, mà còn có thông tin page.

### 6.5. `PageResponse<T>`

`PageResponse<T>` là model generic để parse dữ liệu phân trang từ backend Spring.

Ký hiệu `<T>` nghĩa là loại item bên trong có thể thay đổi.

Ví dụ:

```text
PageResponse<Conversation>
```

Nghĩa là: một trang dữ liệu, trong đó mỗi item là `Conversation`.

Nếu sau này backend có API trả danh sách user theo page, mình có thể có:

```text
PageResponse<User>
```

Vậy `PageResponse<T>` giúp mình không phải viết lại model phân trang nhiều lần.

Các field thường gặp:

- `content`: danh sách item thật sự.
- `totalElements`: tổng số item backend có.
- `totalPages`: tổng số trang.
- `pageNumber`: đang ở trang số mấy.
- `pageSize`: mỗi trang bao nhiêu item.
- `last`: đây có phải trang cuối chưa.

### 6.6. `SendMessageRequest`

`SendMessageRequest` là body mobile gửi lên server khi user bấm nút gửi tin nhắn.

Nó trả lời các câu hỏi:

- Gửi vào conversation nào?
- Nội dung text là gì?
- Loại message là text/file/image/video/call?
- Có reply message nào không?
- Có attachment không?

### 6.7. `Attachment`

`Attachment` là thông tin file/media gắn kèm message.

Ví dụ user gửi ảnh, file PDF, video:

- File nằm ở URL nào.
- Tên file là gì.
- Loại file là gì.
- Kích thước bao nhiêu.

Phase 2 trước mắt có thể ưu tiên text chat. Nhưng model này được chuẩn bị vì backend/web đã có field `attachment`.

### 6.8. `AcknowledgeReceiveMessageRequest`

`AcknowledgeReceiveMessageRequest` là body rất nhỏ dùng khi app báo server rằng một message đã được nhận.

Field duy nhất:

```text
messageId
```

Vì server chỉ cần biết message nào đã được nhận.

### 6.9. `MarkMessageSeenRequest`

`MarkMessageSeenRequest` là body dùng khi app báo server rằng user đã xem tới một message trong conversation.

Nó cần:

- `conversationId`: đang xem cuộc trò chuyện nào.
- `lastSeenMessageId`: đã xem tới message nào.

Lý do không chỉ gửi `messageId`: backend thường cần biết message đó thuộc conversation nào để cập nhật trạng thái đọc đúng phạm vi.

## 7. Model Fields Cho Mobile

### 7.1. `Conversation`

File:

```text
lib/features/chat/data/models/conversation.dart
```

Fields:

```text
conversationId: String
conversationName: String
avatarUrl: String?
lastMessage: ChatMessage?
unreadCount: int
isOnline: bool
isGroup: bool
remoteUserId: String?
isFollowingUp: bool?
isArchived: bool?
```

Dùng cho:

- Chat list item.
- Điều hướng vào direct/group chat.
- Biết conversation là group hay 1-1.

### 7.2. `ChatMessage`

File:

```text
lib/features/chat/data/models/chat_message.dart
```

Fields:

```text
messageId: String
conversationId: String
senderId: String
type: MessageType
content: String?
mediaUrl: String?
mediaName: String?
messageState: MessageState
timestamp: DateTime
replyToMessageId: String?
isDeleted: bool
owner: bool?
```

Dùng cho:

- Message bubble.
- Last message preview.
- Realtime incoming message.
- Sent/delivered/seen updates.

### 7.3. `ConversationMessageList`

File:

```text
lib/features/chat/data/models/conversation_message_list.dart
```

Fields:

```text
messages: List<ChatMessage>
hasMore: bool
nextBeforeMessageId: String?
```

Dùng cho:

- Initial load chat detail.
- Load older messages bằng cursor `before`.

### 7.4. `UserChatList`

File:

```text
lib/features/chat/data/models/user_chat_list.dart
```

Fields:

```text
conversations: PageResponse<Conversation>
```

Dùng cho:

- Parse response `GET /conversations`.

### 7.5. `PageResponse<T>`

File:

```text
lib/features/chat/data/models/page_response.dart
```

Fields:

```text
content: List<T>
totalElements: int
totalPages: int
pageNumber: int
pageSize: int
last: bool
```

Dùng cho:

- Spring Data `Page<T>`.
- Chat list pagination.

### 7.6. `SendMessageRequest`

File:

```text
lib/features/chat/data/models/send_message_request.dart
```

Fields:

```text
conversationId: String
type: MessageType
content: String?
replyToMessageId: String?
attachment: Attachment?
```

Dùng cho:

- `POST /messages/individual/send`
- `POST /messages/group/send`

### 7.7. `Attachment`

File:

```text
lib/features/chat/data/models/attachment.dart
```

Fields:

```text
url: String
fileName: String
fileType: String
fileSize: int
```

Dùng sau MVP text chat, khi làm media/file sharing.

### 7.8. `AcknowledgeReceiveMessageRequest`

File:

```text
lib/features/chat/data/models/acknowledge_receive_message_request.dart
```

Fields:

```text
messageId: String
```

Dùng cho:

- `PATCH /messages/individual/receive`
- `PATCH /messages/group/receive`

### 7.9. `MarkMessageSeenRequest`

File:

```text
lib/features/chat/data/models/mark_message_seen_request.dart
```

Fields:

```text
conversationId: String
lastSeenMessageId: String
```

Dùng cho:

- `PATCH /conversations/{conversationId}/messages/seen`

## 8. Enum Mapping

### `MessageType`

Backend enum hiện tại:

```text
text
file
image
video
call
```

Mobile có thêm `unknown` để app không crash nếu backend thêm type mới.

### `MessageState`

Backend enum hiện tại:

```text
prepare
sent
delivered
seen
```

Mobile có thêm:

```text
failed
unknown
```

`failed` dùng cho optimistic UI sau này; `unknown` để parse an toàn.

## 9. Flow Mobile Nên Làm Tiếp

Thứ tự implement Phase 2 nên là:

1. Thêm Dio interceptor tự gắn Bearer token cho mọi API protected.
2. Tạo `ConversationRepository.getConversations()`.
3. Tạo `ChatListController` và render `ChatListPage` bằng API thật.
4. Tạo `MessageRepository.getMessages()`.
5. Tạo `ChatDetailPage` đọc messages theo `conversationId`.
6. Implement `sendMessage()` cho direct/group dựa trên `Conversation.isGroup`.
7. Tạo `StompService` connect với Authorization header.
8. Subscribe:
   - direct: `/user/queue/messages.receive`
   - group: `/topic/messages.receive-{conversationId}`
   - status: `/user/queue/messages.sent`, `/user/queue/messages.delivered`, `/user/queue/messages.seen`
9. Khi nhận message realtime, gọi ack receive tương ứng.

## 10. Ghi Chú Kỹ Thuật

- OpenAPI JSON hiện thiếu message endpoints; không nên chỉ dựa vào `open-api.json` cho Phase 2.
- Web hiện đang dùng REST để send message, STOMP để receive/status.
- Mobile cũng nên đi theo hướng đó trước, không publish message trực tiếp qua STOMP khi web/backend đang dùng REST send.
- Group realtime topic phụ thuộc `conversationId`, nên khi vào group chat hoặc có list group conversation thì phải subscribe đúng topic.
- Direct realtime dùng user queue chung, sau đó filter theo `conversationId`.
