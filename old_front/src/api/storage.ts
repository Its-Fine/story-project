import { apiClient } from './client';
import { UploadResponse } from '../types';

export const storageApi = {
  // загрузка с прогрессом
  uploadWithProgress: (
    file: File,
    contestId: string,
    token: string,
    onProgress?: (percent: number) => void
  ): Promise<UploadResponse> => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const url = `http://localhost:8000/api/v1/storage/upload_contest_work`;

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
          } catch (error) {
            reject(new Error('Ошибка при обработке ответа'));
          }
        } else {
          reject(new Error(`Ошибка загрузки: ${xhr.status}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Ошибка сети при загрузке'));
      });

      xhr.open('POST', url);
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      xhr.setRequestHeader('X-Contest-Id', contestId);

      const formData = new FormData();
      formData.append('file', file);
      formData.append('upload_type', 'application');
      formData.append('contest_id', contestId);

      xhr.send(formData);
    });
  },

  // простая загрузка
  uploadContestWork: (file: File, contestId: string, token: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_type', 'application');
    formData.append('contest_id', contestId);

    return apiClient<UploadResponse>({
      api: '/storage/upload_contest_work',
      method: 'POST',
      values: formData,
      token,
      customHeaders: {
        'X-Contest-Id': contestId,
      },
    });
  },
};