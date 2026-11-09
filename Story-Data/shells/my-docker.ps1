Set-Location "$PSScriptRoot\.."
docker-compose.exe --env-file "environments/$($args[0]).env" $args[1..($args.Count - 1)]
