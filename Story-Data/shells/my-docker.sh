#!/usr/bin/bash

cd "$(dirname "$(realpath $0)")/.."
sudo docker compose --env-file ./environments/"$1".env ${@:2}
