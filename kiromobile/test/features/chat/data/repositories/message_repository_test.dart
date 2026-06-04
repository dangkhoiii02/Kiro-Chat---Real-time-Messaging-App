import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/chat/data/models/acknowledge_receive_message_request.dart';
import 'package:kiromobile/features/chat/data/models/mark_message_seen_request.dart';
import 'package:kiromobile/features/chat/data/models/message_type.dart';
import 'package:kiromobile/features/chat/data/models/send_message_request.dart';
import 'package:kiromobile/features/chat/data/repositories/message_repository.dart';

void main() {
  test(
    'getMessages calls endpoint with cursor params and parses response',
    () async {
      final dio = Dio();
      final adapter = _MessageListAdapter();
      dio.httpClientAdapter = adapter;
      final repository = MessageRepository(dio);

      final result = await repository.getMessages(
        conversationId: 'conversation-1',
        beforeMessageId: 'message-before',
        limit: 10,
      );

      expect(
        adapter.capturedOptions.path,
        '/conversations/conversation-1/messages',
      );
      expect(adapter.capturedOptions.queryParameters, {
        'before': 'message-before',
        'limit': 10,
      });
      expect(result.messages, hasLength(1));
      expect(result.messages.first.messageId, 'message-1');
      expect(result.messages.first.content, 'Hello from backend');
      expect(result.hasMore, true);
      expect(result.nextBeforeMessageId, 'message-older');
    },
  );

  test('sendMessage calls direct endpoint and parses sent message', () async {
    final dio = Dio();
    final adapter = _SendMessageAdapter();
    dio.httpClientAdapter = adapter;
    final repository = MessageRepository(dio);

    final result = await repository.sendMessage(
      isGroup: false,
      request: const SendMessageRequest(
        conversationId: 'conversation-1',
        type: MessageType.text,
        content: 'Hello direct',
      ),
    );

    expect(adapter.capturedOptions.method, 'POST');
    expect(adapter.capturedOptions.path, '/messages/individual/send');
    expect(adapter.capturedBody['conversationId'], 'conversation-1');
    expect(adapter.capturedBody['type'], 'text');
    expect(adapter.capturedBody['content'], 'Hello direct');
    expect(result.messageId, 'sent-message-1');
    expect(result.content, 'Hello direct');
    expect(result.owner, true);
  });

  test('sendMessage calls group endpoint', () async {
    final dio = Dio();
    final adapter = _SendMessageAdapter();
    dio.httpClientAdapter = adapter;
    final repository = MessageRepository(dio);

    await repository.sendMessage(
      isGroup: true,
      request: const SendMessageRequest(
        conversationId: 'group-1',
        type: MessageType.text,
        content: 'Hello group',
      ),
    );

    expect(adapter.capturedOptions.path, '/messages/group/send');
    expect(adapter.capturedBody['conversationId'], 'group-1');
    expect(adapter.capturedBody['content'], 'Hello group');
  });

  test('acknowledgeReceived calls direct receive endpoint', () async {
    final dio = Dio();
    final adapter = _ReceiveMessageAdapter();
    dio.httpClientAdapter = adapter;
    final repository = MessageRepository(dio);

    final result = await repository.acknowledgeReceived(
      isGroup: false,
      request: const AcknowledgeReceiveMessageRequest(messageId: 'message-1'),
    );

    expect(adapter.capturedOptions.method, 'PATCH');
    expect(adapter.capturedOptions.path, '/messages/individual/receive');
    expect(adapter.capturedBody, {'messageId': 'message-1'});
    expect(result.messageId, 'message-1');
    expect(result.messageState.value, 'delivered');
    expect(result.owner, false);
  });

  test('acknowledgeReceived calls group receive endpoint', () async {
    final dio = Dio();
    final adapter = _ReceiveMessageAdapter();
    dio.httpClientAdapter = adapter;
    final repository = MessageRepository(dio);

    await repository.acknowledgeReceived(
      isGroup: true,
      request: const AcknowledgeReceiveMessageRequest(messageId: 'message-1'),
    );

    expect(adapter.capturedOptions.path, '/messages/group/receive');
  });

  test(
    'markConversationSeen calls seen endpoint with last seen message',
    () async {
      final dio = Dio();
      final adapter = _SeenMessageAdapter();
      dio.httpClientAdapter = adapter;
      final repository = MessageRepository(dio);

      await repository.markConversationSeen(
        request: const MarkMessageSeenRequest(
          conversationId: 'conversation-1',
          lastSeenMessageId: 'message-3',
        ),
      );

      expect(adapter.capturedOptions.method, 'PATCH');
      expect(
        adapter.capturedOptions.path,
        '/conversations/conversation-1/messages/seen',
      );
      expect(adapter.capturedBody, {'lastSeenMessageId': 'message-3'});
    },
  );
}

class _MessageListAdapter implements HttpClientAdapter {
  late RequestOptions capturedOptions;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedOptions = options;

    return ResponseBody.fromString(
      jsonEncode({
        'messages': [
          {
            'messageId': 'message-1',
            'conversationId': 'conversation-1',
            'senderId': 'user-1',
            'type': 'text',
            'content': 'Hello from backend',
            'mediaUrl': null,
            'mediaName': null,
            'messageState': 'sent',
            'timestamp': '2026-05-31T10:00:00.000Z',
            'replyToMessageId': null,
            'isDeleted': false,
            'owner': true,
          },
        ],
        'hasMore': true,
        'nextBeforeMessageId': 'message-older',
      }),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _SendMessageAdapter implements HttpClientAdapter {
  late RequestOptions capturedOptions;
  late Map<String, dynamic> capturedBody;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedOptions = options;
    capturedBody = await _readJsonBody(requestStream);

    return ResponseBody.fromString(
      jsonEncode({
        'messageId': 'sent-message-1',
        'conversationId': capturedBody['conversationId'],
        'senderId': 'current-user',
        'type': capturedBody['type'],
        'content': capturedBody['content'],
        'mediaUrl': null,
        'mediaName': null,
        'messageState': 'sent',
        'timestamp': '2026-06-01T10:00:00.000Z',
        'replyToMessageId': capturedBody['replyToMessageId'],
        'isDeleted': false,
      }),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _ReceiveMessageAdapter implements HttpClientAdapter {
  late RequestOptions capturedOptions;
  late Map<String, dynamic> capturedBody;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedOptions = options;
    capturedBody = await _readJsonBody(requestStream);

    return ResponseBody.fromString(
      jsonEncode({
        'messageId': capturedBody['messageId'],
        'conversationId': 'conversation-1',
        'senderId': 'other-user',
        'type': 'text',
        'content': 'Hello',
        'mediaUrl': null,
        'mediaName': null,
        'messageState': 'delivered',
        'timestamp': '2026-06-01T10:00:00.000Z',
        'replyToMessageId': null,
        'isDeleted': false,
      }),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _SeenMessageAdapter implements HttpClientAdapter {
  late RequestOptions capturedOptions;
  late Map<String, dynamic> capturedBody;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedOptions = options;
    capturedBody = await _readJsonBody(requestStream);

    return ResponseBody.fromString(
      'null',
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Future<Map<String, dynamic>> _readJsonBody(
  Stream<Uint8List>? requestStream,
) async {
  if (requestStream == null) {
    return <String, dynamic>{};
  }

  final bytes = await requestStream.expand((chunk) => chunk).toList();
  final bodyText = utf8.decode(bytes);
  return jsonDecode(bodyText) as Map<String, dynamic>;
}
