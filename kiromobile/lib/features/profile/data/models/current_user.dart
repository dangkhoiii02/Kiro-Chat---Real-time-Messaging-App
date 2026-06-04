class CurrentUser {
  const CurrentUser({
    required this.userId,
    this.emailAddress,
    this.firstname,
    this.lastname,
    this.username,
    this.profilePictureUrl,
  });

  final String userId;
  final String? emailAddress;
  final String? firstname;
  final String? lastname;
  final String? username;
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

  factory CurrentUser.fromJson(Map<String, dynamic> json) {
    return CurrentUser(
      userId: json['userId'] as String,
      emailAddress: json['emailAddress'] as String?,
      firstname: json['firstname'] as String?,
      lastname: json['lastname'] as String?,
      username: json['username'] as String?,
      profilePictureUrl: json['profilePictureUrl'] as String?,
    );
  }
}
