# Безопасная конфигурация и начальное заполнение

## Файлы окружения

- `Story-Data/.env.example` — единственный шаблон с пустыми секретами. Compose не
  запускает стек, пока обязательные значения не заполнены.
- `.env` исключён из Git. Не отправляйте его в репозиторий и не вставляйте секреты
  в логи, команды CI или Pull Request.

Для локальной разработки создайте `.env` безопасной утилитой: она генерирует
разные случайные пароли и JWT-ключ, не перезаписывая существующие значения.

```powershell
.\Story-Data\tools\storyctl.ps1 init-env
```

Затем явно задайте локальные cookie/CORS-настройки: `STORY_COOKIE_SECURE=false`,
`STORY_COOKIE_SAME_SITE=Lax` и `STORY_CORS_ALLOWED_ORIGINS=http://localhost:3000`.

JWT-ключ должен быть корректной Base64-строкой, декодирующейся минимум в 32 байта.
Пример генерации в PowerShell:

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Оба backend-сервиса должны получать один и тот же `STORY_JWT_SECRET`. При его
отсутствии, неправильном Base64 или слишком коротком ключе запуск завершается
ошибкой конфигурации до обслуживания запросов.

## Cookie, HTTPS и CORS

Для production используйте HTTPS и следующие базовые значения:

```dotenv
STORY_COOKIE_SECURE=true
STORY_COOKIE_SAME_SITE=Lax
STORY_CORS_ALLOWED_ORIGINS=https://contest.example.com
```

Origin указываются точно, через запятую, без `*`. Режим `SameSite=None` разрешён
только вместе с `Secure=true`. Refresh token остаётся HttpOnly-cookie с путём
`/api/v1/auth/refresh`; access token передаётся заголовком `Authorization`.

CSRF отключён для stateless API: изменяющие бизнес-операции требуют bearer-токен,
который браузер не добавляет автоматически. Cookie участвует только в получении
нового access token; риск межсайтового чтения ответа ограничен точным CORS,
`SameSite` и HTTPS. При переходе на межсайтовый frontend или расширении cookie-
эндпоинтов необходимо добавить CSRF-токен до изменения этой модели.

## Одноразовое создание администратора

Обычный запуск использует `STORY_BOOTSTRAP_ADMIN_ENABLED=false`. Для первого
администратора задайте уникальный пароль длиной не менее 12 символов и выполните:

```powershell
Set-Location .\Story-Data
$env:STORY_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:STORY_ADMIN_USERNAME = 'admin@example.com'
$env:STORY_ADMIN_PASSWORD = 'replace-with-a-strong-password'
docker compose up -d --build story-back
Remove-Item Env:STORY_BOOTSTRAP_ADMIN_ENABLED, Env:STORY_ADMIN_USERNAME, Env:STORY_ADMIN_PASSWORD
docker compose up -d --force-recreate story-back
```

Если пользователь с таким email уже существует, его пароль, профиль и роль не
перезаписываются. Не сохраняйте bootstrap-флаг включённым в `.env`.

## Одноразовая загрузка конкурсов

Начальный каталог хранится в `story-back/src/main/resources/bootstrap/contests.json`,
а не в Java-коде. Для явной идемпотентной загрузки выполните:

```powershell
Set-Location .\Story-Data
$env:STORY_BOOTSTRAP_CONTESTS_ENABLED = 'true'
docker compose up -d --build story-back
Remove-Item Env:STORY_BOOTSTRAP_CONTESTS_ENABLED
docker compose up -d --force-recreate story-back
```

Существующие конкурсы с тем же точным названием не изменяются. При обычных
рестартах флаг выключен, поэтому удалённые администратором конкурсы не создаются
заново. Повторно включайте загрузку только осознанно.

## Настройки без изменения исходников

- В production frontend по умолчанию обращается к относительному пути
  `/api/v1` через Nginx Gateway. Для standalone `npm start` локальные URL
  задаются в `Story-Front/.env.development`; не переносите их в production.
- Порт публикации frontend на хосте задаёт `STORY_FRONTEND_HOST_PORT`;
  порты PostgreSQL и MinIO — `STORY_POSTGRES_HOST_PORT`,
  `STORY_MINIO_API_HOST_PORT` и `STORY_MINIO_CONSOLE_HOST_PORT`.
  Внутренние порты контейнеров остаются неизменными.
- При **первом** создании администратора можно указать
  `STORY_BOOTSTRAP_ADMIN_FIRST_NAME`, `STORY_BOOTSTRAP_ADMIN_LAST_NAME`
  и `STORY_BOOTSTRAP_ADMIN_BIRTH_DATE` (формат YYYY-MM-DD).
  Изменение этих переменных не переписывает существующего пользователя.
- Допустимые MIME-типы загружаемых работ задаёт
  `STORY_WORK_ALLOWED_CONTENT_TYPES` через запятую. Проверка выполняется
  на backend; изменение настройки само по себе не проверяет содержимое файла.

Все эти параметры находятся в шаблонах `Story-Data/.env*.example`.
Переносимые в контейнер значения перечислены в `Story-Data/docker-compose.yml`.
