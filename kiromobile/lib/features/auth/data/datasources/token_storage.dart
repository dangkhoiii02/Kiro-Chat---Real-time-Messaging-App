import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kiromobile/features/auth/data/models/auth_tokens.dart';

final tokenStorageProvider = Provider<TokenStorage>((ref) {
  return TokenStorage();
});

class TokenStorage {
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  static const accessTokenKey = 'access_token';
  static const refreshTokenKey = 'refresh_token';
  static const idTokenKey = 'id_token';
  static const accessTokenExpiresAtKey = 'access_token_expires_at';

  Future<void> saveTokens(AuthTokens tokens) async {
    await _storage.write(key: accessTokenKey, value: tokens.accessToken);

    if (tokens.refreshToken != null) {
      await _storage.write(key: refreshTokenKey, value: tokens.refreshToken);
    }

    if (tokens.idToken != null) {
      await _storage.write(key: idTokenKey, value: tokens.idToken);
    }

    if (tokens.accessTokenExpiresAt != null) {
      await _storage.write(
        key: accessTokenExpiresAtKey,
        value: tokens.accessTokenExpiresAt!.toIso8601String(),
      );
    }
  }

  Future<String?> readAccessToken() => _storage.read(key: accessTokenKey);

  Future<String?> readRefreshToken() => _storage.read(key: refreshTokenKey);

  Future<String?> readIdToken() => _storage.read(key: idTokenKey);

  Future<DateTime?> readAccessTokenExpiresAt() async {
    final value = await _storage.read(key: accessTokenExpiresAtKey);
    return value != null ? DateTime.parse(value) : null;
  }

  Future<AuthTokens?> readTokens() async {
    final accessToken = await readAccessToken();

    if (accessToken == null || accessToken.isEmpty) {
      return null;
    }

    return AuthTokens(
      accessToken: accessToken,
      refreshToken: await readRefreshToken(),
      idToken: await readIdToken(),
      accessTokenExpiresAt: await readAccessTokenExpiresAt(),
    );
  }

  Future<void> clearTokens() async {
    await _storage.delete(key: accessTokenKey);
    await _storage.delete(key: refreshTokenKey);
    await _storage.delete(key: idTokenKey);
    await _storage.delete(key: accessTokenExpiresAtKey);
  }
}
