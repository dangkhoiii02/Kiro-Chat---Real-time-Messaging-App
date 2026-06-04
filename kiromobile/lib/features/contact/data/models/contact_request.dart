import 'package:kiromobile/features/chat/data/models/page_response.dart';

class ContactRequest {
  const ContactRequest({
    required this.requestUserId,
    this.username,
    this.firstname,
    this.lastname,
    this.emailAddress,
    this.profilePictureUrl,
  });

  final String requestUserId;
  final String? username;
  final String? firstname;
  final String? lastname;
  final String? emailAddress;
  final String? profilePictureUrl;

  String get displayName {
    final fullName = [firstname, lastname]
        .where((part) => part != null && part.trim().isNotEmpty)
        .map((part) => part!.trim())
        .join(' ');

    if (fullName.isNotEmpty) {
      return fullName;
    }

    return username ?? emailAddress ?? 'Kiro user';
  }

  factory ContactRequest.fromJson(Map<String, dynamic> json) {
    final requestUser = json['requestUser'];
    final source = requestUser is Map<String, dynamic>
        ? {...requestUser, ...json}
        : json;

    return ContactRequest(
      requestUserId:
          source['requestUserId'] as String? ?? source['userId'] as String,
      username: source['username'] as String?,
      firstname: source['firstname'] as String?,
      lastname: source['lastname'] as String?,
      emailAddress: source['emailAddress'] as String?,
      profilePictureUrl: source['profilePictureUrl'] as String?,
    );
  }
}

class RestContactRequestList {
  const RestContactRequestList({required this.requests});

  final PageResponse<ContactRequest> requests;

  factory RestContactRequestList.fromJson(Map<String, dynamic> json) {
    return RestContactRequestList(
      requests: PageResponse.fromJson(
        _pageJson(json, 'requests'),
        ContactRequest.fromJson,
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
