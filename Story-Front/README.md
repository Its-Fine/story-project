# Story Frontend

Фронтенд для системы Story на React + TypeScript (Create React App).

## Запуск

```bash
npm install
npm start
```

По умолчанию API использует относительный gateway-путь `/api/v1`. Для hot reload
запустите контейнерный gateway на другом порту и явно укажите его адрес:

```powershell
$env:STORY_FRONTEND_HOST_PORT = '3001'
Set-Location ..\Story-Data
docker compose up -d --build frontend
Set-Location ..\Story-Front
$env:REACT_APP_API_URL = 'http://localhost:3001/api/v1'
$env:REACT_APP_AUTH_URL = 'http://localhost:3001/api/v1'
npm start
```

## Сборка для продакшена

```bash
npm run build
```

## Docker

Фронтенд собирается в Docker через `Dockerfile` в этой директории и раздаётся через nginx.
