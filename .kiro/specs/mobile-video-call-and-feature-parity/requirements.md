# Requirements Document

## Introduction

Tính năng này bổ sung khả năng gọi video/voice trên Android mobile app (Kiro Mobile) và đảm bảo tính năng ngang bằng với web client. Hiện tại, mobile app đã có chat 1-1, group chat cơ bản, danh bạ và friend requests, nhưng chưa có video/voice call. Ngoài ra, một số tính năng web (group profile, block list, media sharing, user presence) chưa được triển khai trên mobile.

Vấn đề cốt lõi cần giải quyết: khi mobile kết nối vào cùng STOMP server, LiveKit room token phải được cấp đúng cho từng platform (web và Android) để cả hai có thể tham gia cùng một phòng gọi mà không xung đột.

Tech stack:
- **Backend**: Spring Boot 3, LiveKit server tại `ws://10.0.2.2:7880` (emulator)
- **Mobile**: Android Java, Retrofit, OkHttp, STOMP WebSocket (StompProtocolAndroid), RxJava2
- **Web**: Angular với LiveKit JS SDK (đã hoạt động)
- **Call signaling**: STOMP topic `/user/queue/calls.signal`, publish to `/app/calls.signal`

---

## Glossary

- **LiveKit_SDK**: Thư viện Android chính thức của LiveKit để xử lý WebRTC media (livekit-android-sdk)
- **Call_Signaling_Manager**: Thành phần Android quản lý luồng tín hiệu cuộc gọi qua STOMP (OFFER/ANSWER/REJECT/END)
- **Video_Call_Activity**: Activity Android chuyên dụng hiển thị giao diện gọi video/voice
- **Incoming_Call_Service**: Android Foreground Service hiển thị thông báo cuộc gọi đến khi app ở background
- **LiveKit_Token_API**: REST endpoint `/api/v1/calls/{conversationId}/token` trên Spring Boot backend trả về JWT token để tham gia LiveKit room
- **StompManager**: Singleton quản lý kết nối STOMP WebSocket trên Android (đã tồn tại)
- **CallSignalMessage**: Đối tượng JSON trao đổi qua STOMP với các trường: `type`, `conversationId`, `targetUserId`, `callerId`, `callerName`, `callerAvatar`, `withVideo`
- **Signal_Type**: Enum gồm `OFFER`, `ANSWER`, `REJECT`, `END`
- **Group_Profile**: Màn hình hiển thị thông tin nhóm (tên, ảnh đại diện, danh sách thành viên)
- **Block_List**: Danh sách người dùng bị chặn bởi người dùng hiện tại
- **User_Presence**: Trạng thái online/offline của người dùng hiển thị trong danh sách chat và màn hình chat
- **Media_Message**: Tin nhắn chứa file ảnh hoặc video được chia sẻ trong cuộc trò chuyện
- **Voice_Call**: Cuộc gọi chỉ có âm thanh (withVideo = false)
- **Video_Call**: Cuộc gọi có cả âm thanh và hình ảnh (withVideo = true)

---

## Requirements

### Requirement 1: Tích hợp LiveKit Android SDK

**User Story:** As a mobile developer, I want to add the LiveKit Android SDK to the project, so that the app can establish WebRTC-based audio/video calls.

#### Acceptance Criteria

1. THE LiveKit_SDK SHALL be declared as a dependency in `app/build.gradle` với phiên bản cố định (pinned version).
2. WHEN the app is built, THE LiveKit_SDK SHALL compile without conflict với các dependency hiện có (Retrofit, OkHttp, RxJava2).
3. THE LiveKit_SDK SHALL support Android minSdk 24 trở lên, phù hợp với cấu hình hiện tại của dự án.

---

### Requirement 2: Lấy LiveKit Room Token từ Backend

**User Story:** As a mobile user, I want the app to obtain a valid LiveKit room token before joining a call, so that I can connect to the correct room securely.

#### Acceptance Criteria

1. WHEN a call is initiated or accepted, THE LiveKit_Token_API SHALL be called with the `conversationId` to retrieve a JWT token.
2. THE LiveKit_Token_API SHALL return a token that allows both web and Android participants to join the same LiveKit room simultaneously without evicting each other.
3. IF the token request fails (network error or HTTP 4xx/5xx), THEN THE Video_Call_Activity SHALL display an error message in Vietnamese and close the call screen.
4. THE LiveKit_Token_API client on Android SHALL send the Bearer access token in the `Authorization` header for authentication.

---

### Requirement 3: Call Signaling qua STOMP WebSocket

**User Story:** As a mobile user, I want to send and receive call signals (OFFER/ANSWER/REJECT/END) over the existing STOMP connection, so that call coordination works the same way as on web.

#### Acceptance Criteria

1. THE Call_Signaling_Manager SHALL subscribe to STOMP destination `/user/queue/calls.signal` ngay sau khi STOMP kết nối thành công.
2. WHEN the user initiates a call, THE Call_Signaling_Manager SHALL publish a `CallSignalMessage` với `type = OFFER` và `withVideo = true` (video call) hoặc `withVideo = false` (voice call) tới STOMP destination `/app/calls.signal`.
3. WHEN the user accepts an incoming call, THE Call_Signaling_Manager SHALL publish a `CallSignalMessage` với `type = ANSWER` tới `/app/calls.signal`.
4. WHEN the user rejects an incoming call, THE Call_Signaling_Manager SHALL publish a `CallSignalMessage` với `type = REJECT` tới `/app/calls.signal`.
5. WHEN the user ends an active call, THE Call_Signaling_Manager SHALL publish a `CallSignalMessage` với `type = END` tới `/app/calls.signal`.
6. WHEN a `REJECT` or `END` signal is received for the active call's `conversationId`, THE Video_Call_Activity SHALL disconnect from the LiveKit room and close.
7. THE Call_Signaling_Manager SHALL reuse the existing `StompManager` singleton connection thay vì tạo kết nối STOMP mới.

---

### Requirement 4: Incoming Call Notification

**User Story:** As a mobile user, I want to see a call notification when someone calls me, so that I can answer or reject the call even when the app is in the background.

#### Acceptance Criteria

1. WHEN an `OFFER` signal is received while the app is in the foreground, THE Incoming_Call_Service SHALL display a full-screen incoming call UI với tên người gọi, ảnh đại diện, và hai nút Accept/Reject.
2. WHEN an `OFFER` signal is received while the app is in the background, THE Incoming_Call_Service SHALL display an Android notification với high-priority channel, tên người gọi, và action buttons Accept/Reject.
3. WHEN the user taps Accept on the notification, THE Incoming_Call_Service SHALL launch the Video_Call_Activity với đúng `conversationId` và `withVideo` từ signal.
4. WHEN the user taps Reject on the notification, THE Incoming_Call_Service SHALL send a `REJECT` signal và dismiss the notification.
5. WHEN an `END` or `REJECT` signal is received for the same `conversationId` as the pending incoming call, THE Incoming_Call_Service SHALL dismiss the incoming call UI/notification automatically.
6. THE Incoming_Call_Service SHALL request `POST_NOTIFICATIONS` permission (Android 13+) trước khi hiển thị notification.

---

### Requirement 5: Video Call Activity

**User Story:** As a mobile user, I want a dedicated screen for video/voice calls with camera and microphone controls, so that I can have a smooth call experience on Android.

#### Acceptance Criteria

1. THE Video_Call_Activity SHALL connect to the LiveKit room using the token from Requirement 2 và URL `ws://10.0.2.2:7880` (emulator) / URL cấu hình cho production.
2. WHEN connected to the LiveKit room, THE Video_Call_Activity SHALL publish local audio track và local video track (nếu `withVideo = true`).
3. WHEN a remote participant publishes a video track, THE Video_Call_Activity SHALL render the remote video in a `SurfaceViewRenderer`.
4. WHEN a remote participant publishes an audio track, THE Video_Call_Activity SHALL play the remote audio automatically.
5. THE Video_Call_Activity SHALL display a toggle button để bật/tắt camera (chỉ hiển thị khi `withVideo = true`).
6. THE Video_Call_Activity SHALL display a toggle button để bật/tắt microphone.
7. THE Video_Call_Activity SHALL display a hang-up button để kết thúc cuộc gọi.
8. THE Video_Call_Activity SHALL display a call duration timer cập nhật mỗi giây sau khi kết nối thành công.
9. WHEN the user taps hang-up, THE Video_Call_Activity SHALL send an `END` signal, disconnect from LiveKit room, và finish the Activity.
10. IF camera or microphone permission is not granted, THEN THE Video_Call_Activity SHALL request the required permissions trước khi publish tracks.
11. IF the user denies camera or microphone permission, THEN THE Video_Call_Activity SHALL continue the call với audio-only mode và hiển thị thông báo giải thích.

---

### Requirement 6: Camera và Microphone Permissions

**User Story:** As a mobile user, I want the app to request camera and microphone permissions properly, so that video and voice calls work without unexpected failures.

#### Acceptance Criteria

1. THE AndroidManifest SHALL declare `android.permission.CAMERA`, `android.permission.RECORD_AUDIO`, và `android.permission.MODIFY_AUDIO_SETTINGS` permissions.
2. WHEN the Video_Call_Activity is launched for a video call, THE Video_Call_Activity SHALL check and request `CAMERA` và `RECORD_AUDIO` permissions at runtime trước khi kết nối LiveKit.
3. WHEN the Video_Call_Activity is launched for a voice call, THE Video_Call_Activity SHALL check and request only `RECORD_AUDIO` permission at runtime.
4. IF all required permissions are granted, THEN THE Video_Call_Activity SHALL proceed to connect to the LiveKit room.

---

### Requirement 7: Cross-Platform Call Compatibility (Web ↔ Android)

**User Story:** As a user, I want to call between web and Android seamlessly, so that platform differences don't break the call experience.

#### Acceptance Criteria

1. WHEN an Android user calls a web user, THE LiveKit_Token_API SHALL issue tokens with the same `roomName` (derived from `conversationId`) for both participants, allowing them to join the same room.
2. WHEN a web user calls an Android user, THE Call_Signaling_Manager SHALL receive the `OFFER` signal and THE Video_Call_Activity SHALL be able to join the same LiveKit room as the web caller.
3. THE LiveKit_Token_API SHALL NOT invalidate or remove existing room participants when a new participant from a different platform joins.
4. WHEN both web and Android participants are in the same LiveKit room, THE Video_Call_Activity SHALL render the remote web participant's video/audio correctly.

---

### Requirement 8: Group Chat — Tạo nhóm và Group Profile

**User Story:** As a mobile user, I want to create group conversations and view/edit group profiles, so that I have the same group chat capabilities as on web.

#### Acceptance Criteria

1. WHEN the user taps "Create Group" from the chat list, THE CreateGroupFragment SHALL allow selecting at least 2 contacts và nhập tên nhóm trước khi tạo.
2. WHEN the group is created successfully, THE StompManager SHALL subscribe to the group topic `/topic/messages.receive-{conversationId}` để nhận tin nhắn realtime.
3. WHEN the user opens a group conversation, THE Group_Profile SHALL be accessible via a button trong toolbar của ChatFragment.
4. THE Group_Profile SHALL display the group name, group avatar (nếu có), và danh sách thành viên với tên và ảnh đại diện.
5. WHEN the group admin updates the group name or avatar, THE Group_Profile SHALL call the backend PATCH endpoint và cập nhật UI ngay lập tức.

---

### Requirement 9: Block List

**User Story:** As a mobile user, I want to block and unblock other users, so that I can control who can contact me.

#### Acceptance Criteria

1. THE ContactsFragment SHALL include a "Blocked Users" section hoặc tab hiển thị danh sách người dùng bị chặn.
2. WHEN the user taps "Block" on a contact, THE ContactsFragment SHALL call `POST /blocks/{blockUserId}` và cập nhật UI để phản ánh trạng thái bị chặn.
3. WHEN the user taps "Unblock" on a blocked user, THE ContactsFragment SHALL call `DELETE /blocks/{blockUserId}` và xóa người dùng đó khỏi danh sách blocked.
4. IF the block/unblock API call fails, THEN THE ContactsFragment SHALL display an error message và revert the UI state.

---

### Requirement 10: Media Sharing trong Chat

**User Story:** As a mobile user, I want to send images and videos in chat conversations, so that I can share media the same way as on web.

#### Acceptance Criteria

1. THE ChatFragment SHALL display an attachment button bên cạnh input field để chọn media từ gallery.
2. WHEN the user selects an image or video, THE ChatFragment SHALL upload the file tới backend media endpoint và gửi tin nhắn chứa media URL.
3. WHEN a message containing an image URL is received, THE MessageAdapter SHALL render a thumbnail của ảnh trong bubble tin nhắn.
4. WHEN the user taps on an image thumbnail, THE ChatFragment SHALL open a full-screen image viewer.
5. IF the media upload fails, THEN THE ChatFragment SHALL display an error toast và không gửi tin nhắn.

---

### Requirement 11: User Presence (Online/Offline Status)

**User Story:** As a mobile user, I want to see whether my contacts are online or offline, so that I know when they are available to chat or call.

#### Acceptance Criteria

1. THE StompManager SHALL subscribe to a presence topic sau khi kết nối STOMP thành công để nhận cập nhật trạng thái online/offline.
2. WHEN a contact's presence status changes, THE ChatListFragment SHALL update the online indicator cho conversation item tương ứng trong danh sách.
3. WHEN the user opens a 1-1 chat, THE ChatFragment SHALL display the online/offline status của người dùng kia trong toolbar.
4. WHEN the app goes to background, THE StompManager SHALL maintain the STOMP connection để tiếp tục nhận presence updates và call signals.

---

### Requirement 12: Voice Call trên Mobile

**User Story:** As a mobile user, I want to make and receive voice-only calls, so that I can communicate without video when preferred.

#### Acceptance Criteria

1. THE ChatFragment SHALL display a voice call button trong toolbar cho 1-1 conversations.
2. WHEN the user taps the voice call button, THE Call_Signaling_Manager SHALL send an `OFFER` signal với `withVideo = false`.
3. WHEN the Video_Call_Activity is launched với `withVideo = false`, THE Video_Call_Activity SHALL connect to LiveKit room với audio track only (không publish video track).
4. WHEN in a voice call, THE Video_Call_Activity SHALL display the remote user's avatar thay vì video feed.
5. THE Video_Call_Activity SHALL display a speaker toggle button để chuyển đổi giữa earpiece và speakerphone trong voice call.
