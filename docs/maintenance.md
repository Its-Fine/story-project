# Обслуживание Story Project

Утилита `Story-Data/tools/storyctl` выполняет обычные операции Docker Compose без
удаления volumes. Она не печатает содержимое `.env` и не содержит команды
`down -v`, `system prune` или удаления базы данных.

## Windows PowerShell

```powershell
Set-Location C:\story-project

# Создать Story-Data/.env и заполнить только отсутствующие генерируемые значения.
.\Story-Data\tools\storyctl.ps1 init-env

# После ручного заполнения домена/CORS проверить файл и Compose.
.\Story-Data\tools\storyctl.ps1 check-config

.\Story-Data\tools\storyctl.ps1 status
.\Story-Data\tools\storyctl.ps1 logs story-back -Follow
.\Story-Data\tools\storyctl.ps1 restart story-back
.\Story-Data\tools\storyctl.ps1 update story-back
```

`restart` перезапускает существующий контейнер. `update` заново собирает образ и
пересоздаёт только выбранный сервис; volumes PostgreSQL и MinIO не затрагиваются.

## Linux

```bash
cd /opt/story-project
./Story-Data/tools/storyctl.sh init-env
./Story-Data/tools/storyctl.sh check-config
./Story-Data/tools/storyctl.sh status
STORY_LOG_FOLLOW=true ./Story-Data/tools/storyctl.sh logs story-back
./Story-Data/tools/storyctl.sh restart story-back
./Story-Data/tools/storyctl.sh update story-back
```

Другой env-файл выбирается через `-EnvFile production.env` в PowerShell или
`STORY_ENV_FILE=production.env` в Linux. Существующие непустые значения команда
`init-env` никогда не заменяет. После создания файла нужно вручную указать
`STORY_CORS_ALLOWED_ORIGINS` и проверить HTTPS/cookie-настройки.

## Docker через SSH

Не открывайте Docker daemon на TCP-порту `2375`. Создайте именованный context,
который подключается по SSH:

```bash
docker context create story-production --docker host=ssh://deploy@example.com
docker --context story-production info
```

На сервере пользователь `deploy` должен входить в ограниченную группу, имеющую
доступ к Docker, а вход по SSH должен использовать ключ. Затем явно передавайте
context утилите:

```powershell
.\Story-Data\tools\storyctl.ps1 status -DockerContext story-production
.\Story-Data\tools\storyctl.ps1 logs story-back -DockerContext story-production -Follow
```

```bash
STORY_DOCKER_CONTEXT=story-production ./Story-Data/tools/storyctl.sh status
```

Важно: env-файл и исходники Compose читаются на локальной машине, с которой
запущена команда, а контейнеры создаются удалённым Docker Engine. Для production
предпочтительнее собирать и публиковать готовые образы в CI; это будет частью T10.

## Безопасный порядок обновления

1. Сохранить backup PostgreSQL и проверить свободное место.
2. Выполнить `check-config`.
3. Обновить один сервис командой `update <service>`.
4. Проверить `status` и `logs <service>`.
5. Только после проверки переходить к следующему сервису.

Команда не выполняет `git pull`: выбор конкретного коммита остаётся явным
действием оператора и позднее будет связан с тегом Docker-образа в Jenkins.
