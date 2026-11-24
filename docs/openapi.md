# OpenAPI Story Project

После запуска Docker Compose актуальные спецификации доступны через единый
Gateway. Внутренние порты Java-сервисов при этом не публикуются:

| Сервис | OpenAPI JSON |
|---|---|
| Авторизация | `http://localhost:3000/docs/auth/openapi.json` |
| Пользовательский backend | `http://localhost:3000/docs/back/openapi.json` |
| Административный backend | `http://localhost:3000/docs/admin/openapi.json` |

JSON можно открыть в браузере, импортировать в Postman или загрузить в Swagger
Editor. Для повседневных ручных проверок удобнее готовая коллекция из
`docs/postman/Story.postman_collection.json`.

OpenAPI описывает контракт, но не отменяет авторизацию. Защищённые операции
по-прежнему требуют access-токен, permissions и проверку актуального `banned` в БД.
Спецификации не содержат паролей, JWT-секретов или пользовательских данных.

В production внешний reverse proxy может ограничить `/docs/**` по IP или закрыть
его полностью, если публичная документация не требуется.
