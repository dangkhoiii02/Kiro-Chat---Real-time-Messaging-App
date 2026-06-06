import 'package:kiromobile/features/chat/data/models/page_response.dart';

class BlockedUser {
  const BlockedUser({
    required this.userId,
    this.username,
    this.fullname,
    this.firstname,
    this.lastname,
    this.emailAddress,
    this.profilePictureUrl,
    this.reason,
  });

  final String userId;
  final String? username;
  final String? fullname;
  final String? firstname;
  final String? lastname;
  final String? emailAddress;
  final String? profilePictureUrl;
  final String? reason;

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

  factory BlockedUser.fromJson(Map<String, dynamic> json) {
    return BlockedUser(
      userId: json['userId'] as String,
      username: json['username'] as String?,
      fullname: json['fullname'] as String?,
      firstname: json['firstname'] as String?,
      lastname: json['lastname'] as String?,
      emailAddress: json['emailAddress'] as String? ?? json['email'] as String?,
      profilePictureUrl:
          json['profilePictureUrl'] as String? ?? json['imageUrl'] as String?,
      reason: json['reason'] as String?,
    );
  }
}

class RestBlockedUserList {
  const RestBlockedUserList({required this.users});

  final PageResponse<BlockedUser> users;

  factory RestBlockedUserList.fromJson(Map<String, dynamic> json) {
    return RestBlockedUserList(
      users: PageResponse.fromJson(
        _pageJson(json, 'users'),
        BlockedUser.fromJson,
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
