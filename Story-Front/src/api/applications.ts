import { apiClient } from './client';

export interface Application {
  appId: number;
  contestName: string;
  contestId: number;
  name: string;
  description: string;
  city?: string;
  organization?: string;
  mentorName?: string;
  worksCount: number;
  isTeam?: boolean;
  isCurator?: boolean;
  status?: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  rejectionReason?: string | null;
  linksToWorks?: string[];
  members?: ApplicationMember[];
}

export interface ApplicationMember {
  memberId?: number;
  firstName: string;
  lastName: string;
  birthDate: string;
}

export interface ApplicationMemberUpdate {
  firstName: string;
  lastName: string;
  birthDate: string;
}

export interface ApplicationsResponse {
  results: Application[];
}

export interface Contest {
  conId: number;
  name: string;
  description?: string;
  startAge?: number;
  endAge?: number;
}

export interface SendApplicationRequest {
  name: string;
  description: string;
  city: string;
  organization?: string;
  mentorName?: string;
  members: unknown[];
  isTeam: boolean;
  isCurator: boolean;
}

export interface ApproveApplicationsRequest {
  application_ids: number[];
}

export interface RejectApplicationRequest {
  application_id: number;
  rejection_reason: string;
}

export interface DeleteApplicationRequest {
  application_id: number;
}

export interface RateApplicationRequest {
  application_id: number;
  rates: any;
}

export interface UpdateApplicationRequest {
  name: string;
  description: string;
  city: string;
  organization?: string;
  mentorName?: string;
  members?: ApplicationMemberUpdate[];
}

export interface UpdateApplicationResponse {
  link_to_file: string;
}

export const applicationsApi = {
  getContests: (token: string) =>
    apiClient<Contest[]>({
      api: '/contests/all',
      method: 'GET',
      token,
    }),

  createApplication: (data: SendApplicationRequest, contestId: string, token: string) =>
    apiClient<{ appId: number }>({
      api: `/applications?conId=${encodeURIComponent(contestId)}`,
      method: 'POST',
      values: data,
      token,
    }),

  getAllPending: (contestId: string, token: string) =>
    apiClient<ApplicationsResponse>({
      api: '/applications/all/pending',
      method: 'GET',
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  approveApplications: (data: ApproveApplicationsRequest, contestId: string, token: string) =>
    apiClient({
      api: '/applications/approve',
      method: 'PUT',
      values: data,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  deleteApplication: (appId: number, token: string) =>
    apiClient({
      api: `/applications/${appId}`,
      method: 'DELETE',
      token,
    }),

  rateApplication: (data: RateApplicationRequest, contestId: string, token: string) =>
    apiClient({
      api: '/applications/rate',
      method: 'POST',
      values: data,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  updateRate: (data: RateApplicationRequest, contestId: string, token: string) =>
    apiClient({
      api: '/applications/rate/update',
      method: 'PATCH',
      values: data,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  rejectApplication: (data: RejectApplicationRequest, contestId: string, token: string) =>
    apiClient({
      api: '/applications/reject',
      method: 'PATCH',
      values: data,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  sendApplication: (data: SendApplicationRequest, contestId: string, token: string) =>
    apiClient({
      api: '/applications/send',
      method: 'POST',
      values: data,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  updateApplication: (appId: number, data: UpdateApplicationRequest, token: string) =>
    apiClient<void>({
      api: `/applications/${appId}`,
      method: 'PUT',
      values: data,
      token,
    }),

  getUserApplications: (token: string) =>
    apiClient<Application[]>({
      api: '/applications',
      method: 'GET',
      token,
    }),

  getAllAccepted: (contestId: string, token: string) =>
    apiClient<ApplicationsResponse>({
      api: '/applications/all/accepted',
      method: 'GET',
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  getAllRated: (contestId: string, token: string) =>
    apiClient<any[]>({
      api: '/applications/all/rate',
      method: 'GET',
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  getStats: (contestId: string, token: string) =>
    apiClient({
      api: '/applications/all/stats',
      method: 'GET',
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),
};