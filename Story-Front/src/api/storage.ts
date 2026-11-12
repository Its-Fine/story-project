import { API_BASE_URL } from './client';

export interface WorkUploadResponse {
  workId: number;
}

export const storageApi = {
  // загрузка с прогрессом
  uploadWorkWithProgress: (
    file: File,
    appId: number,
    token: string,
    onProgress?: (percent: number) => void
  ): Promise<WorkUploadResponse> => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const url = `${API_BASE_URL}/works?appId=${encodeURIComponent(appId)}`;

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable && onProgress) {
          const percent = (e.loaded / e.total) * 100;
          onProgress(Math.round(percent));
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status === 200 || xhr.status === 201) {
          try {
            const response = JSON.parse(xhr.responseText);
            resolve(response);
          } catch {
            reject(new Error('Ошибка при обработке ответа'));
          }
        } else {
          let message = `Ошибка загрузки: ${xhr.status}`;
          try {
            const response = JSON.parse(xhr.responseText);
            if (response.message) message = response.message;
          } catch {
            // Keep the HTTP status message when the server does not return JSON.
          }
          reject(new Error(message));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Ошибка сети при загрузке'));
      });

      xhr.open('POST', url);
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);

      const formData = new FormData();
      formData.append('file', file);

      xhr.send(formData);
    });
  },
};