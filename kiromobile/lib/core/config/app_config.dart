class AppConfig {
  // Android Emulator uses 10.0.2.2 to reach services running on the host machine.
  static const androidEmulatorHost = 'localhost';

  static const apiBaseUrl = 'http://$androidEmulatorHost:8080/api/v1';
  static const socketUrl = 'ws://$androidEmulatorHost:8080/api/v1/ws';
  static const minioPublicUrl = 'http://$androidEmulatorHost:9000';
  static const liveKitUrl = 'ws://$androidEmulatorHost:7880';

  static const keycloakBaseUrl = 'http://$androidEmulatorHost:9093';
  static const keycloakRealm = 'kiro-realm';
  static const keycloakClientId = 'kiro-mobile';
  static const keycloakIssuer = '$keycloakBaseUrl/realms/$keycloakRealm';
  static const keycloakRedirectUrl = 'com.example.kiromobile:/oauthredirect';
  static const keycloakPostLogoutRedirectUrl =
      'com.example.kiromobile:/oauthredirect';
  static const keycloakAllowInsecureConnections = true;

  static const keycloakScopes = <String>[
    'openid',
    'profile',
    'email',
    'offline_access',
  ];
}
