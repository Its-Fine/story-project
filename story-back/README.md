# story-back — пользовательский backend

Spring Boot сервис на Java 21. Он обслуживает действия участника и не содержит
административных контроллеров.

## Основные API

| Путь | Назначение |
|---|---|
| `/api/v1/account` | профиль и изменение пароля |
| `/api/v1/applications` | собственные заявки участника |
| `/api/v1/works` | загрузка, чтение и удаление собственных работ |
| `/api/v1/contests` | список конкурсов |
| `/api/v1/event/status` | текущий статус приёма заявок |

Административные маршруты находятся в соседнем сервисе `story-admin`.

## Где находится код

- `src/main/java/com/example/story/controller` — HTTP-контроллеры;
- `src/main/java/com/example/story/service` — пользовательская бизнес-логика;
- `src/main/java/com/example/story/security` — настройка доступа;
- `src/main/java/com/example/story/configuration` — bootstrap и конфигурация;
- `src/main/resources/db/migration` — миграции Flyway;
- `src/test` — тесты сервиса.

Общие сущности, DTO, репозитории и MinIO-клиент находятся в модуле
`story-common`.

## Проверка

Из корня репозитория:

```powershell
mvn -pl story-back -am verify
```

Docker-сборка также выполняет тесты:

```powershell
docker build -f story-back/Dockerfile -t story-back:local .
```

В составе Compose сервис использует внутренний порт `8000`. С компьютера API
доступен через Nginx Gateway frontend, а не напрямую через порт 8000.
