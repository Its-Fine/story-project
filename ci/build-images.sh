#!/usr/bin/env bash
set -Eeuo pipefail

repository="${1:-}"
tag="${2:-}"
mode="${3:-}"
source_repository="${4:-}"

if [[ -z "$repository" || -z "$tag" ]]; then
  echo "Usage: $0 <repository> <commit-tag> [--push-from <local-repository>]" >&2
  exit 2
fi

services=(story-auth story-back story-admin frontend migrations)

if [[ "$mode" == "--push-from" ]]; then
  [[ -n "$source_repository" ]] || { echo '--push-from requires a local repository.' >&2; exit 2; }
  for service in "${services[@]}"; do
    docker image tag "$source_repository/$service:$tag" "$repository/$service:$tag"
    docker image push "$repository/$service:$tag"
  done
  exit 0
fi

docker build --pull -f story-auth/Dockerfile -t "$repository/story-auth:$tag" .
docker build --pull -f story-back/Dockerfile -t "$repository/story-back:$tag" .
docker build --pull -f story-admin/Dockerfile -t "$repository/story-admin:$tag" .
docker build --pull -f Story-Front/Dockerfile -t "$repository/frontend:$tag" Story-Front
docker build --pull -f Story-Data/Dockerfile.migrations -t "$repository/migrations:$tag" .
