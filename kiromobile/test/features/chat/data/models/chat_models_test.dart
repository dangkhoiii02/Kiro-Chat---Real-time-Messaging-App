import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/conversation_message_list.dart';

void main() {
  test('Conversation.fromJson maps chat list item from backend payload', () {
    final conversation = Conversation.fromJson({
      'conversationId': 'conversation-1',
      'conversationName': 'Kiro Group',
      'avatarUrl': 'http://example.com/group.png',
      'lastMessage': {
        'messageId': 'message-1',
        'conversationId': 'conversation-1',
        'senderId': 'user-1',
        'type': 'text',
        'content': 'Hello',
        'messageState': 'sent',
        'timestamp': '2026-05-29T10:00:00Z',
        'isDeleted': false,
      },
      'unreadCount': 3,
      'isOnline': true,
      'isGroup': true,
      'remoteUserId': null,
    });

    expect(conversation.conversationId, 'conversation-1');
    expect(conversation.conversationName, 'Kiro Group');
    expect(conversation.isGroup, isTrue);
    expect(conversation.unreadCount, 3);
    expect(conversation.lastMessage?.content, 'Hello');
  });

  test('ChatMessage.fromJson maps message and owner flag', () {
    final message = ChatMessage.fromJson({
      'messageId': 'message-1',
      'conversationId': 'conversation-1',
      'senderId': 'user-1',
      'type': 'image',
      'content': null,
      'mediaUrl': 'http://example.com/image.png',
      'mediaName': 'image.png',
      'messageState': 'delivered',
      'timestamp': '2026-05-29T10:00:00Z',
      'replyToMessageId': 'message-0',
      'isDeleted': false,
      'owner': true,
    });

    expect(message.messageId, 'message-1');
    expect(message.type, MessageType.image);
    expect(message.messageState, MessageState.delivered);
    expect(message.mediaName, 'image.png');
    expect(message.owner, isTrue);
  });

  test('ConversationMessageList.fromJson maps cursor pagination payload', () {
    final list = ConversationMessageList.fromJson({
      'messages': [
        {
          'messageId': 'message-1',
          'conversationId': 'conversation-1',
          'senderId': 'user-1',
          'type': 'text',
          'content': 'Hello',
          'messageState': 'seen',
          'timestamp': '2026-05-29T10:00:00Z',
          'isDeleted': false,
          'owner': false,
        },
      ],
      'hasMore': true,
      'nextBeforeMessageId': 'message-1',
    });

    expect(list.messages, hasLength(1));
    expect(list.hasMore, isTrue);
    expect(list.nextBeforeMessageId, 'message-1');
  });
}
