import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/network/dio_client.dart';
import 'package:kiromobile/features/chat/data/models/acknowledge_receive_message_request.dart';
import 'package:kiromobile/features/chat/data/models/chat_message.dart';
import 'package:kiromobile/features/chat/data/models/conversation_message_list.dart';
import 'package:kiromobile/features/chat/data/models/mark_message_seen_request.dart';
import 'package:kiromobile/features/chat/data/models/send_message_request.dart';

final messageRepositoryProvider = Provider<MessageRepository>((ref) {
  final dio = ref.watch(dioClientProvider).dio;
  return MessageRepository(dio);
});

class MessageRepository {
  const MessageRepository(this._dio);

  final Dio _dio;

  Future<ConversationMessageList> getMessages({
    required String conversationId,
    String? beforeMessageId,
    int limit = 20,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/conversations/$conversationId/messages',
      queryParameters: {
        if (beforeMessageId != null && beforeMessageId.isNotEmpty)
          'before': beforeMessageId,
        'limit': limit,
      },
    );

    return ConversationMessageList.fromJson(
      response.data ?? <String, dynamic>{},
    );
  }

  Future<ChatMessage> sendMessage({
    required bool isGroup,
    required SendMessageRequest request,
  }) async {
    final endpoint = isGroup
        ? '/messages/group/send'
        : '/messages/individual/send';
    final response = await _dio.post<Map<String, dynamic>>(
      endpoint,
      data: request.toJson(),
    );

    return ChatMessage.fromJson({...?response.data, 'owner': true});
  }

  Future<ChatMessage> acknowledgeReceived({
    required bool isGroup,
    required AcknowledgeReceiveMessageRequest request,
  }) async {
    final endpoint = isGroup
        ? '/messages/group/receive'
        : '/messages/individual/receive';
    final response = await _dio.patch<Map<String, dynamic>>(
      endpoint,
      data: request.toJson(),
    );

    return ChatMessage.fromJson({...?response.data, 'owner': false});
  }

  Future<void> markConversationSeen({
    required MarkMessageSeenRequest request,
  }) async {
    await _dio.patch<void>(
      '/conversations/${request.conversationId}/messages/seen',
      data: request.toJson(),
    );
  }
}
