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
  works?: ApplicationWork[];
  members?: ApplicationMember[];
}

export interface ApplicationWork {
  workId: number;
  name: string;
  fileSize: number;
  broken: boolean;
  rate?: number | null;
  isRated: boolean;
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
  members: ApplicationMemberUpdate[];
  isTeam: boolean;
  isCurator: boolean;
}

export interface UpdateApplicationRequest {
  name: string;
  description: string;
  city: string;
  organization?: string;
  mentorName?: string;
  members?: ApplicationMemberUpdate[];
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

  deleteApplication: (appId: number, token: string) =>
    apiClient({
      api: `/applications/${appId}`,
      method: 'DELETE',
      token,
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

};
