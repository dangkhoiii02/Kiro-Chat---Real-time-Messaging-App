import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/features/contact/data/models/contact_request.dart';
import 'package:kiromobile/features/contact/data/repositories/contact_repository.dart';
import 'package:kiromobile/features/contact/presentation/providers/contacts_controller.dart';

enum FriendRequestsStatus { initial, loading, success, failure }

class FriendRequestsState {
  const FriendRequestsState({
    required this.status,
    this.requests = const [],
    this.errorMessage,
    this.actionErrorMessage,
  });

  const FriendRequestsState.initial()
    : status = FriendRequestsStatus.initial,
      requests = const [],
      errorMessage = null,
      actionErrorMessage = null;

  final FriendRequestsStatus status;
  final List<ContactRequest> requests;
  final String? errorMessage;
  final String? actionErrorMessage;

  FriendRequestsState copyWith({
    FriendRequestsStatus? status,
    List<ContactRequest>? requests,
    String? errorMessage,
    String? actionErrorMessage,
  }) {
    return FriendRequestsState(
      status: status ?? this.status,
      requests: requests ?? this.requests,
      errorMessage: errorMessage,
      actionErrorMessage: actionErrorMessage,
    );
  }
}

final friendRequestsControllerProvider =
    NotifierProvider<FriendRequestsController, FriendRequestsState>(
      FriendRequestsController.new,
    );

class FriendRequestsController extends Notifier<FriendRequestsState> {
  @override
  FriendRequestsState build() {
    return const FriendRequestsState.initial();
  }

  Future<void> loadRequests() async {
    state = state.copyWith(
      status: FriendRequestsStatus.loading,
      errorMessage: null,
      actionErrorMessage: null,
    );

    try {
      final result = await ref
          .read(contactRepositoryProvider)
          .getContactRequests();
      state = FriendRequestsState(
        status: FriendRequestsStatus.success,
        requests: result.requests.content,
      );
    } catch (e) {
      state = state.copyWith(
        status: FriendRequestsStatus.failure,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> acceptRequest(String requestUserId) async {
    final previousState = state;

    try {
      await ref
          .read(contactRepositoryProvider)
          .acceptContactRequest(requestUserId);
      _removeRequest(requestUserId);
      await ref.read(contactsControllerProvider.notifier).loadFriends();
    } catch (e) {
      state = previousState.copyWith(actionErrorMessage: e.toString());
    }
  }

  Future<void> rejectRequest(String requestUserId) async {
    final previousState = state;

    try {
      await ref
          .read(contactRepositoryProvider)
          .rejectContactRequest(requestUserId);
      _removeRequest(requestUserId);
    } catch (e) {
      state = previousState.copyWith(actionErrorMessage: e.toString());
    }
  }

  void _removeRequest(String requestUserId) {
    state = state.copyWith(
      status: FriendRequestsStatus.success,
      requests: state.requests
          .where((request) => request.requestUserId != requestUserId)
          .toList(),
      actionErrorMessage: null,
    );
  }
}
