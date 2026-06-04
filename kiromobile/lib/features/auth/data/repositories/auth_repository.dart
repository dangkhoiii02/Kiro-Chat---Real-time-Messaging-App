import 'package:kiromobile/core/config/app_config.dart';
import 'package:kiromobile/features/auth/data/datasources/token_storage.dart';
import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:kiromobile/features/auth/data/models/auth_tokens.dart';

class AuthRepository {
  final TokenStorage _tokenStorage;
  final FlutterAppAuth appAuth;
  AuthRepository(this._tokenStorage, this.appAuth);

  AuthorizationServiceConfiguration get _keycloakServiceConfiguration {
    return AuthorizationServiceConfiguration(
      authorizationEndpoint:
          '${AppConfig.keycloakIssuer}/protocol/openid-connect/auth',
      tokenEndpoint:
          '${AppConfig.keycloakIssuer}/protocol/openid-connect/token',
      endSessionEndpoint:
          '${AppConfig.keycloakIssuer}/protocol/openid-connect/logout',
    );
  }

  Future<AuthTokens> loginWithKeycloak() async {
    final result = await appAuth.authorizeAndExchangeCode(
      AuthorizationTokenRequest(
        AppConfig.keycloakClientId,
        AppConfig.keycloakRedirectUrl,
        serviceConfiguration: _keycloakServiceConfiguration,
        scopes: AppConfig.keycloakScopes,
        allowInsecureConnections: AppConfig.keycloakAllowInsecureConnections,
      ),
    );

    final accessToken = result.accessToken;

    if (accessToken == null || accessToken.isEmpty) {
      throw Exception('Không thể lấy được Access Token');
    }

    final expirationDateTime = result.accessTokenExpirationDateTime;
    final expiresIn = expirationDateTime?.difference(DateTime.now()).inSeconds;

    final tokens = AuthTokens.fromTokenResponse(
      accessToken: accessToken,
      refreshToken: result.refreshToken,
      idToken: result.idToken,
      expiresIn: expiresIn,
    );

    await _tokenStorage.saveTokens(tokens);

    return tokens;
  }

  Future<AuthTokens?> restoreSession() {
    return _tokenStorage.readTokens();
  }

  Future<AuthTokens> refreshTokens(AuthTokens currentTokens) async {
    final refreshToken = currentTokens.refreshToken;

    if (refreshToken == null || refreshToken.isEmpty) {
      throw Exception('Không có Refresh Token để làm mới phiên đăng nhập');
    }

    final result = await appAuth.token(
      TokenRequest(
        AppConfig.keycloakClientId,
        AppConfig.keycloakRedirectUrl,
        serviceConfiguration: _keycloakServiceConfiguration,
        scopes: AppConfig.keycloakScopes,
        refreshToken: refreshToken,
        allowInsecureConnections: AppConfig.keycloakAllowInsecureConnections,
      ),
    );

    final accessToken = result.accessToken;

    if (accessToken == null || accessToken.isEmpty) {
      throw Exception('Không thể làm mới Access Token');
    }

    final tokens = AuthTokens(
      accessToken: accessToken,
      refreshToken: result.refreshToken ?? currentTokens.refreshToken,
      idToken: result.idToken ?? currentTokens.idToken,
      accessTokenExpiresAt: result.accessTokenExpirationDateTime,
    );

    await _tokenStorage.saveTokens(tokens);

    return tokens;
  }

  Future<String?> getAccessToken() {
    return _tokenStorage.readAccessToken();
  }

  Future<void> clearLocalSession() {
    return _tokenStorage.clearTokens();
  }

  Future<void> logout() async {
    final tokens = await _tokenStorage.readTokens();

    try {
      final idToken = tokens?.idToken;

      if (idToken != null && idToken.isNotEmpty) {
        await appAuth.endSession(
          EndSessionRequest(
            idTokenHint: idToken,
            postLogoutRedirectUrl: AppConfig.keycloakPostLogoutRedirectUrl,
            serviceConfiguration: _keycloakServiceConfiguration,
            allowInsecureConnections:
                AppConfig.keycloakAllowInsecureConnections,
          ),
        );
      }
    } finally {
      await _tokenStorage.clearTokens();
    }
  }
}
