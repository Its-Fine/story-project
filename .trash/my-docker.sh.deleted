Set-Location "$PSScriptRoot\.."
docker-compose.exe --env-file "environments/$($args[0]).env" $args[1..($args.Count - 1)]
#!/usr/bin/bash

cd "$(dirname "$(realpath $0)")/.."
sudo docker compose --env-file ./environments/"$1".env ${@:2}
