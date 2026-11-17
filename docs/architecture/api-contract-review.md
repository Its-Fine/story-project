# Сверка переданного API с кодом

Проверены обе страницы «Запросы 3.xlsx.pdf» и контроллеры/DTO после PR #8
(16.11.2026). Базовый путь текущих сервисов: `/api/v1`. Таблица из PDF — ориентир;
расхождения не исправляются массовым переименованием полей без перехода frontend.

| Раздел | Реальное состояние | Решение |
|---|---|---|
| Auth signup | JSON использует `first_name`, `last_name`, `birth_date`, `parent_consent`; PDF — camelCase | Сохранить работающий контракт, отдельно согласовать унификацию |
| Auth response | `access_token`, refresh в HttpOnly cookie `refreshToken`; PDF — `accessToken` | Сохранить совместимость с текущим frontend |
| Auth дополнительные маршруты | Есть `/me`, `/logout`, `PUT /reset`, отсутствующие в PDF | Включить в будущую OpenAPI-спецификацию; reset требует текущий пароль |
| Account GET | Возвращает `applicationsCount`, PDF — `contestsCount` | Это разные показатели, не переименовывать механически |
| Account PUT | `RequestUserDataDto`/service меняют имя и фамилию; дата рождения из PDF не реализована | Отдельно решить правила смены возраста после подачи заявки |
| Applications | CRUD реализован; ownership проверяется сервисами, DTO имеют дополнительные поля состояния | Нужны негативные интеграционные тесты чужих заявок при выделении admin |
| Works download | Ответ — байты с Content-Type файла, а не `multipart/form-data` | Исправить описание контракта при OpenAPI; загрузка остаётся multipart |
| Contests | `/all` и `/contests` защищены `hasRole('PARTICIPANT')`; PDF допускает ADMIN для доступных номинаций | Дать admin собственную операцию чтения при выделении сервиса |
| Admin user access | DTO содержит `currentPassword`, `access`; меняется только `closedAccess`. `banned` и `role` из PDF отсутствуют | Добавлять отдельным изменением с permissions, аудитом, защитой последнего администратора |
| Admin password | DTO требует `currentPassword` и `newPassword`, PDF — только `password` | Не ослаблять подтверждение пароля; согласовать контракт с Keycloak |
| Admin contests | В PDF `POST/PUT/DELETE` отмечены `TO DO`, контроллера нет | Отдельный этап CRUD номинаций |
| Event status | В текущем JSON `close`, в PDF `closed`; состояние было в памяти и зависело от личного исключения | Текущий шаг: БД, глобальное состояние, совместимость обоих имён |
| Reports | Есть `/reports/applications/excel`, административный маршрут вне `/admin` | Перенести вместе с остальным admin-функционалом |

## Владение текущими маршрутами

| Реализация сегодня | Маршруты | Планируемый владелец |
|---|---|---|
| `story-auth` | `/auth/signup`, `/login`, `/refresh`, `/logout`, `/me`, `/reset` | auth/Keycloak с переходным API |
| `story-back`, пользовательские контроллеры | `/account/**`, `/applications/**`, `/works/**`, `/contests/**`, `/event/status` | participant service |
| `story-back`, `controller/admin` | `/admin/users/**`, `/admin/applications/**`, `/admin/works/**`, `/admin/event/status` | отдельный `story-admin` |
| `story-back`, `ReportController` | `/reports/applications/excel` | `story-admin` с совместимым переходом URL |

Этот документ не утверждает, что сервисы уже физически разделены. Проверки JWT,
банов и ролей из PR #8 сохраняются. Равенство URL-префиксов и защищённость данных
проверяются отдельно: нельзя считать интерфейс скрытым только потому, что ссылка
на него не показывается участнику.
