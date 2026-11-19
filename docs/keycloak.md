# Keycloak для Story Project

## Состояние перехода

Keycloak добавлен как отдельный, добровольно запускаемый стенд. Он пока не
заменяет `story-auth`, не изменяет существующих пользователей и не принимает
production-трафик. Это позволяет проверить OIDC и миграцию без риска потерять
вход в действующую систему.

Зафиксирована версия **Keycloak 26.8.2**. Версию нельзя заменять на `latest`:
обновление выполняется отдельным изменением после чтения release notes и проверки
миграции БД. Актуальная версия сверена по [официальной странице загрузок](https://www.keycloak.org/downloads).

## Что создаёт стенд

- отдельный PostgreSQL-контейнер и volume только для Keycloak;
- realm `story` без пользователей;
- публичный клиент `story-frontend`;
- Authorization Code flow с обязательным PKCE `S256`;
- точные локальные redirect URI и web origin;
- отдельные audience `story-back` и `story-admin`;
- отключённые Implicit Flow и Direct Access Grants;
- защита от перебора, ротация refresh token и health/readiness.

Бизнес-разрешения по-прежнему принадлежат Story Project и загружаются из его БД.
Realm-роли существуют только для совместимости на переходном этапе.

## Локальный запуск

Скопируйте локальный env-шаблон, если `.env` ещё не создан, и обязательно
замените демонстрационный пароль администратора:

```powershell
Set-Location C:\story-project\Story-Data
Copy-Item .env.local.example .env
# Отредактируйте KEYCLOAK_DB_PASSWORD и KEYCLOAK_ADMIN_PASSWORD.

docker compose --env-file .env `
  -f docker-compose.yml `
  -f docker-compose.keycloak.yml `
  up -d keycloak-postgresql keycloak
```

После готовности:

- Admin Console: <http://localhost:8080/admin/>
- Realm: `story`
- OIDC discovery: <http://localhost:8080/realms/story/.well-known/openid-configuration>
- readiness внутри Docker-сети: `http://keycloak:9000/health/ready`

### Создание и связывание тестового администратора

Пароли не хранятся в realm JSON. Одноразовый PowerShell-скрипт читает их из
локального `.env`, создаёт `STORY_ADMIN_USERNAME` в Keycloak с временным
`STORY_ADMIN_PASSWORD`, назначает realm-роль `ADMIN` и записывает полученный
Keycloak `sub` в существующий профиль Story:

```powershell
Set-Location C:\story-project
.\Story-Data\tools\Provision-KeycloakAdmin.ps1
```

Скрипт не меняет пароль уже существующего Keycloak-пользователя и не может
перепривязать профиль к другому `sub`. При первом входе с временным паролем
Keycloak потребует установить новый. В Git не попадает ни старый, ни новый пароль.

Проверить состояние без показа секретов:

```powershell
docker compose --env-file .env `
  -f docker-compose.yml `
  -f docker-compose.keycloak.yml `
  ps keycloak keycloak-postgresql
```

Не используйте `docker compose down -v`: эта команда удаляет данные. Обычная
остановка стенда выполняется без `-v`.

Realm импортируется при первом создании. Если realm уже существует, файл импорта
не следует считать механизмом обновления. Изменения realm должны быть оформлены
через контролируемый Admin API/CLI или через пересоздание только тестовой БД.
Экспорт realm не является полной резервной копией: официальная документация
указывает, что экспорт не содержит сессии, отозванные токены и события.

## Production

`start-dev` и порт `127.0.0.1:8080` предназначены только для локального стенда.
Перед production необходимы:

1. HTTPS-домен Keycloak и точные redirect/logout URI.
2. Оптимизированный образ с `kc.sh build` и запуском `start`.
3. Секреты из внешнего secret store, а не из репозитория.
4. Закрытый management port `9000` для health checks.
5. Backup отдельной Keycloak DB и проверенный откат версии.
6. Ограниченный сервисный клиент для Admin API, если он действительно потребуется.

Официальные источники: [контейнер Keycloak](https://www.keycloak.org/server/containers),
[импорт и экспорт realm](https://www.keycloak.org/server/importExport),
[health endpoints](https://www.keycloak.org/observability/health),
[OIDC endpoints](https://www.keycloak.org/securing-apps/oidc-layers).

## Миграция существующих пользователей

Таблица `users`, числовые ID, профили, заявки и работы остаются на месте. Миграция
V9 добавляет nullable-пару `oidc_issuer` + `oidc_subject`. Вместе они уникально
связывают профиль с неизменным Keycloak `sub`; частично заполненная пара запрещена.
Email используется только для первичного сопоставления после отдельной проверки
и не становится постоянным внешним ключом.

Пароли в PostgreSQL хранятся как BCrypt-хеши. Прямой импорт этих строк в Keycloak
не считается рабочей миграцией без проверки формата credential выбранной версии.
Допустимы два варианта:

- временный read-only User Storage provider проверяет старый BCrypt при первом
  входе, после чего credential переносится в Keycloak;
- пользователи импортируются без пароля и проходят управляемое восстановление.

Для текущего тестового окружения выбран управляемый сброс без собственного
BCrypt-адаптера: пользователей всего два, поэтому сложность и доступ адаптера к
старой таблице паролей не оправданы. Администратор создаётся с временным паролем
из локального `.env` и обязан заменить его при первом входе. Тестовый участник
может быть создан заново тем же способом, если его данные понадобятся. `story-auth`
остаётся включённым до проверки frontend и полного сценария отката.

## Параллельная проверка токенов

При запуске через `docker-compose.keycloak.yml` сервисы получают
`STORY_OIDC_ENABLED=true`. Spring OAuth2 Resource Server обрабатывает только JWT,
у которого `iss` точно совпадает с настроенным Keycloak issuer. Для такого токена
проверяются подпись по JWKS, стандартные временные claims, issuer и отдельная
audience сервиса. JWT без Keycloak issuer передаётся существующему legacy-фильтру.

После криптографической проверки сервис ищет профиль по паре `oidc_issuer` и
`oidc_subject`, затем повторно проверяет `banned` и загружает актуальные permissions
из PostgreSQL. Поэтому один токен Keycloak без связанного профиля не даёт доступа,
а блокировка или снятие разрешения действует на следующий запрос.

Параллельный режим пока не включён в основном `docker-compose.yml`. Это сделано
намеренно: до появления проверенного процесса связывания аккаунтов обычный запуск
использует прежние JWT и не зависит от доступности Keycloak.

## План переключения и отката

1. Подключить `story-back` и `story-admin` как Spring OAuth2 Resource Server в
   параллельном режиме, не удаляя текущую JWT-проверку.
2. Проверить issuer, audience, срок токена, `sub`, banned и permissions.
3. Мигрировать небольшую тестовую группу пользователей.
4. Перевести frontend на Authorization Code + PKCE за feature flag.
5. Наблюдать ошибки входа и сохранить возможность вернуть старый login.
6. Отключать `story-auth` только после подтверждённой миграции и резервной копии.
