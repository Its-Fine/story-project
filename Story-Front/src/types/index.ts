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

export interface AuthResponse {
  access_token: string;
}

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

export interface UploadResponse {
  link_to_file: string;
}

export interface UserInfo {
  first_name: string;
  last_name: string;
  avatar_link?: string;
}

export interface UserData extends UserInfo {
  email: string;
  birth_date: string;
}