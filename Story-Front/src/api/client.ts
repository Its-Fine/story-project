export const API_BASE_URL = process.env.REACT_APP_API_URL || '/api/v1';
export const AUTH_BASE_URL = process.env.REACT_APP_AUTH_URL || '/api/v1';

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

const fallbackMessage = (status: number) => {
  switch (status) {
    case 400: return 'Проверьте введённые данные.';
    case 401: return 'Сессия истекла. Войдите снова.';
    case 403: return 'Недостаточно прав для выполнения действия.';
    case 404: return 'Запрошенные данные не найдены.';
    case 409: return 'Данные уже изменились. Обновите страницу и повторите действие.';
    case 410: return 'Приём заявок закрыт.';
    case 413: return 'Файл слишком большой.';
    case 415: return 'Формат файла не поддерживается.';
    default: return status >= 500
      ? 'Сервис временно недоступен. Повторите попытку позже.'
      : 'Не удалось выполнить запрос.';
  }
};

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
          message: parsedData?.message || fallbackMessage(response.status),
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
        message: error instanceof Error && error.message !== 'Failed to fetch'
          ? error.message
          : 'Не удалось связаться с сервером. Проверьте подключение и повторите попытку.',
      },
    };
  }
};
