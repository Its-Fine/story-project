#!/usr/bin/env bash
set -Eeuo pipefail

command_name="${1:-}"
service="${2:-}"
env_file="${STORY_ENV_FILE:-.env}"
docker_context="${STORY_DOCKER_CONTEXT:-}"
tail_lines="${STORY_LOG_TAIL:-100}"
follow="${STORY_LOG_FOLLOW:-false}"

case "$command_name" in
  init-env|check-config|status|logs|restart|update) ;;
  *) echo "Usage: $0 {init-env|check-config|status|logs|restart|update} [service]" >&2; exit 2 ;;
esac

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
data_dir="$(cd -- "$script_dir/.." && pwd)"
compose_file="$data_dir/docker-compose.yml"
template_file="$data_dir/.env.example"
[[ "$env_file" = /* ]] || env_file="$data_dir/$env_file"

required_variables=(
  POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
  STORY_MIGRATION_DB_PASSWORD STORY_AUTH_DB_PASSWORD STORY_BACK_DB_PASSWORD STORY_ADMIN_DB_PASSWORD
  MINIO_ROOT_USER MINIO_ROOT_PASSWORD MINIO_MC_BUCKET
  STORY_JWT_SECRET STORY_COOKIE_SECURE STORY_COOKIE_SAME_SITE STORY_CORS_ALLOWED_ORIGINS
)

read_value() {
  local name="$1"
  awk -F= -v key="$name" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$env_file"
}

set_missing_value() {
  local name="$1" value="$2" current temp_file
  current="$(read_value "$name")"
  [[ -n "$current" ]] && return 0
  temp_file="$(mktemp)"
  awk -v key="$name" -v replacement="$name=$value" '
    BEGIN { found = 0 }
    $0 ~ "^" key "=" { print replacement; found = 1; next }
    { print }
    END { if (!found) print replacement }
  ' "$env_file" > "$temp_file"
  chmod --reference="$env_file" "$temp_file" 2>/dev/null || true
  mv -- "$temp_file" "$env_file"
}

random_secret() {
  local bytes="${1:-32}"
  openssl rand -base64 "$bytes" | tr -d '\n'
}

docker_cmd() {
  local docker_args=()
  [[ -n "$docker_context" ]] && docker_args+=(--context "$docker_context")
  docker "${docker_args[@]}" "$@"
}

compose() {
  docker_cmd compose --file "$compose_file" --env-file "$env_file" "$@"
}

wait_service_ready() {
  local service_name="$1" timeout="${2:-90}" container_id deadline state exit_code
  container_id="$(compose ps --quiet "$service_name" | head -n 1)"
  [[ -n "$container_id" ]] || { echo "No container was created for $service_name." >&2; return 1; }
  deadline=$((SECONDS + timeout))
  while ((SECONDS < deadline)); do
    state="$(docker_cmd inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id")"
    if [[ "$state" == 'running healthy' || "$state" == 'running none' ]]; then
      echo "$service_name is ready ($state)."
      return 0
    fi
    if [[ "$service_name" == 'minio-init' && "$state" == 'exited none' ]]; then
      exit_code="$(docker_cmd inspect --format '{{.State.ExitCode}}' "$container_id")"
      [[ "$exit_code" == '0' ]] && { echo 'minio-init completed successfully.'; return 0; }
    fi
    if [[ "$state" =~ ^(exited|dead|removing)\  || "$state" == 'running unhealthy' ]]; then
      echo "$service_name failed readiness check: $state" >&2
      return 1
    fi
    sleep 3
  done
  echo "$service_name did not become ready within $timeout seconds." >&2
  return 1
}

check_config() {
  [[ -f "$env_file" ]] || { echo "Environment file not found: $env_file. Run init-env first." >&2; return 1; }
  local missing=() name value
  for name in "${required_variables[@]}"; do
    value="$(read_value "$name")"
    [[ -n "$value" ]] || missing+=("$name")
  done
  if ((${#missing[@]} > 0)); then
    echo "Required variables are missing or empty: ${missing[*]}" >&2
    return 1
  fi
  local decoded_bytes
  if ! decoded_bytes="$(read_value STORY_JWT_SECRET | openssl base64 -d -A 2>/dev/null | wc -c)" || ((decoded_bytes < 32)); then
    echo 'STORY_JWT_SECRET must be valid Base64 and decode to at least 32 bytes.' >&2
    return 1
  fi
  if [[ "$(read_value STORY_COOKIE_SAME_SITE)" == "None" && "$(read_value STORY_COOKIE_SECURE)" != "true" ]]; then
    echo 'SameSite=None requires STORY_COOKIE_SECURE=true.' >&2
    return 1
  fi
  if [[ "$(read_value STORY_CORS_ALLOWED_ORIGINS)" == *"*"* ]]; then
    echo 'STORY_CORS_ALLOWED_ORIGINS must not contain a wildcard.' >&2
    return 1
  fi
}

case "$command_name" in
  init-env)
    if [[ ! -f "$env_file" ]]; then
      cp -- "$template_file" "$env_file"
      chmod 600 "$env_file"
      echo "Created $env_file from the production template."
    fi
    command -v openssl >/dev/null || { echo 'openssl is required to generate secrets.' >&2; exit 1; }
    set_missing_value POSTGRES_DB story
    set_missing_value POSTGRES_USER story_owner
    set_missing_value POSTGRES_PASSWORD "$(random_secret)"
    set_missing_value STORY_MIGRATION_DB_PASSWORD "$(random_secret)"
    set_missing_value STORY_AUTH_DB_PASSWORD "$(random_secret)"
    set_missing_value STORY_BACK_DB_PASSWORD "$(random_secret)"
    set_missing_value STORY_ADMIN_DB_PASSWORD "$(random_secret)"
    set_missing_value MINIO_ROOT_USER story_minio
    set_missing_value MINIO_ROOT_PASSWORD "$(random_secret)"
    set_missing_value MINIO_MC_BUCKET story-works
    set_missing_value STORY_JWT_SECRET "$(random_secret 48)"
    echo 'Filled only missing generated values. Existing values and secrets were preserved.'
    echo 'Set STORY_CORS_ALLOWED_ORIGINS and review cookie/HTTPS settings, then run check-config.'
    ;;
  check-config)
    check_config
    compose config --quiet
    echo 'Configuration is complete and Docker Compose accepted it.'
    ;;
  status)
    check_config
    compose ps
    ;;
  logs)
    check_config
    log_args=(logs --tail "$tail_lines")
    [[ "$follow" == "true" ]] && log_args+=(--follow)
    [[ -n "$service" ]] && log_args+=("$service")
    compose "${log_args[@]}"
    ;;
  restart)
    [[ -n "$service" ]] || { echo 'restart requires a service name.' >&2; exit 2; }
    check_config
    compose restart "$service"
    wait_service_ready "$service"
    ;;
  update)
    [[ -n "$service" ]] || { echo 'update requires a service name.' >&2; exit 2; }
    check_config
    old_container_id="$(compose ps --quiet "$service" | head -n 1)"
    old_image_name=''
    rollback_image=''
    if [[ -n "$old_container_id" ]]; then
      old_image_name="$(docker_cmd inspect --format '{{.Config.Image}}' "$old_container_id")"
      rollback_image="story-rollback-$service:$(date -u +%Y%m%d%H%M%S)"
      docker_cmd image tag "$old_image_name" "$rollback_image"
    fi
    if ! compose build --pull "$service" ||
       ! compose up --detach --no-deps "$service" ||
       ! wait_service_ready "$service"; then
      if [[ -n "$rollback_image" && -n "$old_image_name" ]]; then
        echo "Update failed. Restoring the previous image for $service." >&2
        docker_cmd image tag "$rollback_image" "$old_image_name"
        compose up --detach --no-deps "$service"
        wait_service_ready "$service"
      fi
      exit 1
    fi
    ;;
esac
