import 'package:kiromobile/features/chat/data/models/page_response.dart';
import 'package:kiromobile/features/contact/data/models/friendship_status.dart';

class ContactProfile {
  const ContactProfile({
    required this.userId,
    required this.friendshipStatus,
    this.username,
    this.fullname,
    this.firstname,
    this.lastname,
    this.emailAddress,
    this.profilePictureUrl,
  });

  final String userId;
  final String? username;
  final String? fullname;
  final String? firstname;
  final String? lastname;
  final String? emailAddress;
  final String? profilePictureUrl;
  final FriendshipStatus friendshipStatus;

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

  factory ContactProfile.fromJson(Map<String, dynamic> json) {
    return ContactProfile(
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
    );
  }

  ContactProfile copyWith({FriendshipStatus? friendshipStatus}) {
    return ContactProfile(
      userId: userId,
      username: username,
      fullname: fullname,
      firstname: firstname,
      lastname: lastname,
      emailAddress: emailAddress,
      profilePictureUrl: profilePictureUrl,
      friendshipStatus: friendshipStatus ?? this.friendshipStatus,
    );
  }
}

class RestContactProfileList {
  const RestContactProfileList({required this.users});

  final PageResponse<ContactProfile> users;

  factory RestContactProfileList.fromJson(Map<String, dynamic> json) {
    return RestContactProfileList(
      users: PageResponse.fromJson(
        _pageJson(json, const ['users', 'contacts']),
        ContactProfile.fromJson,
      ),
    );
  }
}

Map<String, dynamic> _pageJson(Map<String, dynamic> json, List<String> keys) {
  for (final key in keys) {
    final nested = json[key];
    if (nested is Map<String, dynamic>) {
      return nested;
    }
  }

  return json;
}
