# API Gateway и сетевые границы

Единственная браузерная точка входа в локальном Compose — Nginx внутри
`frontend` на `http://localhost:3000`. Java-сервисы больше не публикуют порты
8000–8002 на хост и доступны только во внутренней сети `app`.

## Маршрутизация

| Внешний путь | Внутренний сервис | Доступ |
|---|---|---|
| `/api/v1/auth/**` | `story-auth:8001` | anonymous / authenticated |
| `/api/v1/admin/**` | `story-admin:8002` | ADMIN |
| `/api/v1/reports/**` | `story-admin:8002` | ADMIN |
| `/api/v1/participant/**` | `story-back:8000`, с удалением `/participant` | authenticated participant API |
| `/api/v1/**` | `story-back:8000` | временная совместимость frontend |
| `/docs/auth/openapi.json` | `story-auth:8001/v3/api-docs` | контракт auth API |
| `/docs/back/openapi.json` | `story-back:8000/v3/api-docs` | контракт participant API |
| `/docs/admin/openapi.json` | `story-admin:8002/v3/api-docs` | контракт admin API |

Проверки ролей остаются внутри сервисов. Nginx выбирает upstream, но не считается
источником авторизации и не заменяет JWT, проверку ban или ownership.

## Docker-сети

- `edge`: публичный Nginx;
- `app`: Nginx и три Java-сервиса;
- `data`: Java-сервисы и PostgreSQL;
- `storage`: `story-back`, `story-admin` и MinIO.

PostgreSQL и MinIO в локальной конфигурации привязаны только к `127.0.0.1`, чтобы
сохранить pgAdmin и MinIO Console без публикации в локальную сеть. В production
их host ports следует удалить полностью. HTTPS завершается на внешнем reverse
proxy/Ingress; production Origin и cookie Secure задаются через `.env`.

## Совместимость

Frontend продолжает использовать `/api/v1/**`. Новые participant-маршруты можно
переводить на `/api/v1/participant/**` постепенно. После завершения перехода
legacy catch-all удаляется отдельным изменением, иначе старые клиенты внезапно
перестанут работать.
