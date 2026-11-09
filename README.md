# Story — творческий конкурс (подработка)

Состав проекта:
- `story-back` — бэкенд на Spring Boot 3 (Java 21, PostgreSQL, Minio, JWT)
- `Story-Front` — фронтенд на React 18 + TypeScript (Create React App)
- `Story-Data` — инфраструктура: Docker Compose (PostgreSQL + Minio + Spring)
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
- **story-auth** (регистрация/вход/refresh): `http://localhost:8001`
- **story-back** (бизнес-логика, заявки, отчёты): `http://localhost:8000`
- Swagger UI: `http://localhost:8000/swagger-ui.html`
- PostgreSQL: `localhost:5432`
- Minio: `localhost:9000` (консоль `localhost:9001`)
- Фронтенд: запускается отдельно (см. раздел 2)

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

---

## 2. Запуск фронтенда локально

```bash
cd Story-Front
npm install
npm start        # http://localhost:3000
```

Фронтенд по умолчанию обращается к `http://localhost:8000/api/v1`.

---

## 3. Роли и разделение доступа

Приложение — **одно SPA с ролевым разделением**:

| Роль | Доступ |
|------|--------|
| `PARTICIPANT` | `/profile`, `/application/:contestId` — подача и просмотр своих заявок |
| `ADMIN` | всё выше + `/admin` — админская панель |

Роль определяется из JWT-токена при загрузке приложения (`/api/v1/auth/me`). В шапке (`Header`) отображается текущая роль, доступна ссылка «Админка» только для `ADMIN`.

---

## 4. Что сделано в рамках рефакторинга

- Удалены мёртвые файлы `_REMIND.md`
- Заменён закомментированный плагин в `pom.xml`
- Добавлен `GlobalExceptionHandler` — единая обработка `RuntimeException` и ошибок валидации (`MethodArgumentNotValidException`)
- Добавлены значения по умолчанию для всех обязательных env-переменных в `application.yaml` — приложение стартует даже без `.env`
- Исправлен путь в `docker-compose.yml`: `../Story-Back` → `../story-back`
- Создан `Story-Data/.env.example` с примером всех переменных
- Исправлен `ProtectedRoute` — редирект с несуществующего `/unauthorized` на `/`
- Реализован `Header` с отображением роли и ссылкой на админку
- Добавлен роут `/admin` с защитой `requiredRole="ADMIN"` и заглушка `AdminPage`
- Фронтенд загружает роль пользователя при старте через `/api/v1/auth/me`

---

## 5. Планы на дальнейшее (пункт 4 ТЗ)

- **Вынос системы регистрации/входа** в отдельное приложение `story-auth` (собственный Spring Boot сервис с базой пользователей и JWT). Основное приложение будет верифицировать токены через общий секрет/публичный ключ.
- Аудит-лог («key log») действий — вход, смена прав, смена пароля админом.

---

## 6. Генерация отчётов

Python-утилита в `Story-Tables` (`main.py`, `requirements.txt`). Запуск:

```bash
cd Story-Tables
pip install -r requirements.txt
python main.py
```
