import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/logging/app_logger.dart';
import 'package:kiromobile/core/realtime/stomp_service.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/mark_message_seen_request.dart';
import 'package:kiromobile/features/chat/data/models/send_message_request.dart';
import 'package:kiromobile/features/chat/data/repositories/message_repository.dart';
import 'package:kiromobile/features/chat/presentation/providers/app_realtime_provider.dart';

enum ChatDetailStatus { initial, loading, success, failure }

class ChatDetailState {
  const ChatDetailState({
    required this.status,
    this.messages = const [],
    this.hasMore = false,
    this.nextBeforeMessageId,
    this.errorMessage,
    this.isSending = false,
    this.sendErrorMessage,
  });

  const ChatDetailState.initial()
    : status = ChatDetailStatus.initial,
      messages = const [],
      hasMore = false,
      nextBeforeMessageId = null,
      errorMessage = null,
      isSending = false,
      sendErrorMessage = null;

  const ChatDetailState.loading()
    : status = ChatDetailStatus.loading,
      messages = const [],
      hasMore = false,
      nextBeforeMessageId = null,
      errorMessage = null,
      isSending = false,
      sendErrorMessage = null;

  const ChatDetailState.success({
    required this.messages,
    required this.hasMore,
    this.nextBeforeMessageId,
    this.isSending = false,
    this.sendErrorMessage,
  }) : status = ChatDetailStatus.success,
       errorMessage = null;

  const ChatDetailState.failure(this.errorMessage)
    : status = ChatDetailStatus.failure,
      messages = const [],
      hasMore = false,
      nextBeforeMessageId = null,
      isSending = false,
      sendErrorMessage = null;

  final ChatDetailStatus status;
  final List<ChatMessage> messages;
  final bool hasMore;
  final String? nextBeforeMessageId;
  final String? errorMessage;
  final bool isSending;
  final String? sendErrorMessage;

  ChatDetailState copyWith({
    ChatDetailStatus? status,
    List<ChatMessage>? messages,
    bool? hasMore,
    String? nextBeforeMessageId,
    String? errorMessage,
    bool? isSending,
    String? sendErrorMessage,
  }) {
    return ChatDetailState(
      status: status ?? this.status,
      messages: messages ?? this.messages,
      hasMore: hasMore ?? this.hasMore,
      nextBeforeMessageId: nextBeforeMessageId ?? this.nextBeforeMessageId,
      errorMessage: errorMessage ?? this.errorMessage,
      isSending: isSending ?? this.isSending,
      sendErrorMessage: sendErrorMessage,
    );
  }
}

final chatDetailControllerProvider =
    NotifierProvider<ChatDetailController, ChatDetailState>(
      ChatDetailController.new,
    );

class ChatDetailController extends Notifier<ChatDetailState> {
  StreamSubscription<ChatMessage>? _incomingMessageSubscription;
  StreamSubscription<Map<String, dynamic>>? _statusEventSubscription;
  Conversation? _activeConversation;

  @override
  ChatDetailState build() {
    ref.onDispose(() {
      _incomingMessageSubscription?.cancel();
      _statusEventSubscription?.cancel();
    });

    return const ChatDetailState.initial();
  }

  Future<void> loadMessages({
    required String conversationId,
    String? beforeMessageId,
    int limit = 20,
    bool markSeen = false,
  }) async {
    state = const ChatDetailState.loading();

    try {
      final result = await ref
          .read(messageRepositoryProvider)
          .getMessages(
            conversationId: conversationId,
            beforeMessageId: beforeMessageId,
            limit: limit,
          );

      state = ChatDetailState.success(
        messages: result.messages,
        hasMore: result.hasMore,
        nextBeforeMessageId: result.nextBeforeMessageId,
      );

      if (markSeen) {
        _markLatestIncomingMessageSeen(
          conversationId: conversationId,
          messages: result.messages,
        );
      }
    } catch (e) {
      state = ChatDetailState.failure(e.toString());
    }
  }

  Future<void> sendTextMessage({
    required Conversation conversation,
    required String content,
  }) async {
    final trimmedContent = content.trim();

    if (trimmedContent.isEmpty || state.isSending) {
      return;
    }

    final previousState = state;
    state = previousState.copyWith(isSending: true, sendErrorMessage: null);

    try {
      final sentMessage = await ref
          .read(messageRepositoryProvider)
          .sendMessage(
            isGroup: conversation.isGroup,
            request: SendMessageRequest(
              conversationId: conversation.conversationId,
              type: MessageType.text,
              content: trimmedContent,
            ),
          );

      state = previousState.copyWith(
        status: ChatDetailStatus.success,
        messages: [...previousState.messages, sentMessage],
        isSending: false,
        sendErrorMessage: null,
      );
    } catch (e) {
      state = previousState.copyWith(
        isSending: false,
        sendErrorMessage: e.toString(),
      );
    }
  }

  Future<void> startRealtime(Conversation conversation) async {
    final stompService = ref.read(stompServiceProvider);
    _activeConversation = conversation;
    ref
        .read(activeChatConversationIdProvider.notifier)
        .setActive(conversation.conversationId);

    await _incomingMessageSubscription?.cancel();
    _incomingMessageSubscription = stompService.incomingMessages.listen(
      _appendRealtimeMessage,
    );

    await _statusEventSubscription?.cancel();
    _statusEventSubscription = stompService.statusEvents.listen(
      _handleStatusEvent,
    );
  }

  Future<void> stopRealtime() async {
    await _incomingMessageSubscription?.cancel();
    _incomingMessageSubscription = null;
    await _statusEventSubscription?.cancel();
    _statusEventSubscription = null;
    _activeConversation = null;
    ref.read(activeChatConversationIdProvider.notifier).clear();
  }

  void _appendRealtimeMessage(ChatMessage message) {
    final activeConversation = _activeConversation;
    if (activeConversation == null ||
        message.conversationId != activeConversation.conversationId) {
      return;
    }

    if (state.messages.any((item) => item.messageId == message.messageId)) {
      return;
    }

    state = state.copyWith(
      status: ChatDetailStatus.success,
      messages: [...state.messages, message],
      sendErrorMessage: null,
    );

    if (message.owner != true) {
      unawaited(
        _markConversationSeen(
          conversationId: activeConversation.conversationId,
          messageId: message.messageId,
        ),
      );
    }
  }

  void _markLatestIncomingMessageSeen({
    required String conversationId,
    required List<ChatMessage> messages,
  }) {
    final latestIncomingMessage = _latestIncomingMessage(messages);
    if (latestIncomingMessage == null) {
      return;
    }

    unawaited(
      _markConversationSeen(
        conversationId: conversationId,
        messageId: latestIncomingMessage.messageId,
      ),
    );
  }

  Future<void> _markConversationSeen({
    required String conversationId,
    required String messageId,
  }) async {
    try {
      await ref
          .read(messageRepositoryProvider)
          .markConversationSeen(
            request: MarkMessageSeenRequest(
              conversationId: conversationId,
              lastSeenMessageId: messageId,
            ),
          );

      _updateMessageState(
        messageId: messageId,
        messageState: MessageState.seen,
      );
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot mark conversation message as seen.',
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

      final activeConversation = _activeConversation;
      if (activeConversation != null &&
          message.conversationId != activeConversation.conversationId) {
        return;
      }

      _updateMessageState(
        messageId: message.messageId,
        messageState: message.messageState,
        timestamp: message.timestamp,
      );
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot apply STOMP status event.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  ChatMessage? _latestIncomingMessage(List<ChatMessage> messages) {
    ChatMessage? latest;
    for (final message in messages) {
      if (message.owner == true) {
        continue;
      }

      if (latest == null || message.timestamp.isAfter(latest.timestamp)) {
        latest = message;
      }
    }

    return latest;
  }

  void _updateMessageState({
    required String messageId,
    required MessageState messageState,
    DateTime? timestamp,
  }) {
    var changed = false;
    final nextMessages = state.messages.map((message) {
      if (message.messageId != messageId ||
          !_shouldApplyMessageState(message.messageState, messageState)) {
        return message;
      }

      changed = true;
      return message.copyWith(messageState: messageState, timestamp: timestamp);
    }).toList();

    if (!changed) {
      return;
    }

    state = state.copyWith(messages: nextMessages);
  }

  bool _shouldApplyMessageState(MessageState current, MessageState next) {
    return _messageStateRank(next) >= _messageStateRank(current);
  }

  int _messageStateRank(MessageState state) {
    return switch (state) {
      MessageState.prepare => 0,
      MessageState.sent => 1,
      MessageState.delivered => 2,
      MessageState.seen => 3,
      MessageState.failed => 4,
      MessageState.unknown => -1,
    };
  }
}
