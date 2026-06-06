import 'dart:async';
import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/config/app_config.dart';
import 'package:kiromobile/core/logging/app_logger.dart';
import 'package:kiromobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/presence_event.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

final stompServiceProvider = Provider<StompService>((ref) {
  final authRepository = ref.watch(authRepositoryProvider);
  final service = StompService(
    accessTokenReader: authRepository.getAccessToken,
  );

  ref.onDispose(service.dispose);

  return service;
});

class StompService {
  StompService({required Future<String?> Function() accessTokenReader})
    : _accessTokenReader = accessTokenReader;

  final Future<String?> Function() _accessTokenReader;
  final _incomingMessagesController = StreamController<ChatMessage>.broadcast();
  final _statusEventsController =
      StreamController<Map<String, dynamic>>.broadcast();
  final _presenceEventsController = StreamController<PresenceEvent>.broadcast();

  StompClient? _client;
  StompUnsubscribe? _userMessageUnsubscribe;
  final Set<String> _groupConversationIds = {};
  final Set<String> _presenceUserIds = {};
  final Map<String, StompUnsubscribe> _groupMessageUnsubscribes = {};
  final Map<String, StompUnsubscribe> _presenceUnsubscribes = {};
  final List<StompUnsubscribe> _statusUnsubscribes = [];

  Stream<ChatMessage> get incomingMessages =>
      _incomingMessagesController.stream;
  Stream<Map<String, dynamic>> get statusEvents =>
      _statusEventsController.stream;
  Stream<PresenceEvent> get presenceEvents => _presenceEventsController.stream;

  bool get isConnected => _client?.connected ?? false;

  Future<void> connect() async {
    final currentClient = _client;
    if (currentClient != null && currentClient.isActive) {
      return;
    }

    final accessToken = await _accessTokenReader();
    if (accessToken == null || accessToken.isEmpty) {
      throw StateError('Cannot connect STOMP without access token.');
    }

    _client = StompClient(
      config: StompConfig(
        url: AppConfig.socketUrl,
        stompConnectHeaders: {'Authorization': 'Bearer $accessToken'},
        reconnectDelay: const Duration(seconds: 2),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
        onConnect: (_) {
          _userMessageUnsubscribe = null;
          _statusUnsubscribes.clear();
          _groupMessageUnsubscribes.clear();
          _subscribeUserMessageQueue();
          _subscribeStatusTopics();
          _subscribePresenceTopics();
          _subscribeGroupTopics();
        },
        onWebSocketError: (error) {
          appLogger.w('STOMP websocket error: $error');
        },
        onStompError: (frame) {
          appLogger.w('STOMP error: ${frame.body}');
        },
      ),
    )..activate();
  }

  void watchGroupConversations(Iterable<Conversation> conversations) {
    _groupConversationIds
      ..clear()
      ..addAll(
        conversations
            .where((conversation) => conversation.isGroup)
            .map((conversation) => conversation.conversationId),
      );

    if (isConnected) {
      _subscribeGroupTopics();
    }
  }

  void watchPresenceUsers(Iterable<String?> userIds) {
    _presenceUserIds.addAll(
      userIds.whereType<String>().where((userId) => userId.isNotEmpty),
    );

    if (isConnected) {
      _subscribePresenceTopics();
    }
  }

  void clearConversationSubscriptions() {
    for (final unsubscribe in _groupMessageUnsubscribes.values) {
      unsubscribe();
    }
    _groupMessageUnsubscribes.clear();
    _groupConversationIds.clear();
  }

  void disconnect() {
    _userMessageUnsubscribe?.call();
    _userMessageUnsubscribe = null;

    clearConversationSubscriptions();

    for (final unsubscribe in _statusUnsubscribes) {
      unsubscribe();
    }
    _statusUnsubscribes.clear();
    for (final unsubscribe in _presenceUnsubscribes.values) {
      unsubscribe();
    }
    _presenceUnsubscribes.clear();
    _client?.deactivate();
    _client = null;
  }

  void dispose() {
    disconnect();
    _incomingMessagesController.close();
    _statusEventsController.close();
    _presenceEventsController.close();
  }

  void _subscribeUserMessageQueue() {
    final client = _client;

    if (client == null ||
        !client.connected ||
        _userMessageUnsubscribe != null) {
      return;
    }

    _userMessageUnsubscribe = client.subscribe(
      destination: '/user/queue/messages.receive',
      callback: _handleIncomingMessage,
    );
  }

  void _subscribeGroupTopics() {
    final client = _client;

    if (client == null || !client.connected) {
      return;
    }

    final staleConversationIds = _groupMessageUnsubscribes.keys
        .where(
          (conversationId) => !_groupConversationIds.contains(conversationId),
        )
        .toList();
    for (final conversationId in staleConversationIds) {
      _groupMessageUnsubscribes.remove(conversationId)?.call();
    }

    for (final conversationId in _groupConversationIds) {
      if (_groupMessageUnsubscribes.containsKey(conversationId)) {
        continue;
      }

      _groupMessageUnsubscribes[conversationId] = client.subscribe(
        destination: '/topic/messages.receive-$conversationId',
        callback: (frame) => _handleIncomingMessage(frame, conversationId),
      );
    }
  }

  void _subscribeStatusTopics() {
    final client = _client;
    if (client == null || !client.connected || _statusUnsubscribes.isNotEmpty) {
      return;
    }

    for (final destination in const [
      '/user/queue/messages.sent',
      '/user/queue/messages.delivered',
      '/user/queue/messages.seen',
    ]) {
      _statusUnsubscribes.add(
        client.subscribe(
          destination: destination,
          callback: (frame) => _handleStatusEvent(frame, destination),
        ),
      );
    }
  }

  void _subscribePresenceTopics() {
    final client = _client;
    if (client == null || !client.connected) {
      return;
    }

    for (final userId in _presenceUserIds) {
      if (_presenceUnsubscribes.containsKey(userId)) {
        continue;
      }

      _presenceUnsubscribes[userId] = client.subscribe(
        destination: '/topic/presence.user-$userId.update',
        callback: _handlePresenceEvent,
      );
    }
  }

  void _handleIncomingMessage(
    StompFrame frame, [
    String? expectedConversationId,
  ]) {
    final body = frame.body;
    if (body == null || body.isEmpty) {
      return;
    }

    try {
      final json = jsonDecode(body) as Map<String, dynamic>;
      final message = ChatMessage.fromJson({
        ...json,
        'owner': json['owner'] ?? json['isOwner'] ?? false,
      });

      if (expectedConversationId != null &&
          message.conversationId != expectedConversationId) {
        return;
      }

      _incomingMessagesController.add(message);
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot parse STOMP incoming message.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  void _handleStatusEvent(StompFrame frame, String destination) {
    final body = frame.body;
    if (body == null || body.isEmpty) {
      return;
    }

    try {
      _statusEventsController.add({
        'destination': destination,
        'payload': jsonDecode(body),
      });
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot parse STOMP status event.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  void _handlePresenceEvent(StompFrame frame) {
    final body = frame.body;
    if (body == null || body.isEmpty) {
      return;
    }

    try {
      final json = jsonDecode(body) as Map<String, dynamic>;
      _presenceEventsController.add(PresenceEvent.fromJson(json));
    } catch (error, stackTrace) {
      appLogger.w(
        'Cannot parse STOMP presence event.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }
}
