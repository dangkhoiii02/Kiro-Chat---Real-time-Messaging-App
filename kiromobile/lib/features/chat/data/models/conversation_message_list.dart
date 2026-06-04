import 'package:kiromobile/features/chat/data/models/chat_message.dart';

class ConversationMessageList {
  const ConversationMessageList({
    required this.messages,
    required this.hasMore,
    this.nextBeforeMessageId,
  });

  final List<ChatMessage> messages;
  final bool hasMore;
  final String? nextBeforeMessageId;

  factory ConversationMessageList.fromJson(Map<String, dynamic> json) {
    final messagesJson = (json['messages'] as List<dynamic>? ?? [])
        .whereType<Map<String, dynamic>>();

    return ConversationMessageList(
      messages: messagesJson.map(ChatMessage.fromJson).toList(),
      hasMore: (json['hasMore'] as bool?) ?? false,
      nextBeforeMessageId: json['nextBeforeMessageId'] as String?,
    );
  }
}
