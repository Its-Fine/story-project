# Story Project — понятная карта проекта

## Какая версия является основной

Основная версия использует собственный сервис `story-auth`: регистрация, вход,
refresh-cookie и JWT остаются внутри проекта. Полная авторизация и регистрация
через Keycloak хранится отдельно в черновом PR #26 и сейчас не входит в `main`.
Kubernetes для текущего развёртывания на одном сервере не требуется.

## Как проходит запрос

```text
Браузер
  → frontend / Nginx на порту 3000
      → /api/v1/auth/*                 → story-auth
      → /api/v1/admin/* и /reports/*   → story-admin
      → остальные /api/v1/*            → story-back
          → PostgreSQL и MinIO
```

Порты Java-сервисов 8000–8002 доступны только внутри Docker. Наружу открывается
единый Gateway на порту 3000.

## Где что лежит

| Путь | Что находится внутри |
|---|---|
| `Story-Front/src/pages` | страницы входа, профиля, заявки и админки |
| `Story-Front/src/api` | обращения frontend к API |
| `Story-Front/nginx.conf` | маршрутизация Gateway |
| `story-auth` | регистрация, вход, JWT, refresh и смена пароля |
| `story-back` | профиль, конкурсы, заявки, работы и файлы участника |
| `story-admin` | пользователи, роли, модерация, оценки и Excel-отчёт |
| `story-common` | общие сущности, DTO, репозитории, permissions и MinIO-клиент |
| `story-back/src/main/resources/db/migration` | миграции Flyway |
| `Story-Data/docker-compose.yml` | состав контейнеров, сети и зависимости запуска |
| `Story-Data/tools` | утилиты запуска, логов, обновления и SSH |
| `Jenkinsfile` и `ci` | автоматическая проверка и развёртывание |
| `TASKS.md` | актуальный план разработки |
| `docs/postman` | актуальная коллекция запросов и инструкция Postman |
| `docs/openapi.md` | адреса машинно-читаемых спецификаций API |

## Кто за что отвечает

### story-auth

Работает с учётными данными и выдаёт токены. Он не обрабатывает заявки, работы
или административную модерацию.

### story-back

Обслуживает действия участника только над собственными данными. Административных
контроллеров в этом сервисе нет.

### story-admin

Обслуживает административные операции. Доступ проверяется через permissions из
БД; наличие Gateway не заменяет проверку внутри самого сервиса.

### story-common

Не запускается как контейнер. Это общая Java-библиотека для `story-back` и
`story-admin`, чтобы не копировать сущности и правила безопасности.

### Story-Data

Запускает PostgreSQL, MinIO, три Java-сервиса и frontend. Здесь же находятся
безопасные команды обслуживания без удаления volumes.

## Самые частые действия

```powershell
# Первый запуск
.\Story-Data\tools\storyctl.ps1 init-env
.\Story-Data\tools\storyctl.ps1 start

# Проверка состояния и логов
.\Story-Data\tools\storyctl.ps1 status
.\Story-Data\tools\storyctl.ps1 logs story-back

# Проверка backend
mvn verify

# Проверка frontend
Set-Location .\Story-Front
npm ci
npm run build
```

Подробности конфигурации находятся в `docs/configuration.md`, а порядок безопасного
обновления сервера — в `docs/maintenance.md` и `docs/ci-cd.md`. Проверка API через
Gateway описана в `docs/postman/README.md`.
