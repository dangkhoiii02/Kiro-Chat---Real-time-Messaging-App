import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/chat/data/repositories/conversation_repository.dart';

void main() {
  test(
    'getConversations calls endpoint with query params and parses response',
    () async {
      final dio = Dio();
      final adapter = _ConversationListAdapter();
      dio.httpClientAdapter = adapter;
      final repository = ConversationRepository(dio);

      final result = await repository.getConversations(
        query: 'team',
        page: 1,
        size: 10,
      );

      expect(adapter.capturedOptions.path, '/conversations');
      expect(adapter.capturedOptions.queryParameters, {
        'q': 'team',
        'page': 1,
        'size': 10,
      });
      expect(result.conversations.content, hasLength(1));
      expect(
        result.conversations.content.first.conversationId,
        'conversation-1',
      );
      expect(
        result.conversations.content.first.conversationName,
        'Mobile Team',
      );
      expect(result.conversations.pageNumber, 1);
      expect(result.conversations.pageSize, 10);
    },
  );
}

class _ConversationListAdapter implements HttpClientAdapter {
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
        'conversations': {
          'content': [
            {
              'conversationId': 'conversation-1',
              'conversationName': 'Mobile Team',
              'avatarUrl': null,
              'lastMessage': null,
              'unreadCount': 3,
              'isOnline': false,
              'isGroup': true,
              'remoteUserId': null,
              'isFollowingUp': false,
              'isArchived': false,
            },
          ],
          'totalElements': 1,
          'totalPages': 1,
          'number': 1,
          'size': 10,
          'last': true,
        },
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
