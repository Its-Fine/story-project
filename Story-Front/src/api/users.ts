import { apiClient } from './client';

export interface AccountData {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  applicationsCount: number;
}

export const usersApi = {
  // получить данные текущего аккаунта
  getAccountData: (token: string) =>
    apiClient<AccountData>({
      api: '/account/data',
      method: 'GET',
      token,
    }),

  // обновить данные аккаунта
  updateAccountData: (data: Partial<AccountData>, token: string) =>
    apiClient({
      api: '/account/data',
      method: 'PUT',
      values: data,
      token,
    }),

  // сменить пароль
  updatePassword: (data: { current_password: string; new_password: string }, token: string) =>
    apiClient({
      api: '/account/password',
      method: 'PUT',
      values: data,
      token,
    }),
};