import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/network/dio_client.dart';
import 'package:kiromobile/features/chat/data/models/user_chat_list.dart';

final conversationRepositoryProvider = Provider<ConversationRepository>((ref) {
  final dio = ref.watch(dioClientProvider).dio;
  return ConversationRepository(dio);
});

class ConversationRepository {
  const ConversationRepository(this._dio);

  final Dio _dio;

  Future<UserChatList> getConversations({
    String? query,
    int page = 0,
    int size = 20,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/conversations',
      queryParameters: {
        if (query != null && query.isNotEmpty) 'q': query,
        'page': page,
        'size': size,
      },
    );

    return UserChatList.fromJson(response.data ?? <String, dynamic>{});
  }
}
