import { IEnv } from './env.model';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const w = (window as any).__env || {};

export const environment: IEnv = {
  production: w.production ?? false,

  socketEndpoint: w.socketEndpoint || 'ws://localhost:8080/api/v1/ws',
  apiUrl:         w.apiUrl         || '/api/v1',

  keycloak: {
    issuer:   w.keycloakIssuer    || 'http://localhost:9093',
    realm:    w.keycloakRealm     || 'kiro-realm',
    clientId: w.keycloakClientId  || 'angular',
  },
  livekitUrl: w.livekitUrl || 'ws://localhost:7880',
};
