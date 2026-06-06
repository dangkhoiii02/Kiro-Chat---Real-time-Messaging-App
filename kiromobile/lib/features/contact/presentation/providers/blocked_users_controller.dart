import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/features/contact/data/models/blocked_user.dart';
import 'package:kiromobile/features/contact/data/repositories/contact_repository.dart';

enum BlockedUsersStatus { initial, loading, success, failure }

class BlockedUsersState {
  const BlockedUsersState({
    required this.status,
    this.users = const [],
    this.errorMessage,
    this.actionErrorMessage,
  });

  const BlockedUsersState.initial()
    : status = BlockedUsersStatus.initial,
      users = const [],
      errorMessage = null,
      actionErrorMessage = null;

  final BlockedUsersStatus status;
  final List<BlockedUser> users;
  final String? errorMessage;
  final String? actionErrorMessage;

  BlockedUsersState copyWith({
    BlockedUsersStatus? status,
    List<BlockedUser>? users,
    String? errorMessage,
    String? actionErrorMessage,
  }) {
    return BlockedUsersState(
      status: status ?? this.status,
      users: users ?? this.users,
      errorMessage: errorMessage,
      actionErrorMessage: actionErrorMessage,
    );
  }
}

final blockedUsersControllerProvider =
    NotifierProvider<BlockedUsersController, BlockedUsersState>(
      BlockedUsersController.new,
    );

class BlockedUsersController extends Notifier<BlockedUsersState> {
  @override
  BlockedUsersState build() {
    return const BlockedUsersState.initial();
  }

  Future<void> loadBlockedUsers({int page = 0, int size = 50}) async {
    state = state.copyWith(
      status: BlockedUsersStatus.loading,
      errorMessage: null,
      actionErrorMessage: null,
    );

    try {
      final result = await ref
          .read(contactRepositoryProvider)
          .getBlockedUsers(page: page, size: size);
      state = BlockedUsersState(
        status: BlockedUsersStatus.success,
        users: result.users.content,
      );
    } catch (e) {
      state = state.copyWith(
        status: BlockedUsersStatus.failure,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> unblockUser(String userId) async {
    final previousState = state;

    try {
      await ref.read(contactRepositoryProvider).unblockUser(userId);
      removeUserLocally(userId);
      state = state.copyWith(actionErrorMessage: null);
    } catch (e) {
      state = previousState.copyWith(actionErrorMessage: e.toString());
    }
  }

  void removeUserLocally(String userId) {
    state = state.copyWith(
      status: BlockedUsersStatus.success,
      users: state.users.where((user) => user.userId != userId).toList(),
      actionErrorMessage: null,
    );
  }
}
