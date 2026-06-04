import 'package:kiromobile/features/chat/data/models/message_state.dart';
import 'package:kiromobile/features/chat/data/models/message_type.dart';

export 'message_state.dart';
export 'message_type.dart';

class ChatMessage {
  const ChatMessage({
    required this.messageId,
    required this.conversationId,
    required this.senderId,
    required this.type,
    required this.messageState,
    required this.timestamp,
    required this.isDeleted,
    this.content,
    this.mediaUrl,
    this.mediaName,
    this.replyToMessageId,
    this.owner,
  });

  final String messageId;
  final String conversationId;
  final String senderId;
  final MessageType type;
  final String? content;
  final String? mediaUrl;
  final String? mediaName;
  final MessageState messageState;
  final DateTime timestamp;
  final String? replyToMessageId;
  final bool isDeleted;
  final bool? owner;

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      messageId: json['messageId'] as String,
      conversationId: (json['conversationId'] ?? '') as String,
      senderId: json['senderId'] as String,
      type: MessageType.fromJson(json['type'] as String?),
      content: json['content'] as String?,
      mediaUrl: json['mediaUrl'] as String?,
      mediaName: json['mediaName'] as String?,
      messageState: MessageState.fromJson(json['messageState'] as String?),
      timestamp: DateTime.parse(json['timestamp'] as String),
      replyToMessageId: json['replyToMessageId'] as String?,
      isDeleted: (json['isDeleted'] as bool?) ?? false,
      owner: (json['owner'] ?? json['isOwner']) as bool?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'messageId': messageId,
      'conversationId': conversationId,
      'senderId': senderId,
      'type': type.value,
      'content': content,
      'mediaUrl': mediaUrl,
      'mediaName': mediaName,
      'messageState': messageState.value,
      'timestamp': timestamp.toIso8601String(),
      'replyToMessageId': replyToMessageId,
      'isDeleted': isDeleted,
      if (owner != null) 'owner': owner,
    };
  }

  ChatMessage copyWith({
    MessageState? messageState,
    DateTime? timestamp,
    bool? owner,
  }) {
    return ChatMessage(
      messageId: messageId,
      conversationId: conversationId,
      senderId: senderId,
      type: type,
      content: content,
      mediaUrl: mediaUrl,
      mediaName: mediaName,
      messageState: messageState ?? this.messageState,
      timestamp: timestamp ?? this.timestamp,
      replyToMessageId: replyToMessageId,
      isDeleted: isDeleted,
      owner: owner ?? this.owner,
    );
  }
}
