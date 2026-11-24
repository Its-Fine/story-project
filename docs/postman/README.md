# Postman для Story Project

Импортируйте файл `Story.postman_collection.json` в Postman. Коллекция по
умолчанию обращается к единой точке входа Gateway:

```text
http://localhost:3000/api/v1
```

## Быстрый порядок проверки

1. Заполните переменные `email`, `password`, `adminEmail`, `adminPassword`.
2. Выполните `Auth / Login participant` или `Auth / Login administrator`.
   Успешный запрос автоматически сохранит `access_token` в `accessToken`.
3. Выполняйте participant- или admin-запросы. Bearer-токен подставляется
   автоматически на уровне коллекции.
4. Refresh использует HttpOnly cookie `refreshToken`. Postman сохраняет его из
   ответа login автоматически; вручную копировать cookie в Bearer header не нужно.
5. После создания заявки сохраните её ID в `appId`, а после загрузки работы — ID
   в `workId`.

Регистрация использует snake_case только там, где это явно закреплено текущим
auth-контрактом: `first_name`, `last_name`, `birth_date`, `password_again`,
`parent_consent`. Остальные DTO используют camelCase.

Не сохраняйте реальные production-пароли или токены внутри коллекции.
