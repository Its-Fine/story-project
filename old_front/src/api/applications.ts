import { apiClient } from './client';

export interface Application {
  id: number;
  contest_title: string;
  nomination_name: string;
  annotation: string;
  name: string;
  link_to_work: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  rejection_reason?: string;
  contest_id: number;
}

export interface ApplicationsResponse {
  results: Application[];
}

export interface SendApplicationRequest {
  name: string;
  annotation: string;
  nomination_id: number;
  link_to_work: string;
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
  name?: string;
  annotation?: string;
  link_to_work?: string;
}

export interface UpdateApplicationResponse {
  link_to_file: string;
}

export const applicationsApi = {
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

  deleteApplication: (data: DeleteApplicationRequest, contestId: string, token: string) =>
    apiClient({
      api: '/applications/delete',
      method: 'DELETE',
      values: data,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
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

  updateApplication: (data: UpdateApplicationRequest, contestId: string, token: string) =>
    apiClient<UpdateApplicationResponse>({
      api: '/applications/update',
      method: 'PATCH',
      values: data,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    }),

  getUserApplications: (token: string, status?: string, page: number = 1) => {
    let url = '/applications/all/user';
    const params = new URLSearchParams();
    params.append('page', page.toString());
    if (status) params.append('status', status);
    if (params.toString()) url += `?${params.toString()}`;

    return apiClient<ApplicationsResponse>({
      api: url,
      method: 'GET',
      token,
    });
  },

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