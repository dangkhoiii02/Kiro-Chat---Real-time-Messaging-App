import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/chat/data/models/conversation.dart';
import 'package:kiromobile/features/chat/data/models/page_response.dart';
import 'package:kiromobile/features/contact/data/models/contact_profile.dart';
import 'package:kiromobile/features/contact/data/models/contact_request.dart';
import 'package:kiromobile/features/contact/data/models/friend.dart';
import 'package:kiromobile/features/contact/data/models/friendship_status.dart';
import 'package:kiromobile/features/contact/data/repositories/contact_repository.dart';
import 'package:kiromobile/features/contact/presentation/providers/contacts_controller.dart';
import 'package:kiromobile/features/contact/presentation/providers/friend_requests_controller.dart';
import 'package:kiromobile/features/contact/presentation/providers/user_search_controller.dart';

void main() {
  test('ContactsController loads friends success and failure', () async {
    final repository = _FakeContactRepository()
      ..friendsResult = RestFriendList(
        friends: PageResponse(
          content: [_friend('friend-1')],
          totalElements: 1,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
          last: true,
        ),
      );
    final container = _container(repository);
    addTearDown(container.dispose);

    await container.read(contactsControllerProvider.notifier).loadFriends();

    expect(
      container.read(contactsControllerProvider).status,
      ContactStatus.success,
    );
    expect(
      container.read(contactsControllerProvider).friends.single.userId,
      'friend-1',
    );

    repository.error = Exception('network failed');
    await container.read(contactsControllerProvider.notifier).loadFriends();

    expect(
      container.read(contactsControllerProvider).status,
      ContactStatus.failure,
    );
    expect(
      container.read(contactsControllerProvider).errorMessage,
      contains('network failed'),
    );
  });

  test(
    'UserSearchController sends and cancels request by updating item status',
    () async {
      final repository = _FakeContactRepository()
        ..searchResult = RestContactProfileList(
          users: PageResponse(
            content: [_profile('user-1', FriendshipStatus.notConnected)],
            totalElements: 1,
            totalPages: 1,
            pageNumber: 0,
            pageSize: 20,
            last: true,
          ),
        );
      final container = _container(repository);
      addTearDown(container.dispose);

      final controller = container.read(userSearchControllerProvider.notifier);
      await controller.searchUsers('alice');
      await controller.sendRequest('user-1');

      expect(repository.sentUserIds, ['user-1']);
      expect(
        container
            .read(userSearchControllerProvider)
            .users
            .single
            .friendshipStatus,
        FriendshipStatus.friendRequestSent,
      );

      await controller.cancelRequest('user-1');

      expect(repository.cancelledUserIds, ['user-1']);
      expect(
        container
            .read(userSearchControllerProvider)
            .users
            .single
            .friendshipStatus,
        FriendshipStatus.notConnected,
      );
    },
  );

  test(
    'FriendRequestsController accept and reject remove requests and refresh friends',
    () async {
      final repository = _FakeContactRepository()
        ..requestsResult = RestContactRequestList(
          requests: PageResponse(
            content: [_request('request-user-1'), _request('request-user-2')],
            totalElements: 2,
            totalPages: 1,
            pageNumber: 0,
            pageSize: 20,
            last: true,
          ),
        );
      final container = _container(repository);
      addTearDown(container.dispose);

      final controller = container.read(
        friendRequestsControllerProvider.notifier,
      );
      await controller.loadRequests();
      await controller.acceptRequest('request-user-1');
      await controller.rejectRequest('request-user-2');

      expect(repository.acceptedUserIds, ['request-user-1']);
      expect(repository.rejectedUserIds, ['request-user-2']);
      expect(
        container.read(friendRequestsControllerProvider).requests,
        isEmpty,
      );
      expect(repository.getFriendsCallCount, 1);
    },
  );

  test('open chat returns a conversation reference for navigation', () async {
    final repository = _FakeContactRepository();
    final container = _container(repository);
    addTearDown(container.dispose);

    final conversation = await container
        .read(userSearchControllerProvider.notifier)
        .openChat('user-1');

    expect(repository.openedUserIds, ['user-1']);
    expect(conversation?.conversationId, 'conversation-1');
  });

  test(
    'UserSearchController keeps state and skips refresh when accept fails',
    () async {
      final repository = _FakeContactRepository()
        ..acceptError = Exception('accept failed')
        ..searchResult = RestContactProfileList(
          users: PageResponse(
            content: [
              _profile('user-1', FriendshipStatus.friendRequestReceived),
            ],
            totalElements: 1,
            totalPages: 1,
            pageNumber: 0,
            pageSize: 20,
            last: true,
          ),
        );
      final container = _container(repository);
      addTearDown(container.dispose);

      final controller = container.read(userSearchControllerProvider.notifier);
      await controller.searchUsers('alice');
      await controller.acceptRequest('user-1');

      expect(
        container
            .read(userSearchControllerProvider)
            .users
            .single
            .friendshipStatus,
        FriendshipStatus.friendRequestReceived,
      );
      expect(
        container.read(userSearchControllerProvider).actionErrorMessage,
        contains('accept failed'),
      );
      expect(repository.getFriendsCallCount, 0);
      expect(repository.getContactRequestsCallCount, 0);
    },
  );
}

ProviderContainer _container(ContactRepository repository) {
  return ProviderContainer(
    overrides: [contactRepositoryProvider.overrideWithValue(repository)],
  );
}

ContactProfile _profile(String userId, FriendshipStatus status) {
  return ContactProfile(
    userId: userId,
    username: 'alice',
    firstname: 'Alice',
    lastname: 'Nguyen',
    profilePictureUrl: null,
    friendshipStatus: status,
  );
}

Friend _friend(String userId) {
  return Friend(
    userId: userId,
    username: 'bob',
    firstname: 'Bob',
    lastname: null,
    profilePictureUrl: null,
    isOnline: false,
  );
}

ContactRequest _request(String requestUserId) {
  return ContactRequest(
    requestUserId: requestUserId,
    username: requestUserId,
    firstname: null,
    lastname: null,
    profilePictureUrl: null,
  );
}

class _FakeContactRepository extends ContactRepository {
  _FakeContactRepository() : super(Dio());

  Object? error;
  RestFriendList friendsResult = const RestFriendList(
    friends: PageResponse(
      content: [],
      totalElements: 0,
      totalPages: 0,
      pageNumber: 0,
      pageSize: 20,
      last: true,
    ),
  );
  RestContactProfileList searchResult = const RestContactProfileList(
    users: PageResponse(
      content: [],
      totalElements: 0,
      totalPages: 0,
      pageNumber: 0,
      pageSize: 20,
      last: true,
    ),
  );
  RestContactRequestList requestsResult = const RestContactRequestList(
    requests: PageResponse(
      content: [],
      totalElements: 0,
      totalPages: 0,
      pageNumber: 0,
      pageSize: 20,
      last: true,
    ),
  );

  final sentUserIds = <String>[];
  final cancelledUserIds = <String>[];
  final acceptedUserIds = <String>[];
  final rejectedUserIds = <String>[];
  final openedUserIds = <String>[];
  int getFriendsCallCount = 0;
  int getContactRequestsCallCount = 0;
  Object? acceptError;

  @override
  Future<RestFriendList> getFriends({String? query}) async {
    getFriendsCallCount += 1;
    final failure = error;
    if (failure != null) {
      throw failure;
    }
    return friendsResult;
  }

  @override
  Future<RestContactProfileList> searchUsers({required String query}) async {
    return searchResult;
  }

  @override
  Future<RestContactRequestList> getContactRequests() async {
    getContactRequestsCallCount += 1;
    return requestsResult;
  }

  @override
  Future<void> sendContactRequest(String userId) async {
    sentUserIds.add(userId);
  }

  @override
  Future<void> cancelContactRequest(String userId) async {
    cancelledUserIds.add(userId);
  }

  @override
  Future<void> acceptContactRequest(String requestUserId) async {
    final failure = acceptError;
    if (failure != null) {
      throw failure;
    }
    acceptedUserIds.add(requestUserId);
  }

  @override
  Future<void> rejectContactRequest(String requestUserId) async {
    rejectedUserIds.add(requestUserId);
  }

  @override
  Future<Conversation> openOrCreateDirectConversation(String userId) async {
    openedUserIds.add(userId);
    return const Conversation(
      conversationId: 'conversation-1',
      conversationName: 'Alice',
      isGroup: false,
      unreadCount: 0,
      isOnline: false,
      remoteUserId: 'user-1',
    );
  }
}
