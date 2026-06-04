import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kiromobile/features/auth/data/datasources/token_storage.dart';
import 'package:kiromobile/features/auth/data/models/auth_tokens.dart';
import 'package:kiromobile/features/auth/data/repositories/auth_repository.dart';
import 'package:kiromobile/features/auth/presentation/providers/auth_provider.dart';

void main() {
  test(
    'checkSession refreshes expired access token when refresh succeeds',
    () async {
      final fakeRepository = _FakeAuthRepository(
        restoredTokens: AuthTokens(
          accessToken: 'expired-token',
          refreshToken: 'refresh-token',
          accessTokenExpiresAt: DateTime.now().subtract(
            const Duration(minutes: 1),
          ),
        ),
        refreshedTokens: AuthTokens(
          accessToken: 'fresh-token',
          refreshToken: 'fresh-refresh-token',
          accessTokenExpiresAt: DateTime.now().add(const Duration(minutes: 5)),
        ),
      );
      final container = ProviderContainer(
        overrides: [authRepositoryProvider.overrideWithValue(fakeRepository)],
      );
      addTearDown(container.dispose);

      await container.read(authControllerProvider.notifier).checkSession();

      final authState = container.read(authControllerProvider);
      expect(authState.status, AuthStatus.authenticated);
      expect(authState.tokens?.accessToken, 'fresh-token');
      expect(fakeRepository.refreshTokensCallCount, 1);
      expect(fakeRepository.clearLocalSessionCallCount, 0);
      expect(fakeRepository.logoutCallCount, 0);
    },
  );

  test('checkSession clears local session when refresh fails', () async {
    final fakeRepository = _FakeAuthRepository(
      restoredTokens: AuthTokens(
        accessToken: 'expired-token',
        refreshToken: 'refresh-token',
        accessTokenExpiresAt: DateTime.now().subtract(
          const Duration(minutes: 1),
        ),
      ),
      refreshError: Exception('refresh failed'),
    );
    final container = ProviderContainer(
      overrides: [authRepositoryProvider.overrideWithValue(fakeRepository)],
    );
    addTearDown(container.dispose);

    await container.read(authControllerProvider.notifier).checkSession();

    final authState = container.read(authControllerProvider);
    expect(authState.status, AuthStatus.unauthenticated);
    expect(fakeRepository.refreshTokensCallCount, 1);
    expect(fakeRepository.clearLocalSessionCallCount, 1);
    expect(fakeRepository.logoutCallCount, 0);
  });
}

class _FakeAuthRepository extends AuthRepository {
  _FakeAuthRepository({
    this.restoredTokens,
    this.refreshedTokens,
    this.refreshError,
  }) : super(TokenStorage(), const FlutterAppAuth());

  final AuthTokens? restoredTokens;
  final AuthTokens? refreshedTokens;
  final Object? refreshError;
  int logoutCallCount = 0;
  int refreshTokensCallCount = 0;
  int clearLocalSessionCallCount = 0;

  @override
  Future<String?> getAccessToken() async => restoredTokens?.accessToken;

  @override
  Future<AuthTokens> loginWithKeycloak() async {
    return restoredTokens ??
        const AuthTokens(
          accessToken: 'new-token',
          refreshToken: 'refresh-token',
        );
  }

  @override
  Future<void> logout() async {
    logoutCallCount += 1;
  }

  @override
  Future<AuthTokens> refreshTokens(AuthTokens currentTokens) async {
    refreshTokensCallCount += 1;

    final error = refreshError;
    if (error != null) {
      throw error;
    }

    return refreshedTokens ?? const AuthTokens(accessToken: 'refreshed-token');
  }

  @override
  Future<void> clearLocalSession() async {
    clearLocalSessionCallCount += 1;
  }

  @override
  Future<AuthTokens?> restoreSession() async => restoredTokens;
}
