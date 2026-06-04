import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:kiromobile/features/auth/data/datasources/token_storage.dart';
import 'package:kiromobile/features/auth/data/models/auth_tokens.dart';
import 'package:kiromobile/features/auth/data/repositories/auth_repository.dart';

enum AuthStatus { unknown, loading, authenticated, unauthenticated }

class AuthState {
  const AuthState({required this.status, this.tokens, this.errorMessage});

  const AuthState.unknown()
    : status = AuthStatus.unknown,
      tokens = null,
      errorMessage = null;

  const AuthState.loading()
    : status = AuthStatus.loading,
      tokens = null,
      errorMessage = null;

  const AuthState.authenticated(this.tokens)
    : status = AuthStatus.authenticated,
      errorMessage = null;

  const AuthState.unauthenticated({this.errorMessage})
    : status = AuthStatus.unauthenticated,
      tokens = null;

  final AuthStatus status;
  final AuthTokens? tokens;
  final String? errorMessage;
}

final appAuthProvider = Provider<FlutterAppAuth>((ref) => FlutterAppAuth());

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(
    ref.watch(tokenStorageProvider),
    ref.watch(appAuthProvider),
  ),
);

final authControllerProvider = NotifierProvider<AuthController, AuthState>(() {
  return AuthController();
});

class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() {
    return const AuthState.unknown();
  }

  Future<void> checkSession() async {
    state = const AuthState.loading();

    try {
      final tokens = await ref.read(authRepositoryProvider).restoreSession();

      if (tokens == null) {
        state = const AuthState.unauthenticated();
        return;
      }

      final accessTokenExpiresAt = tokens.accessTokenExpiresAt;
      if (accessTokenExpiresAt != null &&
          !accessTokenExpiresAt.isAfter(DateTime.now())) {
        try {
          final refreshedTokens = await ref
              .read(authRepositoryProvider)
              .refreshTokens(tokens);
          state = AuthState.authenticated(refreshedTokens);
        } catch (e) {
          await ref.read(authRepositoryProvider).clearLocalSession();
          state = AuthState.unauthenticated(errorMessage: e.toString());
        }
        return;
      }

      state = AuthState.authenticated(tokens);
    } catch (e) {
      state = AuthState.unauthenticated(errorMessage: e.toString());
    }
  }

  Future<void> login() async {
    state = const AuthState.loading();

    try {
      final tokens = await ref.read(authRepositoryProvider).loginWithKeycloak();
      state = AuthState.authenticated(tokens);
    } catch (e) {
      state = AuthState.unauthenticated(errorMessage: e.toString());
    }
  }

  Future<void> logout() async {
    state = const AuthState.loading();

    try {
      await ref.read(authRepositoryProvider).logout();
      state = const AuthState.unauthenticated();
    } catch (e) {
      state = AuthState.unauthenticated(errorMessage: e.toString());
    }
  }
}
