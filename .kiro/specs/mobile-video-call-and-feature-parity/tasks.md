# Implementation Tasks

## Task 1: Backend — Fix platform-aware token identity
- [ ] 1.1 Đọc `CallResource.java` và xem cách token hiện tại được tạo
- [ ] 1.2 Thêm `@RequestParam(defaultValue = "web") String platform` vào endpoint `GET /calls/{conversationId}/token`
- [ ] 1.3 Cập nhật logic tạo token: `identity = userId + "_" + platform`
- [ ] 1.4 Build backend để xác nhận không có lỗi compile

## Task 2: Mobile — Thêm dependencies (LiveKit SDK + Coroutines)
- [ ] 2.1 Thêm `livekit` và `coroutines` versions vào `gradle/libs.versions.toml`
- [ ] 2.2 Thêm `livekit-android` và `kotlinx-coroutines-android` libraries vào `libs.versions.toml`
- [ ] 2.3 Thêm `implementation libs.livekit.android` và `implementation libs.kotlinx.coroutines.android` vào `app/build.gradle`
- [ ] 2.4 Thêm Kotlin plugin vào `app/build.gradle` (LiveKit SDK yêu cầu Kotlin runtime)
- [ ] 2.5 Sync Gradle và xác nhận build thành công

## Task 3: Mobile — Thêm permissions vào AndroidManifest
- [ ] 3.1 Thêm `CAMERA`, `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS` permissions
- [ ] 3.2 Thêm `POST_NOTIFICATIONS` permission (Android 13+)
- [ ] 3.3 Khai báo `VideoCallActivity` và `IncomingCallActivity` trong manifest

## Task 4: Mobile — Data models và API interfaces
- [ ] 4.1 Tạo `CallSignalMessage.java` trong `data/model/call/`
- [ ] 4.2 Tạo `CallTokenResponse.java` trong `data/model/call/`
- [ ] 4.3 Tạo `CallApi.java` Retrofit interface trong `data/api/`
- [ ] 4.4 Tạo `BlockApi.java` Retrofit interface trong `data/api/`
- [ ] 4.5 Đăng ký `CallApi` và `BlockApi` trong `ApiClient.java`

## Task 5: Mobile — CallSignalingManager
- [ ] 5.1 Tạo `CallSignalingManager.java` singleton trong `data/socket/`
- [ ] 5.2 Implement `subscribeCallSignals(StompClient)` — subscribe `/user/queue/calls.signal`
- [ ] 5.3 Implement `sendSignal(CallSignalMessage)` — publish tới `/app/calls.signal`
- [ ] 5.4 Expose `Observable<CallSignalMessage> signals()` via PublishSubject
- [ ] 5.5 Tích hợp vào `StompManager.subscribeUserQueues()` — gọi `CallSignalingManager` sau khi STOMP connected

## Task 6: Mobile — IncomingCallActivity
- [ ] 6.1 Tạo layout `activity_incoming_call.xml` (avatar, tên, nút Accept/Reject)
- [ ] 6.2 Tạo `IncomingCallActivity.java` với window flags (show on lock screen)
- [ ] 6.3 Implement Accept: start `VideoCallActivity` với extras từ signal
- [ ] 6.4 Implement Reject: gửi REJECT signal qua `CallSignalingManager`
- [ ] 6.5 Auto-dismiss khi nhận END/REJECT signal cho cùng conversationId
- [ ] 6.6 Tích hợp vào `MainActivity` — observe `CallSignalingManager.signals()` để start `IncomingCallActivity` khi nhận OFFER

## Task 7: Mobile — VideoCallActivity + VideoCallViewModel
- [ ] 7.1 Tạo `CallUiState.java` data class
- [ ] 7.2 Tạo `VideoCallViewModel.java` với LiveData và coroutine scope
- [ ] 7.3 Implement `connect()`: gọi `CallApi.getToken()` → `room.connect()` → publish tracks
- [ ] 7.4 Implement `disconnect()`: `room.disconnect()`, gửi END signal
- [ ] 7.5 Implement `toggleCamera()`, `toggleMicrophone()`, `toggleSpeaker()`
- [ ] 7.6 Implement permission handling: `onPermissionsResult()`
- [ ] 7.7 Tạo layout `activity_video_call.xml` (2 SurfaceViewRenderer, controls)
- [ ] 7.8 Tạo `VideoCallActivity.java`: setup ViewBinding, observe ViewModel, handle permissions
- [ ] 7.9 Implement call duration timer (Handler + Runnable, mỗi giây +1)
- [ ] 7.10 Render remote video/audio khi remote participant publish tracks

## Task 8: Mobile — ChatFragment: nút gọi video/voice
- [ ] 8.1 Thêm menu items (video call, voice call) vào toolbar của `ChatFragment` cho 1-1 conversations
- [ ] 8.2 Implement `onVideoCallClick()`: gửi OFFER signal với `withVideo=true`, start `VideoCallActivity`
- [ ] 8.3 Implement `onVoiceCallClick()`: gửi OFFER signal với `withVideo=false`, start `VideoCallActivity`
- [ ] 8.4 Xử lý ANSWER signal: khi nhận ANSWER → `VideoCallActivity` đã được start, không cần làm gì thêm
- [ ] 8.5 Xử lý REJECT signal: hiển thị Toast "Đối phương đã từ chối cuộc gọi"

## Task 9: Mobile — Media Sharing trong ChatFragment
- [ ] 9.1 Thêm `btnAttachment` ImageButton vào layout `fragment_chat.xml`
- [ ] 9.2 Thêm `uploadAttachment()` method vào `MessageApi.java` (multipart POST)
- [ ] 9.3 Implement `ActivityResultLauncher` để pick image từ gallery
- [ ] 9.4 Implement `ChatViewModel.uploadAndSendMedia(Uri)`: upload → gửi tin nhắn IMAGE
- [ ] 9.5 Cập nhật `MessageAdapter` để render thumbnail ảnh dùng Glide
- [ ] 9.6 Implement full-screen image viewer khi tap vào thumbnail

## Task 10: Mobile — User Presence
- [ ] 10.1 Thêm `SocketEvent.Type.PRESENCE_UPDATE` vào enum `SocketEvent`
- [ ] 10.2 Tạo `PresenceUpdate.java` model
- [ ] 10.3 Thêm `subscribePresence(String userId)` vào `StompManager`
- [ ] 10.4 Cập nhật `ConversationAdapter` để hiển thị online indicator (green dot)
- [ ] 10.5 Cập nhật `ChatFragment` toolbar để hiển thị online/offline status

## Task 11: Mobile — Block List
- [ ] 11.1 Tạo `BlockedUser.java` model và `BlockedUserList.java`
- [ ] 11.2 Thêm tab/section "Blocked Users" vào `ContactsFragment`
- [ ] 11.3 Implement load blocked users: gọi `BlockApi.getBlockedUsers()`
- [ ] 11.4 Implement block user: gọi `BlockApi.blockUser()` + update UI
- [ ] 11.5 Implement unblock user: gọi `BlockApi.unblockUser()` + update UI

## Task 12: Mobile — Group Profile Fragment
- [ ] 12.1 Tạo layout `fragment_group_profile.xml` (tên nhóm, avatar, danh sách thành viên)
- [ ] 12.2 Tạo `GroupProfileFragment.java` với load group info từ API
- [ ] 12.3 Implement edit group name (inline EditText cho admin)
- [ ] 12.4 Implement update group avatar (ActivityResultLauncher + PATCH API)
- [ ] 12.5 Thêm "Group Info" menu item vào toolbar của `ChatFragment` cho group conversations
- [ ] 12.6 Navigate tới `GroupProfileFragment` khi tap "Group Info"
