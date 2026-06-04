import 'package:kiromobile/features/chat/data/models/page_response.dart';
import 'package:kiromobile/features/contact/data/models/friendship_status.dart';

class Friend {
  const Friend({
    required this.userId,
    this.username,
    this.fullname,
    this.firstname,
    this.lastname,
    this.emailAddress,
    this.profilePictureUrl,
    this.friendshipStatus = FriendshipStatus.unknown,
    this.isOnline = false,
  });

  final String userId;
  final String? username;
  final String? fullname;
  final String? firstname;
  final String? lastname;
  final String? emailAddress;
  final String? profilePictureUrl;
  final FriendshipStatus friendshipStatus;
  final bool isOnline;

  String get displayName {
    final backendFullname = fullname?.trim();
    if (backendFullname != null && backendFullname.isNotEmpty) {
      return backendFullname;
    }

    final fullName = [firstname, lastname]
        .where((part) => part != null && part.trim().isNotEmpty)
        .map((part) => part!.trim())
        .join(' ');

    if (fullName.isNotEmpty) {
      return fullName;
    }

    return username ?? emailAddress ?? 'Kiro user';
  }

  factory Friend.fromJson(Map<String, dynamic> json) {
    return Friend(
      userId: json['userId'] as String,
      username: json['username'] as String?,
      fullname: json['fullname'] as String?,
      firstname: json['firstname'] as String?,
      lastname: json['lastname'] as String?,
      emailAddress: json['emailAddress'] as String? ?? json['email'] as String?,
      profilePictureUrl:
          json['profilePictureUrl'] as String? ?? json['imageUrl'] as String?,
      friendshipStatus: FriendshipStatus.fromJson(
        json['friendshipStatus'] as String?,
      ),
      isOnline: (json['isOnline'] as bool?) ?? false,
    );
  }
}

class RestFriendList {
  const RestFriendList({required this.friends});

  final PageResponse<Friend> friends;

  factory RestFriendList.fromJson(Map<String, dynamic> json) {
    return RestFriendList(
      friends: PageResponse.fromJson(
        _pageJson(json, 'friends'),
        Friend.fromJson,
      ),
    );
  }
}

Map<String, dynamic> _pageJson(Map<String, dynamic> json, String key) {
  final nested = json[key];
  if (nested is Map<String, dynamic>) {
    return nested;
  }

  return json;
}
