#!/usr/bin/env bash
set -Eeuo pipefail

repository="${1:-}"
tag="${2:-}"
docker_host="${3:-}"
env_file="${4:-}"

if [[ -z "$repository" || -z "$tag" || -z "$docker_host" || -z "$env_file" ]]; then
  echo "Usage: $0 <repository> <image-tag> <ssh-docker-host> <env-file>" >&2
  exit 2
fi
[[ -f "$env_file" ]] || { echo "Environment file not found: $env_file" >&2; exit 2; }
[[ "$tag" =~ ^[a-zA-Z0-9_.-]+$ ]] || { echo 'Image tag contains unsupported characters.' >&2; exit 2; }
[[ "$docker_host" == ssh://* ]] || { echo 'Only Docker SSH endpoints are allowed.' >&2; exit 2; }

context_name="story-ci-${BUILD_TAG:-manual}-${RANDOM}"
context_name="${context_name//[^a-zA-Z0-9_.-]/-}"
export STORY_IMAGE_REPOSITORY="$repository"
export STORY_IMAGE_TAG="$tag"

cleanup() {
  docker context rm --force "$context_name" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker context create "$context_name" --docker "host=$docker_host" >/dev/null

compose=(docker --context "$context_name" compose
  --env-file "$env_file"
  --file Story-Data/docker-compose.yml
  --file Story-Data/docker-compose.release.yml)

"${compose[@]}" config --quiet
"${compose[@]}" pull story-migrations story-back story-auth story-admin frontend
"${compose[@]}" run --rm story-migrations

wait_ready() {
  local service="$1" container_id deadline state
  container_id="$("${compose[@]}" ps --quiet "$service" | head -n 1)"
  [[ -n "$container_id" ]] || { echo "No container was created for $service." >&2; return 1; }
  deadline=$((SECONDS + 120))
  while ((SECONDS < deadline)); do
    state="$(docker --context "$context_name" inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id")"
    if [[ "$state" == 'running healthy' ]]; then
      echo "$service is ready."
      return 0
    fi
    if [[ "$state" =~ ^(exited|dead|removing)\  || "$state" == 'running unhealthy' ]]; then
      echo "$service failed readiness: $state" >&2
      return 1
    fi
    sleep 3
  done
  echo "$service did not become healthy within 120 seconds." >&2
  return 1
}

for service in story-back story-auth story-admin frontend; do
  "${compose[@]}" up --detach --no-build --no-deps "$service"
  wait_ready "$service"
done

echo "Deployment completed with immutable image tag: $tag"
