import 'package:kiromobile/features/chat/data/models/attachment.dart';
import 'package:kiromobile/features/chat/data/models/message_type.dart';

class SendMessageRequest {
  const SendMessageRequest({
    required this.conversationId,
    required this.type,
    this.content,
    this.replyToMessageId,
    this.attachment,
  });

  final String conversationId;
  final MessageType type;
  final String? content;
  final String? replyToMessageId;
  final Attachment? attachment;

  Map<String, dynamic> toJson() {
    return {
      'conversationId': conversationId,
      'type': type.value,
      if (content != null) 'content': content,
      if (replyToMessageId != null) 'replyToMessageId': replyToMessageId,
      if (attachment != null) 'attachment': attachment!.toJson(),
    };
  }
}
