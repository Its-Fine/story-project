# Сверка переданного API и Postman с кодом

Проверены «Запросы 3.xlsx.pdf», переданная коллекция `Story.postman_collection.json`
и действующие контроллеры/DTO (24.11.2026). Базовый путь через Gateway:
`http://localhost:3000/api/v1`. PDF и исходная коллекция — ориентиры; источником
истины остаётся код. Поля не переименовываются массово без перехода frontend.

| Раздел | Реальное состояние | Решение |
|---|---|---|
| Auth signup | JSON использует `first_name`, `last_name`, `birth_date`, `parent_consent`; PDF — camelCase | Сохранить работающий контракт, отдельно согласовать унификацию |
| Auth response | `access_token`, refresh в HttpOnly cookie `refreshToken`; PDF — `accessToken` | Сохранить совместимость с текущим frontend |
| Auth дополнительные маршруты | Есть `/me`, `/logout`, `PUT /reset`, отсутствующие в PDF | Включить в будущую OpenAPI-спецификацию; reset требует текущий пароль |
| Account GET | Возвращает `applicationsCount`, PDF — `contestsCount` | Это разные показатели, не переименовывать механически |
| Account PUT | `RequestUserDataDto`/service меняют имя и фамилию; дата рождения из PDF не реализована | Отдельно решить правила смены возраста после подачи заявки |
| Applications | CRUD реализован; ownership проверяется сервисами, DTO имеют дополнительные поля состояния | Нужны негативные интеграционные тесты чужих заявок при выделении admin |
| Works download | Ответ — байты с Content-Type файла, а не `multipart/form-data` | Исправить описание контракта при OpenAPI; загрузка остаётся multipart |
| Contests | `/all` и `/contests` относятся к participant API; административного CRUD пока нет | Добавить admin CRUD отдельной итерацией T12 |
| Admin user access | Старый `/access` меняет `closedAccess`; реальная блокировка выполняется новым `/banned`. Роли меняются через `/roles` | Сохранить `/access` для совместимости; блокировка и роли требуют текущий пароль и пишутся в аудит |
| Admin password | DTO требует `currentPassword` и `newPassword`, PDF — только `password` | Не ослаблять подтверждение пароля; согласовать контракт с Keycloak |
| Admin contests | CRUD реализован в `story-admin` по `/admin/contests` | Удаление запрещено при наличии связанных заявок |
| Event status | В текущем JSON `close`, в PDF `closed`; состояние было в памяти и зависело от личного исключения | Текущий шаг: БД, глобальное состояние, совместимость обоих имён |
| Reports | `/reports/applications/excel` обслуживается `story-admin`, Gateway направляет его отдельно | Сохранить URL для совместимости frontend |

## Расхождения исходной Postman-коллекции

- `host` должен быть `http://localhost:3000/api/v1`, а не прямой порт Java-сервиса;
- signup в исходной коллекции использует camelCase, но текущий auth-контракт требует
  `first_name`, `last_name`, `birth_date`, `parent_consent`;
- отсутствовали `/auth/me`, `/auth/logout`, `/auth/reset`;
- отсутствовали смена статуса заявки, оценивание работы, роли, реальная блокировка
  пользователя и Excel-отчёт;
- refresh-токен передаётся HttpOnly cookie и не копируется вручную в Bearer header.

Актуальная импортируемая коллекция хранится в `docs/postman`.

## Владение текущими маршрутами

| Реализация сегодня | Маршруты | Планируемый владелец |
|---|---|---|
| `story-auth` | `/auth/signup`, `/login`, `/refresh`, `/logout`, `/me`, `/reset` | `story-auth` |
| `story-back` | `/account/**`, `/applications/**`, `/works/**`, `/contests/**`, `/event/status` | participant API |
| `story-admin` | `/admin/users/**`, `/admin/applications/**`, `/admin/works/**`, `/admin/event/status` | administrative API |
| `story-admin` | `/reports/applications/excel` | administrative reports |

Сервисы физически разделены, но используют общую PostgreSQL с разными правами.
Gateway не является средством авторизации: JWT, permissions, ban и ownership
проверяются внутри соответствующего сервиса.
