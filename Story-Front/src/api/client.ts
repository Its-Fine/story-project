const API_BASE_URL = 'http://localhost:8000/api/v1';
export const AUTH_BASE_URL = 'http://localhost:8001/api/v1';

interface ApiRequestConfig {
  api: string;
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  values?: any;
  token?: string | null;
  customHeaders?: Record<string, string>;
  withCredentials?: boolean;
  baseUrl?: string;
}

interface ApiResponse<T = any> {
  status: number;
  data?: T;
  error?: {
    status: number;
    message: string;
    details?: any;
  };
}

export const apiClient = async <T = any>({
  api,
  method = 'POST',
  values,
  token,
  customHeaders = {},
  withCredentials = false,
  baseUrl = API_BASE_URL,
}: ApiRequestConfig): Promise<ApiResponse<T>> => {
  try {
    const url = `${baseUrl}${api}`;

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...customHeaders,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const config: RequestInit = {
      method,
      headers,
      credentials: withCredentials ? 'include' : 'same-origin',
    };

    if (values) {
      if (values instanceof FormData) {
        delete headers['Content-Type'];
        config.body = values;
      } else {
        config.body = JSON.stringify(values);
      }
    }

    const response = await fetch(url, config);
    const data = await response.text();

    let parsedData;
    try {
      parsedData = data ? JSON.parse(data) : {};
    } catch {
      parsedData = data;
    }

    if (!response.ok) {
      return {
        status: response.status,
        error: {
          status: response.status,
          message: parsedData?.message || 'Request failed',
          details: parsedData,
        },
      };
    }

    return {
      status: response.status,
      data: parsedData,
    };
  } catch (error) {
    return {
      status: 500,
      error: {
        status: 500,
        message: error instanceof Error ? error.message : 'Network error',
      },
    };
  }
};