import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/core/realtime/presence_repository.dart';

void main() {
  test('heartbeat calls presence heartbeat endpoint', () async {
    final adapter = _PresenceAdapter((options) async {
      expect(options.method, 'POST');
      expect(options.path, '/presence/heartbeat');

      return _jsonResponse({});
    });
    final repository = PresenceRepository(_dio(adapter));

    await repository.heartbeat();
  });

  test('explicitOffline calls presence offline endpoint', () async {
    final adapter = _PresenceAdapter((options) async {
      expect(options.method, 'POST');
      expect(options.path, '/presence/offline');

      return _jsonResponse({});
    });
    final repository = PresenceRepository(_dio(adapter));

    await repository.explicitOffline();
  });

  test('getPresence calls user presence endpoint and parses status', () async {
    final adapter = _PresenceAdapter((options) async {
      expect(options.method, 'GET');
      expect(options.path, '/presence/user-1');

      return _jsonResponse({
        'userId': 'user-1',
        'status': 'online',
        'lastSeen': '2026-06-05T08:30:00.000Z',
        'connectedAt': '2026-06-05T08:00:00.000Z',
      });
    });
    final repository = PresenceRepository(_dio(adapter));

    final presence = await repository.getPresence('user-1');

    expect(presence.userId, 'user-1');
    expect(presence.isOnline, isTrue);
  });
}

Dio _dio(HttpClientAdapter adapter) {
  final dio = Dio();
  dio.httpClientAdapter = adapter;
  return dio;
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

class _PresenceAdapter implements HttpClientAdapter {
  _PresenceAdapter(this.handler);

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
