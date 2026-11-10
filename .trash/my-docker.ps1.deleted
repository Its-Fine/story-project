@echo off
chcp 65001 >nul
echo ==========================================
echo Запуск системы Story (Auth + Back + DB + MinIO)
echo ==========================================

cd /d "%~dp0"

echo [1/2] Остановка старых контейнеров (если есть)...
docker-compose down

echo [2/2] Сборка и запуск всех сервисов...
docker-compose up --build -d

echo.
echo ==========================================
echo Готово! 
echo - story-auth доступен на: http://localhost:8001
echo - story-back доступен на: http://localhost:8000
echo - MinIO Console: http://localhost:9001 (minioadmin / minioadmin)
echo ==========================================
pauseSet-Location "$PSScriptRoot\.."
docker-compose.exe --env-file "environments/$($args[0]).env" $args[1..($args.Count - 1)]
