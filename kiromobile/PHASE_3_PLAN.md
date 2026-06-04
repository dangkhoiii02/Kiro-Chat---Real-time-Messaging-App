# Phase 3 Plan: Contacts + Friend Requests

## Summary

Mục tiêu Phase 3 là làm xong luồng Contacts end-to-end trong hôm nay: xem bạn bè, xem lời mời kết bạn, search user, gửi/accept/reject request, và mở hoặc tạo direct conversation để chat với bạn bè. Phạm vi chưa gồm block/unfriend/presence/notification background.

Backend contract đã xác nhận:

- `GET /friends?q=...`
- `GET /users/search?q=...`
- `GET /users/suggests?q=...`
- `GET /contact-requests`
- `POST /contact-requests/{userId}`
- `DELETE /contact-requests/{userId}`
- `POST /contact-requests/user/{requestUserId}/accept`
- `POST /contact-requests/user/{requestUserId}/reject`
- `GET /conversations/user/{userId}`
- fallback `POST /conversations?userId={userId}` nếu chưa có conversation

## Key Changes

- Tạo feature `contact` theo cùng pattern đã dùng ở chat:
  - models: `ContactProfile`, `Friend`, `ContactRequest`, `FriendshipStatus`, list wrappers dùng `PageResponse<T>`
  - repository: `ContactRepository` gọi friends/search/suggests/contact-requests/open-chat
  - providers: `ContactsController`, `FriendRequestsController`, `UserSearchController`
  - UI: một `ContactsPage` có 3 tab `Friends`, `Requests`, `Search`

- Thêm route và navigation:
  - thêm `appContactsRoute = /app/contacts`
  - bottom navigation tab `CRM` hiện tại đổi thành `Contacts`
  - từ ChatList/Profile có thể đi tới Contacts bằng bottom nav
  - khi bấm message/friend item: gọi `GET /conversations/user/{userId}`, nếu 404 thì gọi `POST /conversations?userId={userId}`, sau đó điều hướng tới `ChatDetailPage`

- UI behavior:
  - Friends tab: load `GET /friends`, search friends bằng query, bấm item để mở chat
  - Requests tab: load `GET /contact-requests`, có nút accept/reject
  - Search tab: debounce 400ms, gọi `GET /users/search?q=...`, action button theo `friendshipStatus`
  - Status mapping:
    - `not_connected` / `not_determined`: hiện `Add`
    - `friend_request_sent`: hiện `Pending`, có thể cancel bằng `DELETE /contact-requests/{userId}`
    - `friend_request_received`: hiện `Accept` và `Reject`
    - `friends`: hiện `Message`
    - `blocked` / `blocked_by`: disable action

- State update:
  - Sau send request: update item status thành `friend_request_sent`
  - Sau cancel/reject: update item status thành `not_connected`
  - Sau accept: update item status thành `friends`, refresh Friends + Requests
  - Nếu API lỗi: giữ state cũ và hiện error/snackbar text đơn giản

## Test Plan

- Model tests:
  - parse `RestContactProfileList`, `RestFriendList`, `RestContactRequestList`
  - parse `FriendshipStatus` đúng các value backend trả về

- Repository tests:
  - `getFriends` gọi đúng `/friends?q=...`
  - `searchUsers` gọi đúng `/users/search?q=...`
  - `getContactRequests` gọi đúng `/contact-requests`
  - `send/cancel/accept/reject` gọi đúng endpoint
  - `openOrCreateDirectConversation` gọi `GET /conversations/user/{userId}` và fallback `POST /conversations?userId=...` khi 404

- Provider tests:
  - load friends success/failure
  - search debounce không cần unit test timing phức tạp, nhưng controller phải có method search rõ ràng
  - send request update status
  - accept/reject request update list
  - open chat trả về conversation reference để UI điều hướng

- Verification:
  - `flutter analyze`
  - `flutter test`
  - manual test bằng 2 account: search user, send request, account kia accept, quay lại account đầu mở chat

## Assumptions

- Làm MVP Phase 3 trong hôm nay, nên chưa làm unfriend/block/presence.
- Chưa thêm package debounce riêng; dùng `Timer` trong UI/controller để tránh dependency mới.
- Contacts UI ưu tiên chạy đúng flow hơn là polish sâu.
- Nếu `GET /conversations/user/{userId}` trả lỗi không phải 404 thì không fallback create, mà hiển thị lỗi.
- Sau khi accept friend request, app refresh lại Friends và Requests để tránh state lệch với backend.
