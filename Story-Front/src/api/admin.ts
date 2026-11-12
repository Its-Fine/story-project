import { API_BASE_URL, apiClient } from './client';

export interface AdminUser {
  userId: number;
  email: string;
  role: string;
  closedAccess: boolean;
  banned: boolean;
  firstName: string;
  lastName: string;
  birthDate: string;
  createdAt: string;
  applicationsCount: number;
}

export interface AdminApplication {
  appId: number;
  name: string;
  description: string;
  city: string;
  organization?: string;
  mentorName?: string;
  contestName: string;
  worksCount: number;
  isTeam: boolean;
  isCurator: boolean;
  createdAt: string;
}

export interface AdminWork {
  workId: number;
  name: string;
  fileSize: number;
  broken: boolean;
  createdAt: string;
}

export const adminApi = {
  getUsersCount: (token: string) =>
    apiClient<number>({ api: '/admin/users/count', method: 'GET', token }),

  getUsers: (token: string, email?: string) => {
    const query = new URLSearchParams({ count: '100', page: '0' });
    if (email) query.set('email', email);
    return apiClient<AdminUser[]>({
      api: `/admin/users?${query.toString()}`,
      method: 'GET',
      token,
    });
  },

  getApplicationsCount: (token: string) =>
    apiClient<number>({ api: '/admin/applications/count', method: 'GET', token }),

  getApplications: (token: string, page = 0) =>
    apiClient<AdminApplication[]>({
      api: `/admin/applications?count=100&page=${page}`,
      method: 'GET',
      token,
    }),

  getWorks: (appId: number, token: string) =>
    apiClient<AdminWork[]>({
      api: `/admin/works?appId=${appId}`,
      method: 'GET',
      token,
    }),

  getEventStatus: (token: string) =>
    apiClient<{ close: boolean }>({ api: '/event/status', method: 'GET', token }),

  setEventStatus: (close: boolean, token: string) =>
    apiClient<{ close: boolean }>({
      api: '/admin/event/status',
      method: 'PUT',
      values: { close },
      token,
    }),

  downloadWork: async (workId: number, token: string): Promise<Blob> => {
    const response = await fetch(`${API_BASE_URL}/admin/works/${workId}/file`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) {
      throw new Error(`Не удалось скачать файл (${response.status})`);
    }
    return response.blob();
  },

  downloadApplicationsReport: async (token: string): Promise<Blob> => {
    const response = await fetch(`${API_BASE_URL}/reports/applications/excel`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) {
      throw new Error(`Не удалось сформировать отчёт (${response.status})`);
    }
    return response.blob();
  },
};
