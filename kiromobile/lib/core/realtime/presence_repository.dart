import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/network/dio_client.dart';
import 'package:kiromobile/features/chat/data/models/presence_event.dart';

final presenceRepositoryProvider = Provider<PresenceRepository>((ref) {
  final dio = ref.watch(dioClientProvider).dio;
  return PresenceRepository(dio);
});

class PresenceRepository {
  const PresenceRepository(this._dio);

  final Dio _dio;

  Future<PresenceEvent> getPresence(String userId) async {
    final response = await _dio.get<Map<String, dynamic>>('/presence/$userId');
    return PresenceEvent.fromJson(response.data ?? <String, dynamic>{});
  }

  Future<void> heartbeat() async {
    await _dio.post<void>('/presence/heartbeat');
  }

  Future<void> explicitOffline() async {
    await _dio.post<void>('/presence/offline');
  }
}
