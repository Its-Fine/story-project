import { apiClient } from './client';

export interface AccountData {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  applicationsCount: number;
}

export interface UpdateAccountData {
  firstName: string;
  lastName: string;
}

export interface UpdatePasswordData {
  current_password: string;
  new_password: string;
}

export const usersApi = {
  getAccountData: (token: string) => apiClient<AccountData>({
    api: '/account/data',
    method: 'GET',
    token,
  }),

  updateAccountData: (data: UpdateAccountData, token: string) => apiClient<void>({
    api: '/account/data',
    method: 'PUT',
    values: data,
    token,
  }),

  updatePassword: (data: UpdatePasswordData, token: string) => apiClient<void>({
    api: '/account/password',
    method: 'PUT',
    values: data,
    token,
  }),
};
