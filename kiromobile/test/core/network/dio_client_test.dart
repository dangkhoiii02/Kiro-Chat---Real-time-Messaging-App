import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/core/network/dio_client.dart';

void main() {
  test('adds bearer token to request when access token exists', () async {
    final dioClient = DioClient(accessTokenReader: () async => 'mobile-token');
    final adapter = _CapturingHttpClientAdapter();
    dioClient.dio.httpClientAdapter = adapter;

    await dioClient.dio.get('/conversations');

    expect(
      adapter.capturedOptions.headers['Authorization'],
      'Bearer mobile-token',
    );
  });
}

class _CapturingHttpClientAdapter implements HttpClientAdapter {
  late RequestOptions capturedOptions;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    capturedOptions = options;
    return ResponseBody.fromString('{}', 200);
  }

  @override
  void close({bool force = false}) {}
}
