import { apiClient } from './client';

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

export interface ConfirmLoginRequest {
  code: string;
}

export interface ResetPasswordRequest {
  current_password: string;
  new_password: string;
}

export interface AuthResponse {
  access_token: string;
}

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient<AuthResponse>({
      api: '/auth/login',
      method: 'POST',
      values: data,
    }),

  register: (data: RegisterRequest) =>
    apiClient({
      api: '/auth/registration',
      method: 'POST',
      values: data,
    }),

  confirmLogin: (data: ConfirmLoginRequest) =>
    apiClient<AuthResponse>({
      api: '/auth/confirm_login',
      method: 'POST',
      values: data,
    }),

  logout: (token: string) =>
    apiClient({
      api: '/auth/logout',
      method: 'POST',
      token,
    }),

  resetPassword: (data: ResetPasswordRequest, token: string) =>
    apiClient({
      api: '/auth/reset',
      method: 'PUT',
      values: data,
      token,
    }),
};