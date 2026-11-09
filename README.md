# Story-Back

# Story — творческий конкурс

Состав проекта:
- `story-auth` — отдельный сервис аутентификации (Spring Boot 3, Java 21, JWT, httpOnly refresh-cookie)
- `story-back` — бэкенд на Spring Boot 3 (Java 21, PostgreSQL, Minio, JWT)
- `Story-Front` — фронтенд на React 18 + TypeScript (Create React App)
- `Story-Data` — инфраструктура: Docker Compose (PostgreSQL + Minio + Spring + Frontend)
- `Story-Tables` — Python-утилита для генерации отчётов/дипломов

---

## 1. Быстрый старт через Docker (рекомендуется)

Все сервисы поднимаются одним `docker-compose` из папки `Story-Data`.

### 1.1 Подготовка окружения

```bash
cd Story-Data
cp .env.example .env
# отредактируйте .env при необходимости (пароли, секрет JWT и т.д.)
```

### 1.2 Запуск

```bash
# из папки Story-Data
docker compose up -d --build
```

Сервисы:
- **Фронтенд** (SPA): `http://localhost:3000`
- **story-auth** (регистрация/вход/refresh): `http://localhost:8001`
- **story-back** (бизнес-логика, заявки, отчёты): `http://localhost:8000`
- Swagger UI: `http://localhost:8000/swagger-ui.html`
- PostgreSQL: `localhost:5432`
- Minio: `localhost:9000` (консоль `localhost:9001`)

Остановить:
```bash
docker compose down
```

### 1.3 Переменные окружения

Пример `.env` лежит в `Story-Data/.env.example`. Обязательные минимум:
- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`
- `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_MC_BUCKET`
- `STORY_JWT_SECRET` — Base64-секрет не менее 32 байт
- `STORY_ADMIN_USERNAME`, `STORY_ADMIN_PASSWORD` — создаваемый супер администратор при старте
- `STORY_COOKIE_SECURE` — `false` для localhost (http), `true` для продакшена (https)

---

## 2. Запуск фронтенда локально

```bash
cd Story-Front
npm install
npm start        # http://localhost:3000
```

Фронтенд по умолчанию обращается к `http://localhost:8000/api/v1` и `http://localhost:8001/api/v1`.

---

## 3. Роли и разделение доступа

Приложение — **одно SPA с ролевым разделением**:

| Роль | Доступ |
|------|--------|
| `PARTICIPANT` | `/profile`, `/application/:contestId` — подача и просмотр своих заявок |
| `ADMIN` | всё выше + `/admin` — админская панель |

Роль определяется из JWT-токена при загрузке приложения (`/api/v1/auth/me`). В шапке (`Header`) отображается текущая роль, доступна ссылка «Админка» только для `ADMIN`.

---

## 4. Архитектура аутентификации

- **story-auth** — отдельный Spring Boot сервис (порт 8001), отвечает за регистрацию, логин, refresh, сброс пароля. Использует общую таблицу `users` с story-back.
- **story-back** — верифицирует JWT через общий `STORY_JWT_SECRET`, не управляет пользователями напрямую.
- **Frontend (nginx)** — проксирует `/api/v1/auth/*` на story-auth, остальные `/api/v1/*` на story-back. Один origin — нет проблем с CORS.

---

## 5. Что сделано

- ✅ Единая таблица пользователей `users` (оба сервиса смотрят в одну БД)
- ✅ Auth как отдельное приложение (story-auth: signup/login/refresh/me/logout/reset)
- ✅ Excel-отчёт через Apache POI (внутри story-back)
- ✅ SPA с ролевыми роутами
- ✅ Docker Compose: PostgreSQL + Minio + story-auth + story-back + frontend
- ✅ CORS настроен в обоих Spring-сервисах
- ✅ Cookie configurable через `STORY_COOKIE_SECURE`

---

## 6. Генерация отчётов

Python-утилита в `Story-Tables` (`main.py`, `requirements.txt`). Запуск:

```bash
cd Story-Tables
pip install -r requirements.txt
python main.py
```
