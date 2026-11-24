# Story-Data — запуск и обслуживание

В этой папке находятся Docker Compose, настройки PostgreSQL и утилиты управления
проектом. Пользовательские данные хранятся в Docker volumes и не удаляются при
обычной остановке контейнеров.

## Первый локальный запуск на Windows

Из корня репозитория выполните:

```powershell
.\Story-Data\tools\storyctl.ps1 init-env
.\Story-Data\tools\storyctl.ps1 check-config
.\Story-Data\tools\storyctl.ps1 start
```

После запуска приложение доступно по адресу <http://localhost:3000>.

`init-env` создаёт `Story-Data/.env` со случайными паролями и JWT-ключом. Файл
`.env` нельзя добавлять в Git. Production-шаблон `.env.example` не содержит
секретов и не позволяет запустить Compose, пока обязательные значения пусты.

## Контейнеры

| Контейнер | Назначение | Доступ с компьютера |
|---|---|---|
| `frontend` | React и Nginx Gateway | `localhost:3000` |
| `story-auth` | Регистрация, вход и JWT | только внутри Docker, порт 8001 |
| `story-back` | Пользовательский API | только внутри Docker, порт 8000 |
| `story-admin` | Административный API | только внутри Docker, порт 8002 |
| `postgresql` | Основная БД | `127.0.0.1:5432` |
| `minio` | Файлы работ | `127.0.0.1:9000`, консоль 9001 |
| `minio-init` | Одноразовое создание bucket | постоянно работать не должен |

Java-сервисы не публикуются наружу. Браузер обращается только к frontend, а Nginx
направляет запросы в нужный внутренний сервис.

## Повседневные команды

```powershell
.\Story-Data\tools\storyctl.ps1 status
.\Story-Data\tools\storyctl.ps1 logs
.\Story-Data\tools\storyctl.ps1 logs story-back
.\Story-Data\tools\storyctl.ps1 restart story-back
.\Story-Data\tools\storyctl.ps1 update story-back
```

Версии для Linux находятся в `tools/storyctl.sh`. Работа через Docker SSH context
описана в [инструкции обслуживания](../docs/maintenance.md).

## Зависимости запуска

- PostgreSQL должен пройти health check до запуска backend.
- `story-back` применяет Flyway-миграции и подготавливает общую схему БД.
- `story-auth` и `story-admin` ждут готовности `story-back`, чтобы не обращаться к
  ещё не обновлённой схеме.
- `minio-init` ждёт MinIO и создаёт bucket с повторными попытками.
- `story-admin` ждёт готовности схемы и завершения `minio-init`.
- frontend запускается после создания трёх API-сервисов.

Эти зависимости соответствуют текущему владельцу миграций. При выделении
миграций в отдельный обязательный контейнер зависимости можно будет ослабить.

## Безопасная остановка

```powershell
Set-Location .\Story-Data
docker compose down
```

Команда сохраняет PostgreSQL и MinIO. Не используйте `docker compose down -v`
при обычной работе: параметр `-v` удаляет все данные проекта.

Дополнительные инструкции:

- [настройки и создание администратора](../docs/configuration.md);
- [миграции БД](../docs/database-migrations.md);
- [маршруты Gateway](../docs/architecture/api-gateway.md);
- [Jenkins и развёртывание](../docs/ci-cd.md).
