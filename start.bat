@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
set "DATA_DIR=%PROJECT_ROOT%Story-Data"
set "ENV_FILE=%DATA_DIR%\.env"
set "STORYCTL=%DATA_DIR%\tools\storyctl.ps1"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker Desktop is not installed or docker.exe is not available in PATH.
  exit /b 1
)

if not exist "%ENV_FILE%" (
  echo Creating a local environment file with generated secrets...
  powershell -NoProfile -ExecutionPolicy Bypass -File "%STORYCTL%" init-env
  if errorlevel 1 exit /b 1
  echo.
  echo Edit "%ENV_FILE%" before starting:
  echo   STORY_COOKIE_SECURE=false
  echo   STORY_COOKIE_SAME_SITE=Lax
  echo   STORY_CORS_ALLOWED_ORIGINS=http://localhost:3000
  echo Then run start.bat again.
  exit /b 0
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%STORYCTL%" check-config
if errorlevel 1 exit /b 1

pushd "%DATA_DIR%"
docker compose --env-file .env --file docker-compose.yml up --detach --build
set "RESULT=%ERRORLEVEL%"
popd

if not "%RESULT%"=="0" exit /b %RESULT%
echo Story Project containers were started. Use storyctl.ps1 status to inspect them.
exit /b 0
