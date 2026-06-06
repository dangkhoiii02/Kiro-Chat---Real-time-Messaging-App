import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/contact/data/models/contact_profile.dart';
import 'package:kiromobile/features/contact/data/models/friendship_status.dart';
import 'package:kiromobile/features/contact/data/repositories/contact_repository.dart';
import 'package:kiromobile/features/contact/presentation/providers/contacts_controller.dart';
import 'package:kiromobile/features/contact/presentation/providers/friend_requests_controller.dart';

enum UserSearchStatus { initial, loading, success, failure }

class UserSearchState {
  const UserSearchState({
    required this.status,
    this.users = const [],
    this.query = '',
    this.errorMessage,
    this.actionErrorMessage,
  });

  const UserSearchState.initial()
    : status = UserSearchStatus.initial,
      users = const [],
      query = '',
      errorMessage = null,
      actionErrorMessage = null;

  final UserSearchStatus status;
  final List<ContactProfile> users;
  final String query;
  final String? errorMessage;
  final String? actionErrorMessage;

  UserSearchState copyWith({
    UserSearchStatus? status,
    List<ContactProfile>? users,
    String? query,
    String? errorMessage,
    String? actionErrorMessage,
  }) {
    return UserSearchState(
      status: status ?? this.status,
      users: users ?? this.users,
      query: query ?? this.query,
      errorMessage: errorMessage,
      actionErrorMessage: actionErrorMessage,
    );
  }
}

final userSearchControllerProvider =
    NotifierProvider<UserSearchController, UserSearchState>(
      UserSearchController.new,
    );

class UserSearchController extends Notifier<UserSearchState> {
  @override
  UserSearchState build() {
    return const UserSearchState.initial();
  }

  Future<void> searchUsers(String query) async {
    final trimmedQuery = query.trim();
    if (trimmedQuery.isEmpty) {
      state = const UserSearchState.initial();
      return;
    }

    state = state.copyWith(
      status: UserSearchStatus.loading,
      query: trimmedQuery,
      errorMessage: null,
      actionErrorMessage: null,
    );

    try {
      final result = await ref
          .read(contactRepositoryProvider)
          .searchUsers(query: trimmedQuery);
      state = UserSearchState(
        status: UserSearchStatus.success,
        users: result.users.content,
        query: trimmedQuery,
      );
    } catch (e) {
      state = state.copyWith(
        status: UserSearchStatus.failure,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> sendRequest(String userId) async {
    await _mutateStatus(
      userId: userId,
      action: () =>
          ref.read(contactRepositoryProvider).sendContactRequest(userId),
      nextStatus: FriendshipStatus.friendRequestSent,
    );
  }

  Future<void> cancelRequest(String userId) async {
    await _mutateStatus(
      userId: userId,
      action: () =>
          ref.read(contactRepositoryProvider).cancelContactRequest(userId),
      nextStatus: FriendshipStatus.notConnected,
    );
  }

  Future<void> acceptRequest(String userId) async {
    final accepted = await _mutateStatus(
      userId: userId,
      action: () =>
          ref.read(contactRepositoryProvider).acceptContactRequest(userId),
      nextStatus: FriendshipStatus.friends,
    );
    if (accepted) {
      await ref.read(contactsControllerProvider.notifier).loadFriends();
      await ref.read(friendRequestsControllerProvider.notifier).loadRequests();
    }
  }

  Future<void> rejectRequest(String userId) async {
    await _mutateStatus(
      userId: userId,
      action: () =>
          ref.read(contactRepositoryProvider).rejectContactRequest(userId),
      nextStatus: FriendshipStatus.notConnected,
    );
  }

  Future<void> blockUser(String userId) async {
    final blocked = await _mutateStatus(
      userId: userId,
      action: () => ref.read(contactRepositoryProvider).blockUser(userId),
      nextStatus: FriendshipStatus.blocked,
    );
    if (blocked) {
      ref.read(contactsControllerProvider.notifier).removeFriendLocally(userId);
      ref
          .read(friendRequestsControllerProvider.notifier)
          .removeRequestLocally(userId);
    }
  }

  Future<void> unblockUser(String userId) async {
    await _mutateStatus(
      userId: userId,
      action: () => ref.read(contactRepositoryProvider).unblockUser(userId),
      nextStatus: FriendshipStatus.notConnected,
    );
  }

  void markUserBlocked(String userId) {
    _replaceUserStatus(userId, FriendshipStatus.blocked);
  }

  Future<Conversation?> openChat(String userId) async {
    try {
      var conversation = await ref
          .read(contactRepositoryProvider)
          .openOrCreateDirectConversation(userId);

      final userProfile = state.users.firstWhere(
        (u) => u.userId == userId,
        orElse: () => const ContactProfile(
          userId: '',
          friendshipStatus: FriendshipStatus.unknown,
        ),
      );
      if (userProfile.userId.isNotEmpty) {
        conversation = conversation.copyWith(
          conversationName: userProfile.displayName,
          avatarUrl: userProfile.profilePictureUrl,
        );
      }

      state = state.copyWith(actionErrorMessage: null);
      return conversation;
    } catch (e) {
      state = state.copyWith(actionErrorMessage: e.toString());
      return null;
    }
  }

  Future<bool> _mutateStatus({
    required String userId,
    required Future<void> Function() action,
    required FriendshipStatus nextStatus,
  }) async {
    final previousState = state;

    try {
      await action();
      state = previousState.copyWith(actionErrorMessage: null);
      _replaceUserStatus(userId, nextStatus);
      return true;
    } catch (e) {
      state = previousState.copyWith(actionErrorMessage: e.toString());
      return false;
    }
  }

  void _replaceUserStatus(String userId, FriendshipStatus nextStatus) {
    state = state.copyWith(
      users: state.users
          .map(
            (user) => user.userId == userId
                ? user.copyWith(friendshipStatus: nextStatus)
                : user,
          )
          .toList(),
      actionErrorMessage: null,
    );
  }
}
