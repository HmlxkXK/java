@echo off
chcp 65001 >nul
title reigeki-anime - Vue3 Vite Frontend (5173)
cd /d "%~dp0"

echo ================================================
echo  reigeki-anime frontend - Vue 3 + Vite
echo  Port: 5173   API proxied to 127.0.0.1:8081
echo ================================================
echo.

if not exist node_modules (
    echo First run: installing dependencies...
    call npm install
    echo.
)

call npm run dev
pause
