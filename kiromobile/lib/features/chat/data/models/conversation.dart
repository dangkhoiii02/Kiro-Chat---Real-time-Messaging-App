import 'package:kiromobile/features/chat/data/models/chat_message.dart';

class Conversation {
  const Conversation({
    required this.conversationId,
    required this.conversationName,
    required this.isGroup,
    required this.unreadCount,
    required this.isOnline,
    this.avatarUrl,
    this.lastMessage,
    this.remoteUserId,
    this.isFollowingUp,
    this.isArchived,
  });

  final String conversationId;
  final String conversationName;
  final String? avatarUrl;
  final ChatMessage? lastMessage;
  final int unreadCount;
  final bool isOnline;
  final bool isGroup;
  final String? remoteUserId;
  final bool? isFollowingUp;
  final bool? isArchived;

  factory Conversation.fromJson(Map<String, dynamic> json) {
    final lastMessageJson = json['lastMessage'];
    final conversationId = json['conversationId'] as String;

    return Conversation(
      conversationId: conversationId,
      conversationName: json['conversationName'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      lastMessage: lastMessageJson is Map<String, dynamic>
          ? ChatMessage.fromJson({
              ...lastMessageJson,
              if (lastMessageJson['conversationId'] == null)
                'conversationId': conversationId,
            })
          : null,
      unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
      isOnline: (json['isOnline'] as bool?) ?? false,
      isGroup: (json['isGroup'] as bool?) ?? false,
      remoteUserId: json['remoteUserId'] as String?,
      isFollowingUp: json['isFollowingUp'] as bool?,
      isArchived: json['isArchived'] as bool?,
    );
  }

  Conversation copyWith({
    ChatMessage? lastMessage,
    int? unreadCount,
    bool? isOnline,
  }) {
    return Conversation(
      conversationId: conversationId,
      conversationName: conversationName,
      isGroup: isGroup,
      unreadCount: unreadCount ?? this.unreadCount,
      isOnline: isOnline ?? this.isOnline,
      avatarUrl: avatarUrl,
      lastMessage: lastMessage ?? this.lastMessage,
      remoteUserId: remoteUserId,
      isFollowingUp: isFollowingUp,
      isArchived: isArchived,
    );
  }
}
