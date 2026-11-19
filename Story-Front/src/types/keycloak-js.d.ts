// Compatibility declarations for TypeScript 4.9 using moduleResolution=node.
// keycloak-js 26 publishes its own declarations through package.json exports,
// which this older resolver cannot discover.
declare module 'keycloak-js' {
  interface KeycloakConfig {
    url: string;
    realm: string;
    clientId: string;
  }

  interface KeycloakInitOptions {
    onLoad?: 'login-required' | 'check-sso';
    flow?: 'standard' | 'implicit' | 'hybrid';
    pkceMethod?: 'S256' | false;
    checkLoginIframe?: boolean;
  }

  interface KeycloakLoginOptions {
    redirectUri?: string;
    action?: string;
  }

  interface KeycloakLogoutOptions {
    redirectUri?: string;
  }

  export default class Keycloak {
    constructor(config: KeycloakConfig);

    authenticated?: boolean;
    token?: string;
    realmAccess?: { roles: string[] };
    onTokenExpired?: () => void;

    init(options?: KeycloakInitOptions): Promise<boolean>;
    login(options?: KeycloakLoginOptions): Promise<void>;
    logout(options?: KeycloakLogoutOptions): Promise<void>;
    updateToken(minValidity: number): Promise<boolean>;
    clearToken(): void;
  }
}
