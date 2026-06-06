import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/contact/data/repositories/contact_repository.dart';

void main() {
  test('getFriends calls friends endpoint with query', () async {
    final adapter = _ContactAdapter((options) async {
      expect(options.method, 'GET');
      expect(options.path, '/friends');
      expect(options.queryParameters, {'q': 'alice'});

      return _jsonResponse({
        'friends': {
          'content': [],
          'totalElements': 0,
          'totalPages': 0,
          'number': 0,
          'size': 20,
          'last': true,
        },
      });
    });
    final repository = ContactRepository(_dio(adapter));

    await repository.getFriends(query: 'alice');
  });

  test('searchUsers calls users search endpoint with query', () async {
    final adapter = _ContactAdapter((options) async {
      expect(options.method, 'GET');
      expect(options.path, '/users/search');
      expect(options.queryParameters, {'q': 'bob'});

      return _jsonResponse({
        'users': {
          'content': [],
          'totalElements': 0,
          'totalPages': 0,
          'number': 0,
          'size': 20,
          'last': true,
        },
      });
    });
    final repository = ContactRepository(_dio(adapter));

    await repository.searchUsers(query: 'bob');
  });

  test('getContactRequests calls contact requests endpoint', () async {
    final adapter = _ContactAdapter((options) async {
      expect(options.method, 'GET');
      expect(options.path, '/contact-requests');
      expect(options.queryParameters, isEmpty);

      return _jsonResponse({
        'requests': {
          'content': [],
          'totalElements': 0,
          'totalPages': 0,
          'number': 0,
          'size': 20,
          'last': true,
        },
      });
    });
    final repository = ContactRepository(_dio(adapter));

    await repository.getContactRequests();
  });

  test('request mutations call the backend contract endpoints', () async {
    final calls = <String>[];
    final adapter = _ContactAdapter((options) async {
      calls.add('${options.method} ${options.path}');
      return _jsonResponse({});
    });
    final repository = ContactRepository(_dio(adapter));

    await repository.sendContactRequest('user-1');
    await repository.cancelContactRequest('user-1');
    await repository.acceptContactRequest('request-user-1');
    await repository.rejectContactRequest('request-user-1');

    expect(calls, [
      'POST /contact-requests/user-1',
      'DELETE /contact-requests/user-1',
      'POST /contact-requests/user/request-user-1/accept',
      'POST /contact-requests/user/request-user-1/reject',
    ]);
  });

  test(
    'friend and block mutations call the backend contract endpoints',
    () async {
      final calls = <String>[];
      final adapter = _ContactAdapter((options) async {
        calls.add('${options.method} ${options.path}');
        return _jsonResponse({});
      });
      final repository = ContactRepository(_dio(adapter));

      await repository.removeFriend('friend-1');
      await repository.blockUser('user-1');
      await repository.unblockUser('user-1');

      expect(calls, [
        'DELETE /friends/friend-1',
        'POST /blocks/user-1',
        'DELETE /blocks/user-1',
      ]);
    },
  );

  test('getBlockedUsers calls blocks endpoint with paging', () async {
    final adapter = _ContactAdapter((options) async {
      expect(options.method, 'GET');
      expect(options.path, '/blocks');
      expect(options.queryParameters, {'page': 1, 'size': 10});

      return _jsonResponse({
        'users': {
          'content': [],
          'totalElements': 0,
          'totalPages': 0,
          'number': 1,
          'size': 10,
          'last': true,
        },
      });
    });
    final repository = ContactRepository(_dio(adapter));

    await repository.getBlockedUsers(page: 1, size: 10);
  });

  test(
    'openOrCreateDirectConversation returns existing direct conversation',
    () async {
      final calls = <String>[];
      final adapter = _ContactAdapter((options) async {
        calls.add('${options.method} ${options.path}');
        expect(options.queryParameters, isEmpty);

        return _jsonResponse(_conversationJson('conversation-1'));
      });
      final repository = ContactRepository(_dio(adapter));

      final result = await repository.openOrCreateDirectConversation('user-1');

      expect(calls, ['GET /conversations/user/user-1']);
      expect(result.conversationId, 'conversation-1');
    },
  );

  test(
    'openOrCreateDirectConversation creates conversation after get 404',
    () async {
      var callCount = 0;
      final adapter = _ContactAdapter((options) async {
        callCount += 1;

        if (callCount == 1) {
          expect(options.method, 'GET');
          expect(options.path, '/conversations/user/user-1');
          throw DioException(
            requestOptions: options,
            response: Response(
              requestOptions: options,
              statusCode: 404,
              data: {'message': 'not found'},
            ),
          );
        }

        expect(options.method, 'POST');
        expect(options.path, '/conversations');
        expect(options.queryParameters, {'userId': 'user-1'});
        return _jsonResponse(_conversationJson('conversation-created'));
      });
      final repository = ContactRepository(_dio(adapter));

      final result = await repository.openOrCreateDirectConversation('user-1');

      expect(result.conversationId, 'conversation-created');
    },
  );
}

Dio _dio(HttpClientAdapter adapter) {
  final dio = Dio();
  dio.httpClientAdapter = adapter;
  return dio;
}

Map<String, dynamic> _conversationJson(String conversationId) {
  return {
    'conversationId': conversationId,
    'conversationName': 'Alice',
    'avatarUrl': null,
    'lastMessage': null,
    'unreadCount': 0,
    'isOnline': false,
    'isGroup': false,
    'remoteUserId': 'user-1',
  };
}

ResponseBody _jsonResponse(Map<String, dynamic> body) {
  return ResponseBody.fromString(
    jsonEncode(body),
    200,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}

class _ContactAdapter implements HttpClientAdapter {
  _ContactAdapter(this.handler);

  final Future<ResponseBody> Function(RequestOptions options) handler;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    return handler(options);
  }

  @override
  void close({bool force = false}) {}
}
