# Jenkins CI/CD для Story Project

Корневой `Jenkinsfile` разделяет проверку кода, публикацию образов и production-
развёртывание. Обычная сборка ветки или Pull Request ничего не публикует и не
подключается к серверу.

## Что требуется от Jenkins

Jenkins-agent должен работать на Linux и иметь Git, Docker Engine и Docker
Compose v2. Maven, Java и Node устанавливать на agent не нужно: проверки
выполняются в зафиксированных контейнерах Maven 3.9 / Java 21 и Node 20.

Необходимые Jenkins plugins:

- Pipeline и Multibranch Pipeline;
- Credentials Binding;
- SSH Agent;
- JUnit;
- Workspace Cleanup.

Рекомендуется создать Multibranch Pipeline для репозитория и включить обнаружение
веток и Pull Request. Webhook GitHub должен только запускать сборку; право merge
остаётся у владельца репозитория.

## Credentials

Значения хранятся только в Jenkins Credentials:

| ID | Тип | Назначение |
|---|---|---|
| `story-registry` | Username with password | Вход в Docker Registry |
| `story-production-ssh` | SSH username with private key | Docker context через SSH |
| `story-production-env` | Secret file | Production `.env` для Compose |

Secret file должен содержать те же обязательные переменные, что и
`Story-Data/.env.example`. Jenkinsfile не печатает его содержимое. Docker daemon
нельзя публиковать на незащищённом TCP-порту `2375`; deploy принимает только адрес
вида `ssh://deploy@example.com`.

## Этапы pipeline

1. Определяется короткий SHA текущего коммита.
2. Корневой Maven reactor выполняет `clean verify`; Jenkins сохраняет Surefire XML.
3. Frontend выполняет `npm ci`, тесты без watch-режима и production build.
4. Собираются пять образов: `story-auth`, `story-back`, `story-admin`, `frontend`
   и отдельный образ миграций.
5. Публикация разрешена только для ветки `main`, при включённом параметре
   `PUBLISH_IMAGES` и после успешных проверок.
6. Production deploy разрешён только для `main`, требует ручного подтверждения,
   сначала выполняет Flyway, затем по одному обновляет и проверяет сервисы.

Образы получают неизменяемый тег из первых 12 символов commit SHA. Pipeline не
использует `latest` для развёртывания.

## Первая настройка публикации

При запуске сборки `main` задайте:

- `PUBLISH_IMAGES=true`;
- `IMAGE_REPOSITORY=registry.example.com/story`.

После входа через `story-registry` будут опубликованы, например:

```text
registry.example.com/story/story-back:1a2b3c4d5e6f
registry.example.com/story/frontend:1a2b3c4d5e6f
registry.example.com/story/migrations:1a2b3c4d5e6f
```

## Развёртывание и откат

Для deploy нужны параметры:

- `DEPLOY_TO_PRODUCTION=true`;
- `IMAGE_REPOSITORY`;
- `DEPLOY_DOCKER_HOST=ssh://deploy@example.com`;
- `DEPLOY_IMAGE_TAG` — необязательный тег; пустое значение означает SHA текущей
  сборки.

Перед заменой приложений запускается отдельный одноразовый контейнер Flyway.
Обновление останавливается при ошибке миграции или readiness-проверки. Volumes
PostgreSQL и MinIO не удаляются.

Для отката запустите pipeline `main` повторно, укажите SHA ранее успешно
развёрнутой версии в `DEPLOY_IMAGE_TAG`, включите `DEPLOY_TO_PRODUCTION` и
подтвердите действие. Откат приложения не отменяет уже применённые миграции:
поэтому production-миграции должны оставаться обратно совместимыми, а перед
опасным изменением необходим проверенный backup/restore.

## Что ещё нужно проверить на реальном Jenkins

Конфигурация не запускалась на вашем Jenkins и не публиковала образы, потому что
адрес registry, сервер и Credentials пока не предоставлены. Перед первым
production deploy нужно проверить pipeline на тестовом registry и тестовом
Docker-хосте, затем зафиксировать успешный SHA и результат отката на предыдущий
тег.
