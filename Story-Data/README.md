# Story-Data

## Зачем нужно

Хранит `docker-compose.yml` для развертывания всей ИС.

## Файловая структура

### Директории

- `shells` - Скрипты для удобного развертывания ИС.
- `environments` - секреты для запуска сервисов и настройки данных.
- `services` - настройки для микросервисов системы.

*Нужен файл с переменными окружения по примеру из `environments/test.env` для запуска сервиса на **production**.*

### Файлы запуска

- `my-docker.*` - скрипты для быстрого запуска docker-compose, первым аргументом указывается имя файла с переменными окружения.
- `copy.sh` - скрипт для быстрого перемещения файлов из репозитория в специальное место на **production**.
- `dist.sh` - скрипт для быстрой замены фронта на заглушку для проведения технических работ на **production**.

## Примеры использования

    # Запуск и остановка сервиса на windows

    .\shells\my-docker.ps1 test up -d
    .\shells\my-docker.ps1 test down

    # Запуск и остановка сервиса на linux

    bash .\shells\my-docker.sh test up -d
    bash .\shells\my-docker.sh test down
# Story-Data

Docker Compose configuration for deploying the entire Story information system.

## Quick Start

```bash
# Copy environment file
cp .env.example .env

# Edit .env - change STORY_JWT_SECRET to a random Base64 string (≥32 bytes)

# Start all services
docker compose up -d --build
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| **frontend** | 3000 | React SPA (nginx) |
| **story-auth** | 8001 | Authentication service (JWT, refresh-cookie) |
| **story-back** | 8000 | Main backend API |
| **postgresql** | 5432 | PostgreSQL 16 database |
| **minio** | 9000/9001 | S3-compatible object storage (API/Console) |

### Minio Note

Starting September 2026, MinIO Inc. removed official Docker images (`minio/minio`, `minio/mc`) from Docker Hub. This compose file uses **pgsty/minio** and **pgsty/mc** — community-maintained forks (Silo) that preserve full S3 API compatibility.

## Stopping

```bash
docker compose down          # Stop services
docker compose down -v       # Stop and delete all data (volumes)
```

## Logs

```bash
docker compose logs -f              # All services
docker compose logs -f story-auth   # Single service
```

## Local Development

If you prefer running frontend/backend locally (not in Docker):

1. Start only infrastructure: `docker compose up -d postgresql minio`
2. Run Spring Boot apps with `STORY_DATASOURCE_HOST=localhost`, `STORY_MINIO_HOST=localhost`
3. Run frontend with `npm start`
