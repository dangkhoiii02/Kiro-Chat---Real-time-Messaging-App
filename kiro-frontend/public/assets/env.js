(function (window) {
  window.__env = window.__env || {};

  window.__env.production = true;
  window.__env.socketEndpoint = 'wss://api.kiro-chat.indevs.in/api/v1/ws';
  window.__env.apiUrl = 'https://api.kiro-chat.indevs.in/api/v1';

  window.__env.keycloak = {
    issuer: 'https://auth.kiro-chat.indevs.in',
    realm: 'kiro-realm',
    clientId: 'angular'
  };
  window.__env.livekitUrl = 'wss://call.kiro-chat.indevs.in';

})(window);
