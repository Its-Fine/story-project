import { apiClient, AUTH_BASE_URL } from './client';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  first_name: string;
  last_name: string;
  email: string;
  birth_date: string;
  password: string;
  password_again: string;
  consent: boolean;
  parent_consent: boolean;
}

export interface ResetPasswordRequest {
  current_password: string;
  new_password: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token?: string;
}

export interface MeResponse {
  email: string;
  role: string; // 'ADMIN' или 'PARTICIPANT'
}

const AUTH_OPTIONS = {
  baseUrl: AUTH_BASE_URL,
  withCredentials: true,
};

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient<AuthResponse>({
      api: '/auth/login',
      method: 'POST',
      values: data,
      ...AUTH_OPTIONS,
    }),

  register: (data: RegisterRequest) =>
    apiClient({
      api: '/auth/signup',
      method: 'POST',
      values: data,
      ...AUTH_OPTIONS,
    }),

  logout: (token: string) =>
    apiClient({
      api: '/auth/logout',
      method: 'POST',
      token,
      ...AUTH_OPTIONS,
    }),

  resetPassword: (data: ResetPasswordRequest, token: string) =>
    apiClient({
      api: '/auth/reset',
      method: 'PUT',
      values: data,
      token,
      ...AUTH_OPTIONS,
    }),

  refresh: () =>
    apiClient<AuthResponse>({
      api: '/auth/refresh',
      method: 'POST',
      ...AUTH_OPTIONS,
    }),

  me: (token: string) =>
    apiClient<MeResponse>({
      api: '/auth/me',
      method: 'GET',
      token,
      ...AUTH_OPTIONS,
    }),
};
