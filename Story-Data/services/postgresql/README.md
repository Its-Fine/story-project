# PostgreSQL

Единственный актуальный набор миграций находится в
`story-back/src/main/resources/db/migration` и выполняется Flyway до запуска JPA.
Старые `db-init.sql` и отдельная папка `migrations` удалены как не подключённые к
Compose дубли, которые могли расходиться с реальной схемой.

Скрипт `init/01-create-service-roles.sh` создаёт служебные роли при инициализации
нового volume. Для существующей базы порядок действий описан в
`docs/database-migrations.md`.
