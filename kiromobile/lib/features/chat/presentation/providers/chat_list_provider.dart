import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/realtime/stomp_service.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/repositories/conversation_repository.dart';

enum ChatListStatus { initial, loading, success, failure }

class ChatListState {
  const ChatListState({
    required this.status,
    this.conversations = const [],
    this.errorMessage,
  });

  const ChatListState.initial()
    : status = ChatListStatus.initial,
      conversations = const [],
      errorMessage = null;

  const ChatListState.loading()
    : status = ChatListStatus.loading,
      conversations = const [],
      errorMessage = null;

  const ChatListState.success(this.conversations)
    : status = ChatListStatus.success,
      errorMessage = null;

  const ChatListState.failure(this.errorMessage)
    : status = ChatListStatus.failure,
      conversations = const [];

  final ChatListStatus status;
  final List<Conversation> conversations;
  final String? errorMessage;
}

final chatListControllerProvider =
    NotifierProvider<ChatListController, ChatListState>(ChatListController.new);

class ChatListController extends Notifier<ChatListState> {
  @override
  ChatListState build() {
    return const ChatListState.initial();
  }

  Future<void> loadConversations({
    String? query,
    int page = 0,
    int size = 20,
  }) async {
    state = const ChatListState.loading();

    try {
      final result = await ref
          .read(conversationRepositoryProvider)
          .getConversations(query: query, page: page, size: size);

      state = ChatListState.success(result.conversations.content);
      final isDefaultInboxLoad = (query == null || query.isEmpty) && page == 0;
      if (isDefaultInboxLoad) {
        ref
            .read(stompServiceProvider)
            .watchGroupConversations(result.conversations.content);
      }
    } catch (e) {
      state = ChatListState.failure(e.toString());
    }
  }

  bool isGroupConversation(String conversationId) {
    return state.conversations
        .where((conversation) => conversation.conversationId == conversationId)
        .any((conversation) => conversation.isGroup);
  }

  void applyIncomingMessage(
    ChatMessage message, {
    required bool incrementUnread,
  }) {
    final conversations = state.conversations;
    final index = conversations.indexWhere(
      (conversation) => conversation.conversationId == message.conversationId,
    );

    if (index == -1) {
      return;
    }

    final conversation = conversations[index];
    final updatedConversation = conversation.copyWith(
      lastMessage: message,
      unreadCount: incrementUnread
          ? conversation.unreadCount + 1
          : conversation.unreadCount,
    );
    final nextConversations = [
      updatedConversation,
      ...conversations.where(
        (item) => item.conversationId != message.conversationId,
      ),
    ];

    state = ChatListState.success(nextConversations);
  }

  void applyMessageStatus(ChatMessage message) {
    final conversations = state.conversations;
    final index = conversations.indexWhere(
      (conversation) => conversation.conversationId == message.conversationId,
    );

    if (index == -1) {
      return;
    }

    final conversation = conversations[index];
    final lastMessage = conversation.lastMessage;
    if (lastMessage == null || lastMessage.messageId != message.messageId) {
      return;
    }

    final nextConversations = [...conversations];
    nextConversations[index] = conversation.copyWith(
      lastMessage: lastMessage.copyWith(
        messageState: message.messageState,
        timestamp: message.timestamp,
      ),
    );

    state = ChatListState.success(nextConversations);
  }
}
