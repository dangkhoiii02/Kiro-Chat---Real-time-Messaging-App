enum FriendshipStatus {
  notConnected,
  notDetermined,
  friendRequestSent,
  friendRequestReceived,
  friends,
  blocked,
  blockedBy,
  unknown;

  factory FriendshipStatus.fromJson(String? value) {
    final normalized = value?.trim().toLowerCase();

    return switch (normalized) {
      'not_connected' => FriendshipStatus.notConnected,
      'not_determined' => FriendshipStatus.notDetermined,
      'friend_request_sent' => FriendshipStatus.friendRequestSent,
      'friend_request_received' => FriendshipStatus.friendRequestReceived,
      'connected' => FriendshipStatus.friends,
      'friends' => FriendshipStatus.friends,
      'blocked' => FriendshipStatus.blocked,
      'blocked_by' => FriendshipStatus.blockedBy,
      _ => FriendshipStatus.unknown,
    };
  }

  String get value {
    return switch (this) {
      FriendshipStatus.notConnected => 'not_connected',
      FriendshipStatus.notDetermined => 'not_determined',
      FriendshipStatus.friendRequestSent => 'friend_request_sent',
      FriendshipStatus.friendRequestReceived => 'friend_request_received',
      FriendshipStatus.friends => 'friends',
      FriendshipStatus.blocked => 'blocked',
      FriendshipStatus.blockedBy => 'blocked_by',
      FriendshipStatus.unknown => 'unknown',
    };
  }
}
