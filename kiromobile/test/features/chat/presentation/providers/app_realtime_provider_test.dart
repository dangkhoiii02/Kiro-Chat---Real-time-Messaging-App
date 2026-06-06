import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/core/realtime/presence_repository.dart';
import 'package:kiromobile/core/realtime/stomp_service.dart';
import 'package:kiromobile/features/chat/data/models/acknowledge_receive_message_request.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/mark_message_seen_request.dart';
import 'package:kiromobile/features/chat/data/models/page_response.dart';
import 'package:kiromobile/features/chat/data/models/presence_event.dart';
import 'package:kiromobile/features/chat/data/models/send_message_request.dart';
import 'package:kiromobile/features/chat/data/models/user_chat_list.dart';
import 'package:kiromobile/features/chat/data/repositories/conversation_repository.dart';
import 'package:kiromobile/features/chat/data/repositories/message_repository.dart';
import 'package:kiromobile/features/chat/presentation/providers/app_realtime_provider.dart';
import 'package:kiromobile/features/chat/presentation/providers/chat_list_provider.dart';

void main() {
  test(
    'starts app realtime and handles incoming message outside detail',
    () async {
      final fakeConversationRepository = _FakeConversationRepository([
        _directConversation,
      ]);
      final fakeMessageRepository = _FakeMessageRepository();
      final fakeStompService = _FakeStompService();
      final container = _container(
        fakeConversationRepository,
        fakeMessageRepository,
        fakeStompService,
      );
      addTearDown(container.dispose);

      await container
          .read(chatListControllerProvider.notifier)
          .loadConversations();
      await container.read(appRealtimeControllerProvider.notifier).start();

      fakeStompService.addIncoming(
        _message(
          messageId: 'incoming-1',
          owner: false,
          messageState: MessageState.sent,
        ),
      );
      await pumpEventQueue(times: 5);

      final conversation = container
          .read(chatListControllerProvider)
          .conversations
          .single;
      expect(fakeMessageRepository.acknowledgedMessageIds, ['incoming-1']);
      expect(conversation.lastMessage?.messageId, 'incoming-1');
      expect(conversation.lastMessage?.messageState, MessageState.delivered);
      expect(conversation.unreadCount, 1);
    },
  );

  test('does not increase unread count for active conversation', () async {
    final fakeConversationRepository = _FakeConversationRepository([
      _directConversation,
    ]);
    final fakeMessageRepository = _FakeMessageRepository();
    final fakeStompService = _FakeStompService();
    final container = _container(
      fakeConversationRepository,
      fakeMessageRepository,
      fakeStompService,
    );
    addTearDown(container.dispose);

    await container
        .read(chatListControllerProvider.notifier)
        .loadConversations();
    container
        .read(activeChatConversationIdProvider.notifier)
        .setActive(_directConversation.conversationId);
    await container.read(appRealtimeControllerProvider.notifier).start();

    fakeStompService.addIncoming(
      _message(messageId: 'incoming-1', owner: false),
    );
    await pumpEventQueue(times: 5);

    final conversation = container
        .read(chatListControllerProvider)
        .conversations
        .single;
    expect(conversation.unreadCount, 0);
  });

  test('subscribes group topics after conversations are loaded', () async {
    final fakeConversationRepository = _FakeConversationRepository([
      _directConversation,
      _groupConversation,
    ]);
    final fakeMessageRepository = _FakeMessageRepository();
    final fakeStompService = _FakeStompService();
    final container = _container(
      fakeConversationRepository,
      fakeMessageRepository,
      fakeStompService,
    );
    addTearDown(container.dispose);

    await container.read(appRealtimeControllerProvider.notifier).start();
    await container
        .read(chatListControllerProvider.notifier)
        .loadConversations();

    expect(fakeStompService.groupConversationIds, ['group-1']);
    expect(fakeStompService.presenceUserIds, ['other-user']);
  });

  test('status events update chat list last message state', () async {
    final fakeConversationRepository = _FakeConversationRepository([
      _directConversation.copyWith(
        lastMessage: _message(
          messageId: 'mine-1',
          owner: true,
          messageState: MessageState.sent,
        ),
      ),
    ]);
    final fakeMessageRepository = _FakeMessageRepository();
    final fakeStompService = _FakeStompService();
    final container = _container(
      fakeConversationRepository,
      fakeMessageRepository,
      fakeStompService,
    );
    addTearDown(container.dispose);

    await container
        .read(chatListControllerProvider.notifier)
        .loadConversations();
    await container.read(appRealtimeControllerProvider.notifier).start();

    final payload = _message(
      messageId: 'mine-1',
      owner: true,
      messageState: MessageState.seen,
    ).toJson()..remove('owner');
    fakeStompService.addStatus({
      'destination': '/user/queue/messages.seen',
      'payload': payload,
    });
    await pumpEventQueue(times: 5);

    expect(
      container
          .read(chatListControllerProvider)
          .conversations
          .single
          .lastMessage
          ?.messageState,
      MessageState.seen,
    );
  });

  test('presence events update matching direct conversations', () async {
    final fakeConversationRepository = _FakeConversationRepository([
      _directConversation.copyWith(isOnline: false),
      _groupConversation,
    ]);
    final fakeMessageRepository = _FakeMessageRepository();
    final fakeStompService = _FakeStompService();
    final container = _container(
      fakeConversationRepository,
      fakeMessageRepository,
      fakeStompService,
    );
    addTearDown(container.dispose);

    await container
        .read(chatListControllerProvider.notifier)
        .loadConversations();
    await container.read(appRealtimeControllerProvider.notifier).start();

    fakeStompService.addPresence(
      PresenceEvent(
        userId: 'other-user',
        isOnline: true,
        lastSeen: DateTime.parse('2026-06-05T08:30:00.000Z'),
      ),
    );
    await pumpEventQueue(times: 5);

    final conversations = container
        .read(chatListControllerProvider)
        .conversations;
    expect(
      conversations
          .singleWhere(
            (conversation) => conversation.conversationId == 'conversation-1',
          )
          .isOnline,
      isTrue,
    );
    expect(
      conversations
          .singleWhere(
            (conversation) => conversation.conversationId == 'group-1',
          )
          .isOnline,
      isTrue,
    );
  });

  test(
    'loads presence snapshots after direct conversations are loaded',
    () async {
      final fakeConversationRepository = _FakeConversationRepository([
        _directConversation.copyWith(isOnline: false),
        _groupConversation,
      ]);
      final fakeMessageRepository = _FakeMessageRepository();
      final fakePresenceRepository = _FakePresenceRepository()
        ..presenceByUserId['other-user'] = const PresenceEvent(
          userId: 'other-user',
          isOnline: true,
        );
      final fakeStompService = _FakeStompService();
      final container = _container(
        fakeConversationRepository,
        fakeMessageRepository,
        fakeStompService,
        presenceRepository: fakePresenceRepository,
      );
      addTearDown(container.dispose);

      await container
          .read(chatListControllerProvider.notifier)
          .loadConversations();

      expect(fakePresenceRepository.presenceUserIds, ['other-user']);
      expect(
        container
            .read(chatListControllerProvider)
            .conversations
            .singleWhere(
              (conversation) => conversation.conversationId == 'conversation-1',
            )
            .isOnline,
        isTrue,
      );
    },
  );

  test('start and stop update current user presence', () async {
    final fakeConversationRepository = _FakeConversationRepository([]);
    final fakeMessageRepository = _FakeMessageRepository();
    final fakePresenceRepository = _FakePresenceRepository();
    final fakeStompService = _FakeStompService();
    final container = _container(
      fakeConversationRepository,
      fakeMessageRepository,
      fakeStompService,
      presenceRepository: fakePresenceRepository,
    );
    addTearDown(container.dispose);

    await container.read(appRealtimeControllerProvider.notifier).start();
    await container.read(appRealtimeControllerProvider.notifier).stop();

    expect(fakePresenceRepository.heartbeatCallCount, 1);
    expect(fakePresenceRepository.explicitOfflineCallCount, 1);
  });
}

ProviderContainer _container(
  ConversationRepository conversationRepository,
  MessageRepository messageRepository,
  StompService stompService, {
  PresenceRepository? presenceRepository,
}) {
  return ProviderContainer(
    overrides: [
      conversationRepositoryProvider.overrideWithValue(conversationRepository),
      messageRepositoryProvider.overrideWithValue(messageRepository),
      presenceRepositoryProvider.overrideWithValue(
        presenceRepository ?? _FakePresenceRepository(),
      ),
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
  remoteUserId: 'other-user',
);

const _groupConversation = Conversation(
  conversationId: 'group-1',
  conversationName: 'Group Chat',
  isGroup: true,
  unreadCount: 0,
  isOnline: true,
);

ChatMessage _message({
  required String messageId,
  required bool owner,
  MessageState messageState = MessageState.sent,
}) {
  return ChatMessage(
    messageId: messageId,
    conversationId: _directConversation.conversationId,
    senderId: owner ? 'current-user' : 'other-user',
    type: MessageType.text,
    content: 'Hello',
    messageState: messageState,
    timestamp: DateTime.parse('2026-06-03T10:00:00.000Z'),
    isDeleted: false,
    owner: owner,
  );
}

class _FakeConversationRepository extends ConversationRepository {
  _FakeConversationRepository(this.conversations) : super(Dio());

  final List<Conversation> conversations;

  @override
  Future<UserChatList> getConversations({
    String? query,
    int page = 0,
    int size = 20,
  }) async {
    return UserChatList(
      conversations: PageResponse(
        content: conversations,
        totalElements: conversations.length,
        totalPages: 1,
        pageNumber: 0,
        pageSize: conversations.length,
        last: true,
      ),
    );
  }
}

class _FakeMessageRepository extends MessageRepository {
  _FakeMessageRepository() : super(Dio());

  final acknowledgedMessageIds = <String>[];

  @override
  Future<ChatMessage> acknowledgeReceived({
    required bool isGroup,
    required AcknowledgeReceiveMessageRequest request,
  }) async {
    acknowledgedMessageIds.add(request.messageId);

    return _message(
      messageId: request.messageId,
      owner: false,
      messageState: MessageState.delivered,
    );
  }

  @override
  Future<ChatMessage> sendMessage({
    required bool isGroup,
    required SendMessageRequest request,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<void> markConversationSeen({required MarkMessageSeenRequest request}) {
    throw UnimplementedError();
  }
}

class _FakePresenceRepository extends PresenceRepository {
  _FakePresenceRepository() : super(Dio());

  final presenceByUserId = <String, PresenceEvent>{};
  final presenceUserIds = <String>[];
  int heartbeatCallCount = 0;
  int explicitOfflineCallCount = 0;

  @override
  Future<PresenceEvent> getPresence(String userId) async {
    presenceUserIds.add(userId);
    return presenceByUserId[userId] ??
        PresenceEvent(userId: userId, isOnline: false);
  }

  @override
  Future<void> heartbeat() async {
    heartbeatCallCount += 1;
  }

  @override
  Future<void> explicitOffline() async {
    explicitOfflineCallCount += 1;
  }
}

class _FakeStompService extends StompService {
  _FakeStompService() : super(accessTokenReader: () async => 'access-token');

  final _incomingController = StreamController<ChatMessage>.broadcast();
  final _statusController = StreamController<Map<String, dynamic>>.broadcast();
  final _presenceController = StreamController<PresenceEvent>.broadcast();
  final groupConversationIds = <String>[];
  final presenceUserIds = <String>[];
  bool connected = false;

  @override
  Stream<ChatMessage> get incomingMessages => _incomingController.stream;

  @override
  Stream<Map<String, dynamic>> get statusEvents => _statusController.stream;

  @override
  Stream<PresenceEvent> get presenceEvents => _presenceController.stream;

  @override
  bool get isConnected => connected;

  @override
  Future<void> connect() async {
    connected = true;
  }

  @override
  void watchGroupConversations(Iterable<Conversation> conversations) {
    groupConversationIds
      ..clear()
      ..addAll(
        conversations
            .where((conversation) => conversation.isGroup)
            .map((conversation) => conversation.conversationId),
      );
  }

  @override
  void watchPresenceUsers(Iterable<String?> userIds) {
    presenceUserIds
      ..clear()
      ..addAll(userIds.whereType<String>());
  }

  @override
  void disconnect() {
    connected = false;
    groupConversationIds.clear();
  }

  void addIncoming(ChatMessage message) {
    _incomingController.add(message);
  }

  void addStatus(Map<String, dynamic> event) {
    _statusController.add(event);
  }

  void addPresence(PresenceEvent event) {
    _presenceController.add(event);
  }

  @override
  void dispose() {
    _incomingController.close();
    _statusController.close();
    _presenceController.close();
  }
}
