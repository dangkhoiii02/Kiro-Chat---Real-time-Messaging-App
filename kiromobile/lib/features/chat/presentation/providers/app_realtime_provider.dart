import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/logging/app_logger.dart';
import 'package:kiromobile/core/realtime/presence_repository.dart';
import 'package:kiromobile/core/realtime/stomp_service.dart';
import 'package:kiromobile/features/chat/data/models/acknowledge_receive_message_request.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/presence_event.dart';
import 'package:kiromobile/features/chat/data/repositories/message_repository.dart';
import 'package:kiromobile/features/chat/presentation/providers/chat_list_provider.dart';
import 'package:kiromobile/features/contact/presentation/providers/contacts_controller.dart';

final activeChatConversationIdProvider =
    NotifierProvider<ActiveChatConversationController, String?>(
      ActiveChatConversationController.new,
    );

class ActiveChatConversationController extends Notifier<String?> {
  @override
  String? build() {
    return null;
  }

  void setActive(String conversationId) {
    state = conversationId;
  }

  void clear() {
    state = null;
  }
}

final appRealtimeControllerProvider =
    NotifierProvider<AppRealtimeController, bool>(AppRealtimeController.new);

class AppRealtimeController extends Notifier<bool> {
  static const _presenceHeartbeatInterval = Duration(seconds: 30);

  StreamSubscription<ChatMessage>? _incomingMessageSubscription;
  StreamSubscription<Map<String, dynamic>>? _statusEventSubscription;
  StreamSubscription<PresenceEvent>? _presenceEventSubscription;
  Timer? _presenceHeartbeatTimer;
  bool _started = false;

  @override
  bool build() {
    final stompService = ref.read(stompServiceProvider);
    final presenceRepository = ref.read(presenceRepositoryProvider);

    ref.onDispose(() {
      _incomingMessageSubscription?.cancel();
      _statusEventSubscription?.cancel();
      _presenceEventSubscription?.cancel();
      _presenceHeartbeatTimer?.cancel();
      if (_started) {
        unawaited(_sendExplicitOffline(presenceRepository));
      }
      stompService.disconnect();
    });

    return false;
  }

  Future<void> start() async {
    if (state) {
      return;
    }

    final stompService = ref.read(stompServiceProvider);
    await stompService.connect();
    await _sendPresenceHeartbeat();
    _presenceHeartbeatTimer?.cancel();
    _presenceHeartbeatTimer = Timer.periodic(
      _presenceHeartbeatInterval,
      (_) => unawaited(_sendPresenceHeartbeat()),
    );

    await _incomingMessageSubscription?.cancel();
    _incomingMessageSubscription = stompService.incomingMessages.listen(
      _handleIncomingMessage,
    );

    await _statusEventSubscription?.cancel();
    _statusEventSubscription = stompService.statusEvents.listen(
      _handleStatusEvent,
    );

    await _presenceEventSubscription?.cancel();
    _presenceEventSubscription = stompService.presenceEvents.listen(
      _handlePresenceEvent,
    );

    _started = true;
    state = true;
  }

  Future<void> stop() async {
    await _incomingMessageSubscription?.cancel();
    _incomingMessageSubscription = null;
    await _statusEventSubscription?.cancel();
    _statusEventSubscription = null;
    await _presenceEventSubscription?.cancel();
    _presenceEventSubscription = null;
    _presenceHeartbeatTimer?.cancel();
    _presenceHeartbeatTimer = null;
    await _sendExplicitOffline();
    ref.read(stompServiceProvider).disconnect();
    ref.read(activeChatConversationIdProvider.notifier).clear();
    _started = false;
    state = false;
  }

  void _handleIncomingMessage(ChatMessage message) {
    final activeConversationId = ref.read(activeChatConversationIdProvider);
    final isActiveConversation = activeConversationId == message.conversationId;

    ref
        .read(chatListControllerProvider.notifier)
        .applyIncomingMessage(
          message,
          incrementUnread: message.owner != true && !isActiveConversation,
        );

    if (message.owner != true) {
      unawaited(_acknowledgeReceived(message));
    }
  }

  Future<void> _acknowledgeReceived(ChatMessage message) async {
    try {
      final deliveredMessage = await ref
          .read(messageRepositoryProvider)
          .acknowledgeReceived(
            isGroup: ref
                .read(chatListControllerProvider.notifier)
                .isGroupConversation(message.conversationId),
            request: AcknowledgeReceiveMessageRequest(
              messageId: message.messageId,
            ),
          );

      ref
          .read(chatListControllerProvider.notifier)
          .applyMessageStatus(deliveredMessage);
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot acknowledge app-level incoming message.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  void _handleStatusEvent(Map<String, dynamic> event) {
    final payload = event['payload'];
    if (payload is! Map<String, dynamic>) {
      return;
    }

    try {
      final message = ChatMessage.fromJson({
        ...payload,
        'owner': payload['owner'] ?? payload['isOwner'] ?? true,
      });

      ref.read(chatListControllerProvider.notifier).applyMessageStatus(message);
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot apply app-level status event.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  void _handlePresenceEvent(PresenceEvent event) {
    ref.read(chatListControllerProvider.notifier).applyPresence(event);
    ref.read(contactsControllerProvider.notifier).applyPresence(event);
  }

  Future<void> _sendPresenceHeartbeat([PresenceRepository? repository]) async {
    try {
      final PresenceRepository presenceRepository =
          repository ?? ref.read(presenceRepositoryProvider);
      await presenceRepository.heartbeat();
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot send presence heartbeat.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  Future<void> _sendExplicitOffline([PresenceRepository? repository]) async {
    try {
      final PresenceRepository presenceRepository =
          repository ?? ref.read(presenceRepositoryProvider);
      await presenceRepository.explicitOffline();
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot send explicit offline presence.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }
}
