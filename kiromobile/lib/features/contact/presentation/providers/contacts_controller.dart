import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/core/logging/app_logger.dart';
import 'package:kiromobile/core/realtime/presence_repository.dart';
import 'package:kiromobile/core/realtime/stomp_service.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/presence_event.dart';
import 'package:kiromobile/features/contact/data/models/friend.dart';
import 'package:kiromobile/features/contact/data/repositories/contact_repository.dart';

enum ContactStatus { initial, loading, success, failure }

class ContactsState {
  const ContactsState({
    required this.status,
    this.friends = const [],
    this.errorMessage,
    this.openChatErrorMessage,
    this.actionErrorMessage,
  });

  const ContactsState.initial()
    : status = ContactStatus.initial,
      friends = const [],
      errorMessage = null,
      openChatErrorMessage = null,
      actionErrorMessage = null;

  final ContactStatus status;
  final List<Friend> friends;
  final String? errorMessage;
  final String? openChatErrorMessage;
  final String? actionErrorMessage;

  ContactsState copyWith({
    ContactStatus? status,
    List<Friend>? friends,
    String? errorMessage,
    String? openChatErrorMessage,
    String? actionErrorMessage,
  }) {
    return ContactsState(
      status: status ?? this.status,
      friends: friends ?? this.friends,
      errorMessage: errorMessage,
      openChatErrorMessage: openChatErrorMessage,
      actionErrorMessage: actionErrorMessage,
    );
  }
}

final contactsControllerProvider =
    NotifierProvider<ContactsController, ContactsState>(ContactsController.new);

class ContactsController extends Notifier<ContactsState> {
  @override
  ContactsState build() {
    return const ContactsState.initial();
  }

  Future<void> loadFriends({String? query}) async {
    state = state.copyWith(
      status: ContactStatus.loading,
      errorMessage: null,
      openChatErrorMessage: null,
      actionErrorMessage: null,
    );

    try {
      final result = await ref
          .read(contactRepositoryProvider)
          .getFriends(query: query);
      state = ContactsState(
        status: ContactStatus.success,
        friends: result.friends.content,
      );
      ref
          .read(stompServiceProvider)
          .watchPresenceUsers(
            result.friends.content.map((friend) => friend.userId),
          );
      await _loadPresenceSnapshots(result.friends.content);
    } catch (e) {
      state = state.copyWith(
        status: ContactStatus.failure,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> removeFriend(String friendId) async {
    final previousState = state;

    try {
      await ref.read(contactRepositoryProvider).removeFriend(friendId);
      removeFriendLocally(friendId);
      state = state.copyWith(actionErrorMessage: null);
    } catch (e) {
      state = previousState.copyWith(actionErrorMessage: e.toString());
    }
  }

  Future<void> blockUser(String userId) async {
    final previousState = state;

    try {
      await ref.read(contactRepositoryProvider).blockUser(userId);
      removeFriendLocally(userId);
      state = state.copyWith(actionErrorMessage: null);
    } catch (e) {
      state = previousState.copyWith(actionErrorMessage: e.toString());
    }
  }

  void removeFriendLocally(String userId) {
    state = state.copyWith(
      status: ContactStatus.success,
      friends: state.friends
          .where((friend) => friend.userId != userId)
          .toList(),
      actionErrorMessage: null,
    );
  }

  void applyPresence(PresenceEvent event) {
    var changed = false;
    final nextFriends = state.friends.map((friend) {
      if (friend.userId != event.userId) {
        return friend;
      }

      changed = true;
      return friend.copyWith(isOnline: event.isOnline);
    }).toList();

    if (!changed) {
      return;
    }

    state = state.copyWith(friends: nextFriends);
  }

  Future<void> _loadPresenceSnapshots(List<Friend> friends) async {
    final userIds = friends
        .map((friend) => friend.userId)
        .where((userId) => userId.isNotEmpty)
        .toSet();

    if (userIds.isEmpty) {
      return;
    }

    final presenceRepository = ref.read(presenceRepositoryProvider);
    final snapshots = await Future.wait(
      userIds.map((userId) async {
        try {
          return await presenceRepository.getPresence(userId);
        } catch (error, stackTrace) {
          appLogger.w(
            'Cannot load contact presence snapshot for user $userId.',
            error: error,
            stackTrace: stackTrace,
          );
          return null;
        }
      }),
    );

    for (final snapshot in snapshots.whereType<PresenceEvent>()) {
      applyPresence(snapshot);
    }
  }

  Future<Conversation?> openChat(String userId) async {
    try {
      var conversation = await ref
          .read(contactRepositoryProvider)
          .openOrCreateDirectConversation(userId);

      final friend = state.friends.firstWhere(
        (f) => f.userId == userId,
        orElse: () => const Friend(userId: ''),
      );
      if (friend.userId.isNotEmpty) {
        conversation = conversation.copyWith(
          conversationName: friend.displayName,
          avatarUrl: friend.profilePictureUrl,
        );
      }

      state = state.copyWith(openChatErrorMessage: null);
      return conversation;
    } catch (e) {
      state = state.copyWith(openChatErrorMessage: e.toString());
      return null;
    }
  }
}
