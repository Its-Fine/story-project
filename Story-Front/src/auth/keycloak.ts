import Keycloak from 'keycloak-js';

const KEYCLOAK_ENABLED = process.env.REACT_APP_KEYCLOAK_ENABLED === 'true';

const keycloak = KEYCLOAK_ENABLED
  ? new Keycloak({
      url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080',
      realm: process.env.REACT_APP_KEYCLOAK_REALM || 'story',
      clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'story-frontend',
    })
  : null;

let initialization: Promise<boolean> | null = null;

export const isKeycloakEnabled = () => KEYCLOAK_ENABLED;

export const initializeKeycloak = async () => {
  if (!keycloak) return false;

  if (!initialization) {
    initialization = keycloak.init({
      onLoad: 'check-sso',
      flow: 'standard',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    }).catch(() => false);

    keycloak.onTokenExpired = () => {
      void keycloak.updateToken(30).catch(() => keycloak.clearToken());
    };
  }

  return initialization;
};

export const isKeycloakAuthenticated = () => Boolean(keycloak?.authenticated);

export const getAccessToken = () => {
  if (keycloak?.authenticated && keycloak.token) return keycloak.token;
  return localStorage.getItem('token');
};

export const getValidAccessToken = async () => {
  if (!keycloak?.authenticated) return localStorage.getItem('token');

  try {
    await keycloak.updateToken(30);
    return keycloak.token ?? null;
  } catch {
    keycloak.clearToken();
    return null;
  }
};

export const getKeycloakRole = () => {
  const roles = keycloak?.realmAccess?.roles ?? [];
  if (roles.includes('ADMIN')) return 'ADMIN';
  if (roles.includes('PARTICIPANT')) return 'PARTICIPANT';
  return null;
};

export const loginWithKeycloak = async () => {
  if (!keycloak) throw new Error('Keycloak не включён');
  await keycloak.login({ redirectUri: window.location.origin });
};

export const updateKeycloakPassword = async () => {
  if (!keycloak) throw new Error('Keycloak не включён');
  await keycloak.login({
    action: 'UPDATE_PASSWORD',
    redirectUri: `${window.location.origin}/profile`,
  });
};

export const logoutFromKeycloak = async () => {
  if (!keycloak) return;
  await keycloak.logout({ redirectUri: window.location.origin });
};
