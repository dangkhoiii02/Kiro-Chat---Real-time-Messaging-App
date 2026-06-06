import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/network/dio_client.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/contact/data/models/blocked_user.dart';
import 'package:kiromobile/features/contact/data/models/contact_profile.dart';
import 'package:kiromobile/features/contact/data/models/contact_request.dart';
import 'package:kiromobile/features/contact/data/models/friend.dart';
import 'package:kiromobile/core/logging/app_logger.dart';

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
    appLogger.d('searchUsers: Starting search for query="$query"');
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/users/search',
        queryParameters: {'q': query.trim()},
      );
      appLogger.d(
        'searchUsers: GET success. Response data keys: ${response.data?.keys}',
      );

      final result = RestContactProfileList.fromJson(
        response.data ?? <String, dynamic>{},
      );
      appLogger.d(
        'searchUsers: Parsed contact profiles list successfully. Count: ${result.users.content.length}',
      );
      return result;
    } on DioException catch (error) {
      appLogger.e(
        'searchUsers: DioException occurred (status: ${error.response?.statusCode})',
        error: error,
        stackTrace: error.stackTrace,
      );
      rethrow;
    } catch (otherError, stackTrace) {
      appLogger.e(
        'searchUsers: Unexpected error or parsing failure occurred',
        error: otherError,
        stackTrace: stackTrace,
      );
      rethrow;
    }
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

  Future<RestBlockedUserList> getBlockedUsers({
    int page = 0,
    int size = 50,
  }) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/blocks',
      queryParameters: {'page': page, 'size': size},
    );

    return RestBlockedUserList.fromJson(response.data ?? <String, dynamic>{});
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

  Future<void> removeFriend(String friendId) async {
    await _dio.delete<void>('/friends/$friendId');
  }

  Future<void> blockUser(String userId) async {
    await _dio.post<void>('/blocks/$userId');
  }

  Future<void> unblockUser(String userId) async {
    await _dio.delete<void>('/blocks/$userId');
  }

  Future<Conversation> openOrCreateDirectConversation(String userId) async {
    appLogger.d(
      'openOrCreateDirectConversation: Starting flow for userId=$userId',
    );
    try {
      appLogger.d(
        'openOrCreateDirectConversation: GET /conversations/user/$userId',
      );
      final response = await _dio.get<Map<String, dynamic>>(
        '/conversations/user/$userId',
      );
      appLogger.d(
        'openOrCreateDirectConversation: GET success. Parsing conversation data...',
      );
      final conversation = _conversationFromData(response.data);
      appLogger.d(
        'openOrCreateDirectConversation: GET parsed successfully: ${conversation.conversationId}',
      );
      return conversation;
    } on DioException catch (error) {
      if (error.response?.statusCode != 404) {
        appLogger.e(
          'openOrCreateDirectConversation: GET failed with status ${error.response?.statusCode}',
          error: error,
          stackTrace: error.stackTrace,
        );
        rethrow;
      }

      appLogger.i(
        'openOrCreateDirectConversation: GET returned 404. Creating new conversation via POST /conversations...',
      );
      try {
        final response = await _dio.post<Map<String, dynamic>>(
          '/conversations',
          queryParameters: {'userId': userId},
        );
        appLogger.d(
          'openOrCreateDirectConversation: POST success. Parsing conversation data...',
        );
        final conversation = _conversationFromData(response.data);
        appLogger.d(
          'openOrCreateDirectConversation: POST parsed successfully: ${conversation.conversationId}',
        );
        return conversation;
      } on DioException catch (postError) {
        appLogger.e(
          'openOrCreateDirectConversation: POST failed to create conversation',
          error: postError,
          stackTrace: postError.stackTrace,
        );
        rethrow;
      } catch (postOtherError, postStackTrace) {
        appLogger.e(
          'openOrCreateDirectConversation: POST parsing or other error occurred',
          error: postOtherError,
          stackTrace: postStackTrace,
        );
        rethrow;
      }
    } catch (otherError, stackTrace) {
      appLogger.e(
        'openOrCreateDirectConversation: GET parsing or other error occurred',
        error: otherError,
        stackTrace: stackTrace,
      );
      rethrow;
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
