import { apiClient } from './client';
import { UserInfo, UserData } from '../types';

export const usersApi = {
  // получить основную информацию
  getUserInfo: (token: string) =>
    apiClient<UserInfo>({
      api: '/users/info',
      method: 'GET',
      token,
    }),

  getUserData: (token: string) =>
    apiClient<UserData>({
      api: '/users/user_info',
      method: 'GET',
      token,
    }),

  // обновить данные
  updateUserData: (data: Partial<UserData>, token: string) =>
    apiClient({
      api: '/users/user_data',
      method: 'PATCH',
      values: data,
      token,
    }),

  // поиск пользователей
  searchUsers: (email: string, token: string) =>
    apiClient<any[]>({
      api: `/users/search?email=${encodeURIComponent(email)}`,
      method: 'GET',
      token,
    }),
};