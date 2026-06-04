import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/auth/data/models/auth_tokens.dart';

void main() {
  test('fromTokenResponse maps Keycloak tokens and calculates expiry time', () {
    final issuedAt = DateTime.utc(2026, 5, 25, 10);

    final tokens = AuthTokens.fromTokenResponse(
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      idToken: 'id-token',
      expiresIn: 300,
      issuedAt: issuedAt,
    );

    expect(tokens.accessToken, 'access-token');
    expect(tokens.refreshToken, 'refresh-token');
    expect(tokens.idToken, 'id-token');
    expect(
      tokens.accessTokenExpiresAt,
      issuedAt.add(const Duration(seconds: 300)),
    );
  });
}
