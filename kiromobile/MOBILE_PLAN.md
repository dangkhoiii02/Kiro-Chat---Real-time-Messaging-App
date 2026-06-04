# Kế hoạch làm Mobile Kiro Chat

Tài liệu này là roadmap để làm app mobile cho Kiro Chat bằng Flutter. Mục tiêu là giúp một fresher mobile biết cần làm gì trước, học công nghệ nào ở từng giai đoạn, và tránh nhảy vào phần khó quá sớm.

## 1. Mục tiêu MVP

MVP mobile cần chạy được các luồng chính với backend/web hiện tại:

- Đăng nhập bằng Keycloak.
- Xem thông tin profile người dùng hiện tại.
- Xem danh sách cuộc trò chuyện.
- Gửi và nhận tin nhắn realtime giữa mobile và web.
- Quản lý bạn bè: tìm user, gửi lời mời, chấp nhận, từ chối.
- Tạo group chat cơ bản và nhắn tin trong group.
- Có README/hướng dẫn chạy local rõ ràng.

Các tính năng khó như gửi media, block list, presence, video call và voice call để sau MVP.

## 2. Tech stack chính

### 2.1. Core stack cần thêm ngay

| Tech | Dùng để làm gì | Học ở phase |
| --- | --- | --- |
| `flutter_riverpod` | Quản lý state, tách logic khỏi UI | Phase 0, 1, 2 |
| `go_router` | Điều hướng giữa login, shell, chat detail, profile | Phase 0 |
| `dio` | Gọi REST API backend | Phase 1, 2, 3, 4 |
| `flutter_secure_storage` | Lưu access token và refresh token an toàn | Phase 1 |
| `flutter_appauth` | Login Keycloak bằng OAuth2/PKCE | Phase 1 |
| `stomp_dart_client` | Kết nối WebSocket/STOMP để nhận tin realtime | Phase 2 |
| `cached_network_image` | Load avatar, ảnh profile, ảnh message sau này | Phase 1, 2 |
| `intl` | Format ngày giờ tin nhắn | Phase 2 |
| `logger` | Log khi debug API, auth, socket | Phase 0 trở đi |
| `connectivity_plus` | Theo dõi trạng thái mạng, hỗ trợ reconnect | Phase 2, 5 |

### 2.2. Stack để sau MVP

| Tech | Dùng để làm gì | Khi nào dùng |
| --- | --- | --- |
| `image_picker` | Chọn ảnh từ gallery/camera | Media sharing |
| `file_picker` | Chọn file ngoài ảnh | File sharing |
| `permission_handler` | Xin quyền camera, microphone, notification | Media, call |
| `livekit_client` | Video call và voice call | Call phase |
| `flutter_local_notifications` | Notification/incoming call local | Call phase |

Nguyên tắc: tuần đầu không thêm LiveKit, notification, media picker. Cứ làm auth và chat realtime chạy chắc trước.

## 3. Cấu trúc thư mục đề xuất

```text
lib/
  app/
    app.dart
    router.dart
    theme.dart
  core/
    config/
    network/
    storage/
    realtime/
    errors/
  features/
    auth/
    profile/
    chat/
    contact/
    group/
  shared/
    widgets/
    models/
    utils/
```

Quy ước:

- UI screen nằm trong từng feature.
- API/repository nằm trong từng feature nếu chỉ feature đó dùng.
- Code dùng chung như `DioClient`, `TokenStorage`, `StompService` đặt trong `core`.
- Widget dùng lại nhiều nơi đặt trong `shared/widgets`.

## 4. Lộ trình triển khai

### Phase 0: Setup nền móng

Thời gian dự kiến: 1-2 ngày.

Việc cần làm:

- Dọn app counter mặc định của Flutter.
- Setup `go_router` cho các route chính:
  - splash/auth gate
  - login
  - main shell
  - chat detail
- Setup `flutter_riverpod`.
- Tạo theme cơ bản cho app chat.
- Tạo `DioClient` rỗng, `Logger`, config local.
- Tạo bottom navigation: Chat, Contacts, Profile.

Tech cần học ở phase này:

- Flutter project structure.
- `MaterialApp.router`.
- `go_router`.
- `ProviderScope` của Riverpod.
- Cách chia thư mục theo feature.

Done khi:

- App mở lên không còn màn counter.
- Điều hướng được giữa 3 tab chính.
- `flutter analyze` không báo lỗi.

### Phase 1: Auth + Profile

Thời gian dự kiến: 3-4 ngày.

Việc cần làm:

- Login Keycloak bằng `flutter_appauth`.
- Lưu token bằng `flutter_secure_storage`.
- Tự gắn Bearer token vào request qua `Dio` interceptor.
- Khi app mở lại, đọc token và quyết định vào app hay quay về login.
- Gọi `GET /users/me`.
- Hiển thị profile cơ bản: avatar, tên, username/email nếu API có.
- Logout: xóa token, disconnect socket nếu đã connect.

Tech cần học ở phase này:

- OAuth2 Authorization Code + PKCE.
- Keycloak mobile login.
- Secure storage.
- Dio interceptor.
- Riverpod state cho auth session.

Done khi:

- Login thành công bằng account Keycloak local.
- Tắt mở app vẫn giữ session nếu token còn hợp lệ.
- Logout sạch token.
- Profile load từ backend thật, không dùng mock data.

### Phase 2: Chat list + Chat realtime

Thời gian dự kiến: 5-7 ngày.

Việc cần làm:

- Gọi API lấy danh sách conversations.
- Làm màn chat list: avatar, tên, last message, thời gian.
- Mở chat detail.
- Gọi API lấy messages của conversation.
- Làm message bubble trái/phải.
- Gửi message.
- Kết nối STOMP sau khi login.
- Subscribe topic message theo conversation.
- Nhận message realtime và merge vào list.
- Xử lý reconnect khi socket mất kết nối.

Tech cần học ở phase này:

- `stomp_dart_client`.
- WebSocket/STOMP concept: connect, subscribe, publish.
- Riverpod `StateNotifier` hoặc `AsyncNotifier`.
- Scroll controller trong chat.
- Format thời gian bằng `intl`.

Done khi:

- Mobile gửi tin, web nhận realtime.
- Web gửi tin, mobile nhận realtime.
- Tin nhắn không bị duplicate.
- Logout thì STOMP disconnect.
- Mất mạng hoặc backend restart thì có cơ chế reconnect/retry cơ bản.

### Phase 3: Contacts + Friend requests

Thời gian dự kiến: 4-5 ngày.

Việc cần làm:

- Màn Contacts có các tab:
  - Friends
  - Requests
  - Search
- Gọi API danh sách bạn bè.
- Search user với debounce 300-500ms.
- Gửi lời mời kết bạn.
- Chấp nhận/từ chối lời mời.
- Từ friend item có thể mở hoặc tạo direct conversation.

Tech cần học ở phase này:

- Debounce input.
- Optimistic UI mức đơn giản.
- Error state và empty state.
- Tái sử dụng widget list item.

Done khi:

- Search user chạy với API thật.
- Gửi, accept, reject request cập nhật UI đúng.
- Sau khi thành bạn bè có thể mở chat với người đó.

### Phase 4: Group chat cơ bản

Thời gian dự kiến: 4-5 ngày.

Việc cần làm:

- Tạo màn Create Group.
- Chọn ít nhất 2 contacts.
- Nhập tên group.
- Gọi API tạo group.
- Tạo xong điều hướng vào group chat.
- Group chat dùng lại màn chat detail nếu có thể.
- Làm Group Info read-only: tên nhóm, avatar, danh sách thành viên nếu API có.

Tech cần học ở phase này:

- Form validation.
- Multi-select list.
- Tái sử dụng UI giữa direct chat và group chat.
- Điều hướng sau khi mutation thành công.

Done khi:

- Tạo group thành công.
- Vào group chat và gửi tin được.
- Web/mobile thấy tin group realtime.

### Phase 5: Polish + demo

Thời gian dự kiến: 3-4 ngày.

Việc cần làm:

- Sửa UI cho màn hình nhỏ.
- Thêm loading, empty, error state đủ dùng.
- Pull-to-refresh chat list và contacts.
- Snackbar/toast lỗi bằng tiếng Việt.
- Kiểm tra lại toàn bộ flow demo.
- Viết hướng dẫn chạy mobile trong README hoặc tài liệu riêng.

Tech cần học ở phase này:

- Responsive layout trong Flutter.
- Error handling UX.
- Build/debug Android emulator.
- Cách viết checklist test thủ công.

Done khi demo được trong 5 phút:

- Login.
- Xem profile.
- Search và add friend.
- Accept friend request.
- Chat realtime web/mobile.
- Tạo group và chat group.

## 5. Backlog sau MVP

### 5.1. Media sharing

Mục tiêu:

- Chọn ảnh từ gallery.
- Upload ảnh qua backend/MinIO.
- Gửi message kiểu ảnh.
- Render thumbnail trong chat.
- Tap ảnh để xem full screen.

Tech cần dùng:

- `image_picker`.
- `cached_network_image`.
- Multipart upload bằng `dio`.
- Có thể thêm `photo_view` nếu cần zoom ảnh.

### 5.2. Block list

Mục tiêu:

- Xem danh sách user đã block.
- Block/unblock user.
- Revert UI nếu API fail.

Tech cần dùng:

- `dio`.
- Riverpod state cho list.
- Optimistic UI.

### 5.3. Presence

Mục tiêu:

- Hiển thị online/offline ở chat list và chat header.
- Nhận cập nhật presence qua STOMP.

Tech cần dùng:

- `stomp_dart_client`.
- Riverpod shared state.
- `connectivity_plus` để phân biệt mất mạng local và offline thật.

### 5.4. Video call và voice call

Mục tiêu:

- Gửi/nhận call signal qua STOMP.
- Incoming call screen.
- Video call screen.
- Voice call audio-only.
- Xin quyền camera/microphone.

Tech cần dùng:

- `livekit_client`.
- `permission_handler`.
- `flutter_local_notifications`.
- `stomp_dart_client`.

Ghi chú backend:

- Token LiveKit nên hỗ trợ platform identity, ví dụ `?platform=android`, để web và mobile của cùng một user không đá nhau khỏi room.

## 6. Mapping phase và tech cần học

| Phase | Mục tiêu | Tech trọng tâm |
| --- | --- | --- |
| Phase 0 | Setup app shell | Flutter structure, Riverpod, GoRouter |
| Phase 1 | Auth + profile | AppAuth, Keycloak, Secure Storage, Dio |
| Phase 2 | Chat realtime | STOMP, Riverpod async state, Intl |
| Phase 3 | Contacts | Dio, debounce, optimistic UI |
| Phase 4 | Group chat | Form validation, multi-select, route flow |
| Phase 5 | Polish demo | Responsive UI, error handling, manual test |
| Backlog media | Gửi ảnh/file | Image picker, multipart upload |
| Backlog presence | Online/offline | STOMP shared state, connectivity |
| Backlog call | Video/voice call | LiveKit, permissions, notifications |

## 7. Checklist làm việc hằng ngày

Mỗi ngày nên làm theo nhịp này:

1. Chọn một task nhỏ có thể xong trong ngày.
2. Đọc API/web flow liên quan trước khi code.
3. Code UI tối thiểu trước nếu cần nhìn flow.
4. Nối API thật, tránh mock data kéo dài.
5. Tự test happy path.
6. Tự test lỗi cơ bản: backend tắt, token hết hạn, mạng yếu.
7. Ghi lại lỗi gặp trong ngày để hỏi senior.

## 8. Quy tắc cho fresher

- Không làm nhiều feature cùng lúc.
- Không thêm package nếu chưa biết rõ dùng ở phase nào.
- Không để widget quá lớn; nếu một file screen dài khó đọc thì tách widget con.
- Không gọi API trực tiếp trong widget nếu logic bắt đầu phức tạp; đưa vào repository/controller.
- Không hardcode token.
- Không dùng mock data khi backend local đã chạy được.
- Ưu tiên demo end-to-end hơn UI quá đẹp trong giai đoạn MVP.

## 9. Thứ tự package nên thêm

### Thêm ngay khi bắt đầu

```yaml
dependencies:
  flutter_riverpod: ^2.6.1
  go_router: ^14.8.1
  dio: ^5.8.0+1
  flutter_secure_storage: ^9.2.4
  flutter_appauth: ^8.0.3
  stomp_dart_client: ^2.1.3
  cached_network_image: ^3.4.1
  intl: ^0.20.2
  logger: ^2.5.0
  connectivity_plus: ^6.1.3
```

Trước khi thêm, nên kiểm tra version mới nhất tương thích với Flutter/Dart hiện tại bằng:

```bash
flutter pub outdated
```

### Chỉ thêm khi vào backlog tương ứng

```yaml
dependencies:
  image_picker: ^1.1.2
  file_picker: ^8.3.7
  permission_handler: ^11.4.0
  livekit_client: ^2.4.9
  flutter_local_notifications: ^18.0.1
```

Các version trên là mốc khởi đầu để tham khảo. Khi bắt tay làm thật, ưu tiên kiểm tra version mới nhất và đọc breaking changes trước khi nâng.

## 10. Definition of Done cho MVP

MVP được xem là hoàn thành khi:

- App chạy được trên Android emulator.
- Backend local chạy theo `RUNNING_GUIDE.md`.
- Login Keycloak thành công.
- Profile lấy từ backend thật.
- Chat realtime web/mobile hoạt động hai chiều.
- Friend request hoạt động.
- Group chat cơ bản hoạt động.
- Có hướng dẫn chạy mobile rõ ràng.
- `flutter analyze` không có lỗi nghiêm trọng.
- Không còn màn counter demo mặc định của Flutter.
