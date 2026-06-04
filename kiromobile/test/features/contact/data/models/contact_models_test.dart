import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/contact/data/models/contact_profile.dart';
import 'package:kiromobile/features/contact/data/models/contact_request.dart';
import 'package:kiromobile/features/contact/data/models/friend.dart';
import 'package:kiromobile/features/contact/data/models/friendship_status.dart';

void main() {
  test('RestContactProfileList parses paged contact profiles', () {
    final result = RestContactProfileList.fromJson({
      'users': {
        'content': [
          {
            'userId': 'user-1',
            'username': 'alice',
            'firstname': 'Alice',
            'lastname': 'Nguyen',
            'profilePictureUrl': 'https://example.com/alice.png',
            'friendshipStatus': 'not_connected',
          },
        ],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
        'size': 20,
        'last': true,
      },
    });

    expect(result.users.content, hasLength(1));
    expect(result.users.content.first.userId, 'user-1');
    expect(result.users.content.first.displayName, 'Alice Nguyen');
    expect(
      result.users.content.first.friendshipStatus,
      FriendshipStatus.notConnected,
    );
  });

  test('RestContactProfileList parses backend user payload aliases', () {
    final result = RestContactProfileList.fromJson({
      'users': {
        'content': [
          {
            'userId': '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
            'fullname': 'Nguyen Van A',
            'username': 'nva_dev',
            'email': 'nva@example.com',
            'imageUrl': 'http://localhost:9000/kiro-bucket-01/avatar.png',
            'friendshipStatus': 'NOT_CONNECTED',
          },
        ],
        'pageable': {'pageNumber': 0, 'pageSize': 20},
        'totalPages': 1,
        'totalElements': 1,
        'last': true,
        'size': 20,
        'number': 0,
      },
    });

    final user = result.users.content.single;

    expect(user.displayName, 'Nguyen Van A');
    expect(user.emailAddress, 'nva@example.com');
    expect(
      user.profilePictureUrl,
      'http://localhost:9000/kiro-bucket-01/avatar.png',
    );
    expect(user.friendshipStatus, FriendshipStatus.notConnected);
  });

  test('RestContactProfileList parses search response wrapped by contacts', () {
    final result = RestContactProfileList.fromJson({
      'contacts': {
        'content': [
          {
            'userId': 'user-contacts-1',
            'fullname': 'Le Van C',
            'username': 'lvc_dev',
            'email': 'lvc@example.com',
            'imageUrl': 'http://localhost:9000/kiro-bucket-01/c.png',
            'friendshipStatus': 'NOT_CONNECTED',
          },
        ],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
        'size': 20,
        'last': true,
      },
    });

    expect(result.users.content, hasLength(1));
    expect(result.users.content.single.userId, 'user-contacts-1');
    expect(result.users.content.single.displayName, 'Le Van C');
  });

  test('RestFriendList parses paged friends', () {
    final result = RestFriendList.fromJson({
      'friends': {
        'content': [
          {
            'userId': 'friend-1',
            'username': 'bob',
            'firstname': 'Bob',
            'lastname': null,
            'profilePictureUrl': null,
            'isOnline': true,
          },
        ],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
        'size': 20,
        'last': true,
      },
    });

    expect(result.friends.content.single.userId, 'friend-1');
    expect(result.friends.content.single.displayName, 'Bob');
    expect(result.friends.content.single.isOnline, isTrue);
  });

  test('RestFriendList parses backend friend payload aliases', () {
    final result = RestFriendList.fromJson({
      'friends': {
        'content': [
          {
            'userId': '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
            'fullname': 'Nguyen Van A',
            'username': 'nva_dev',
            'email': 'nva@example.com',
            'imageUrl': 'http://localhost:9000/kiro-bucket-01/avatar.png',
            'friendshipStatus': 'CONNECTED',
          },
        ],
        'pageable': {'pageNumber': 0, 'pageSize': 20},
        'totalPages': 1,
        'totalElements': 1,
        'last': true,
        'size': 20,
        'number': 0,
      },
    });

    final friend = result.friends.content.single;

    expect(friend.userId, '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d');
    expect(friend.displayName, 'Nguyen Van A');
    expect(friend.username, 'nva_dev');
    expect(friend.emailAddress, 'nva@example.com');
    expect(
      friend.profilePictureUrl,
      'http://localhost:9000/kiro-bucket-01/avatar.png',
    );
    expect(friend.friendshipStatus, FriendshipStatus.friends);
  });

  test('RestContactRequestList parses paged contact requests', () {
    final result = RestContactRequestList.fromJson({
      'requests': {
        'content': [
          {
            'requestUserId': 'request-user-1',
            'username': 'carol',
            'firstname': null,
            'lastname': null,
            'profilePictureUrl': null,
          },
        ],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
        'size': 20,
        'last': true,
      },
    });

    expect(result.requests.content.single.requestUserId, 'request-user-1');
    expect(result.requests.content.single.displayName, 'carol');
  });

  test('RestContactRequestList parses backend request user aliases', () {
    final result = RestContactRequestList.fromJson({
      'requests': {
        'content': [
          {
            'requestUserId': 'request-user-1',
            'requestUser': {
              'userId': 'request-user-1',
              'fullname': 'Tran Thi B',
              'username': 'ttb_dev',
              'email': 'ttb@example.com',
              'imageUrl': 'http://localhost:9000/kiro-bucket-01/b.png',
            },
          },
        ],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
        'size': 20,
        'last': true,
      },
    });

    final request = result.requests.content.single;

    expect(request.requestUserId, 'request-user-1');
    expect(request.displayName, 'Tran Thi B');
    expect(request.emailAddress, 'ttb@example.com');
    expect(
      request.profilePictureUrl,
      'http://localhost:9000/kiro-bucket-01/b.png',
    );
  });

  test('FriendshipStatus maps backend values', () {
    expect(
      FriendshipStatus.fromJson('not_connected'),
      FriendshipStatus.notConnected,
    );
    expect(
      FriendshipStatus.fromJson('not_determined'),
      FriendshipStatus.notDetermined,
    );
    expect(
      FriendshipStatus.fromJson('friend_request_sent'),
      FriendshipStatus.friendRequestSent,
    );
    expect(
      FriendshipStatus.fromJson('friend_request_received'),
      FriendshipStatus.friendRequestReceived,
    );
    expect(FriendshipStatus.fromJson('friends'), FriendshipStatus.friends);
    expect(FriendshipStatus.fromJson('blocked'), FriendshipStatus.blocked);
    expect(FriendshipStatus.fromJson('blocked_by'), FriendshipStatus.blockedBy);
    expect(FriendshipStatus.fromJson('unexpected'), FriendshipStatus.unknown);
  });
}
