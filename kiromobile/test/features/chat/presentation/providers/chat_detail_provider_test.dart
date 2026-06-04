import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/core/realtime/stomp_service.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/conversation_message_list.dart';
import 'package:kiromobile/features/chat/data/models/mark_message_seen_request.dart';
import 'package:kiromobile/features/chat/data/models/send_message_request.dart';
import 'package:kiromobile/features/chat/data/repositories/message_repository.dart';
import 'package:kiromobile/features/chat/presentation/providers/chat_detail_provider.dart';

void main() {
  test('receives active conversation message then marks it seen', () async {
    final fakeRepository = _FakeMessageRepository();
    final fakeStompService = _FakeStompService();
    final container = _container(fakeRepository, fakeStompService);
    addTearDown(container.dispose);

    final controller = container.read(chatDetailControllerProvider.notifier);
    await controller.startRealtime(_directConversation);

    fakeStompService.addIncoming(
      _message(
        messageId: 'incoming-1',
        owner: false,
        messageState: MessageState.sent,
      ),
    );
    await pumpEventQueue(times: 5);

    expect(fakeRepository.seenMessageIds, ['incoming-1']);
    expect(
      container.read(chatDetailControllerProvider).messages.single.messageState,
      MessageState.seen,
    );
  });

  test('loadMessages marks latest incoming message as seen', () async {
    final fakeRepository = _FakeMessageRepository()
      ..messagesResult = ConversationMessageList(
        messages: [
          _message(
            messageId: 'incoming-old',
            owner: false,
            timestamp: DateTime.parse('2026-06-01T09:00:00.000Z'),
          ),
          _message(
            messageId: 'mine-latest',
            owner: true,
            timestamp: DateTime.parse('2026-06-01T11:00:00.000Z'),
          ),
          _message(
            messageId: 'incoming-latest',
            owner: false,
            timestamp: DateTime.parse('2026-06-01T10:00:00.000Z'),
          ),
        ],
        hasMore: false,
      );
    final fakeStompService = _FakeStompService();
    final container = _container(fakeRepository, fakeStompService);
    addTearDown(container.dispose);

    await container
        .read(chatDetailControllerProvider.notifier)
        .loadMessages(
          conversationId: _directConversation.conversationId,
          markSeen: true,
        );
    await pumpEventQueue(times: 5);

    expect(fakeRepository.seenMessageIds, ['incoming-latest']);
  });

  test('status event updates matching message state', () async {
    final fakeRepository = _FakeMessageRepository()
      ..messagesResult = ConversationMessageList(
        messages: [
          _message(
            messageId: 'mine-1',
            owner: true,
            messageState: MessageState.sent,
          ),
        ],
        hasMore: false,
      );
    final fakeStompService = _FakeStompService();
    final container = _container(fakeRepository, fakeStompService);
    addTearDown(container.dispose);

    final controller = container.read(chatDetailControllerProvider.notifier);
    await controller.loadMessages(
      conversationId: _directConversation.conversationId,
    );
    await controller.startRealtime(_directConversation);

    final statusPayload = _message(
      messageId: 'mine-1',
      owner: true,
      messageState: MessageState.seen,
    ).toJson()..remove('owner');
    fakeStompService.addStatus({
      'destination': '/user/queue/messages.seen',
      'payload': statusPayload,
    });
    await pumpEventQueue(times: 5);

    expect(
      container.read(chatDetailControllerProvider).messages.single.messageState,
      MessageState.seen,
    );
  });
}

ProviderContainer _container(
  MessageRepository messageRepository,
  StompService stompService,
) {
  return ProviderContainer(
    overrides: [
      messageRepositoryProvider.overrideWithValue(messageRepository),
      stompServiceProvider.overrideWithValue(stompService),
    ],
  );
}

const _directConversation = Conversation(
  conversationId: 'conversation-1',
  conversationName: 'Direct Chat',
  isGroup: false,
  unreadCount: 0,
  isOnline: true,
);

ChatMessage _message({
  required String messageId,
  required bool owner,
  MessageState messageState = MessageState.sent,
  DateTime? timestamp,
}) {
  return ChatMessage(
    messageId: messageId,
    conversationId: _directConversation.conversationId,
    senderId: owner ? 'current-user' : 'other-user',
    type: MessageType.text,
    content: 'Hello',
    messageState: messageState,
    timestamp: timestamp ?? DateTime.parse('2026-06-01T10:00:00.000Z'),
    isDeleted: false,
    owner: owner,
  );
}

class _FakeMessageRepository extends MessageRepository {
  _FakeMessageRepository() : super(Dio());

  ConversationMessageList messagesResult = const ConversationMessageList(
    messages: [],
    hasMore: false,
  );
  final seenMessageIds = <String>[];

  @override
  Future<ConversationMessageList> getMessages({
    required String conversationId,
    String? beforeMessageId,
    int limit = 20,
  }) async {
    return messagesResult;
  }

  @override
  Future<ChatMessage> sendMessage({
    required bool isGroup,
    required SendMessageRequest request,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<void> markConversationSeen({
    required MarkMessageSeenRequest request,
  }) async {
    seenMessageIds.add(request.lastSeenMessageId);
  }
}

class _FakeStompService extends StompService {
  _FakeStompService() : super(accessTokenReader: () async => 'access-token');

  final _incomingController = StreamController<ChatMessage>.broadcast();
  final _statusController = StreamController<Map<String, dynamic>>.broadcast();

  bool connected = false;

  @override
  Stream<ChatMessage> get incomingMessages => _incomingController.stream;

  @override
  Stream<Map<String, dynamic>> get statusEvents => _statusController.stream;

  @override
  bool get isConnected => connected;

  @override
  Future<void> connect() async {
    connected = true;
  }

  @override
  @override
  void clearConversationSubscriptions() {
    connected = false;
  }

  void addIncoming(ChatMessage message) {
    _incomingController.add(message);
  }

  void addStatus(Map<String, dynamic> event) {
    _statusController.add(event);
  }

  @override
  void dispose() {
    _incomingController.close();
    _statusController.close();
  }
}
