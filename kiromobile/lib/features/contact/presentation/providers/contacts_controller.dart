import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/contact/data/models/friend.dart';
import 'package:kiromobile/features/contact/data/repositories/contact_repository.dart';

enum ContactStatus { initial, loading, success, failure }

class ContactsState {
  const ContactsState({
    required this.status,
    this.friends = const [],
    this.errorMessage,
    this.openChatErrorMessage,
  });

  const ContactsState.initial()
    : status = ContactStatus.initial,
      friends = const [],
      errorMessage = null,
      openChatErrorMessage = null;

  final ContactStatus status;
  final List<Friend> friends;
  final String? errorMessage;
  final String? openChatErrorMessage;

  ContactsState copyWith({
    ContactStatus? status,
    List<Friend>? friends,
    String? errorMessage,
    String? openChatErrorMessage,
  }) {
    return ContactsState(
      status: status ?? this.status,
      friends: friends ?? this.friends,
      errorMessage: errorMessage,
      openChatErrorMessage: openChatErrorMessage,
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
    );

    try {
      final result = await ref
          .read(contactRepositoryProvider)
          .getFriends(query: query);
      state = ContactsState(
        status: ContactStatus.success,
        friends: result.friends.content,
      );
    } catch (e) {
      state = state.copyWith(
        status: ContactStatus.failure,
        errorMessage: e.toString(),
      );
    }
  }

  Future<Conversation?> openChat(String userId) async {
    try {
      final conversation = await ref
          .read(contactRepositoryProvider)
          .openOrCreateDirectConversation(userId);
      state = state.copyWith(openChatErrorMessage: null);
      return conversation;
    } catch (e) {
      state = state.copyWith(openChatErrorMessage: e.toString());
      return null;
    }
  }
}
