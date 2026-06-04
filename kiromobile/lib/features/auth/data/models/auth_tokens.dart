class AuthTokens {
  const AuthTokens({
    required this.accessToken,
    this.refreshToken,
    this.idToken,
    this.accessTokenExpiresAt,
  });

  final String accessToken;
  final String? refreshToken;
  final String? idToken;
  final DateTime? accessTokenExpiresAt;

  factory AuthTokens.fromTokenResponse({
    required String accessToken,
    String? refreshToken,
    String? idToken,
    int? expiresIn,
    DateTime? issuedAt,
  }) {
    final now = issuedAt ?? DateTime.now();

    return AuthTokens(
      accessToken: accessToken,
      refreshToken: refreshToken,
      idToken: idToken,
      accessTokenExpiresAt: expiresIn == null
          ? null
          : now.add(Duration(seconds: expiresIn)),
    );
  }
}
