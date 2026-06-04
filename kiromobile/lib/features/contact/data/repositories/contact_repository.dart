import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/network/dio_client.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/contact/data/models/contact_profile.dart';
import 'package:kiromobile/features/contact/data/models/contact_request.dart';
import 'package:kiromobile/features/contact/data/models/friend.dart';

final contactRepositoryProvider = Provider<ContactRepository>((ref) {
  final dio = ref.watch(dioClientProvider).dio;
  return ContactRepository(dio);
});

class ContactRepository {
  const ContactRepository(this._dio);

  final Dio _dio;

  Future<RestFriendList> getFriends({String? query}) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/friends',
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'q': query.trim(),
      },
    );

    return RestFriendList.fromJson(response.data ?? <String, dynamic>{});
  }

  Future<RestContactProfileList> searchUsers({required String query}) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/users/search',
      queryParameters: {'q': query.trim()},
    );

    return RestContactProfileList.fromJson(
      response.data ?? <String, dynamic>{},
    );
  }

  Future<RestContactProfileList> suggestUsers({String? query}) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/users/suggests',
      queryParameters: {
        if (query != null && query.trim().isNotEmpty) 'q': query.trim(),
      },
    );

    return RestContactProfileList.fromJson(
      response.data ?? <String, dynamic>{},
    );
  }

  Future<RestContactRequestList> getContactRequests() async {
    final response = await _dio.get<Map<String, dynamic>>('/contact-requests');

    return RestContactRequestList.fromJson(
      response.data ?? <String, dynamic>{},
    );
  }

  Future<void> sendContactRequest(String userId) async {
    await _dio.post<void>('/contact-requests/$userId');
  }

  Future<void> cancelContactRequest(String userId) async {
    await _dio.delete<void>('/contact-requests/$userId');
  }

  Future<void> acceptContactRequest(String requestUserId) async {
    await _dio.post<void>('/contact-requests/user/$requestUserId/accept');
  }

  Future<void> rejectContactRequest(String requestUserId) async {
    await _dio.post<void>('/contact-requests/user/$requestUserId/reject');
  }

  Future<Conversation> openOrCreateDirectConversation(String userId) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/conversations/user/$userId',
      );
      return _conversationFromData(response.data);
    } on DioException catch (error) {
      if (error.response?.statusCode != 404) {
        rethrow;
      }

      final response = await _dio.post<Map<String, dynamic>>(
        '/conversations',
        queryParameters: {'userId': userId},
      );
      return _conversationFromData(response.data);
    }
  }

  Conversation _conversationFromData(Map<String, dynamic>? data) {
    final responseData = data ?? <String, dynamic>{};
    final conversation = responseData['conversation'];

    if (conversation is Map<String, dynamic>) {
      return Conversation.fromJson(conversation);
    }

    return Conversation.fromJson(responseData);
  }
}
