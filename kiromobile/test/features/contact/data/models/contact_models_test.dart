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
