@echo off
chcp 65001 >nul
title reigeki-anime - Start All

echo ================================================
echo  Starting backend (Spring Boot :8081) ...
echo ================================================
start "reigeki-backend" cmd /k "D:\workSpace\java\springBootTest\启动后端.bat"

echo ================================================
echo  Starting frontend (Vite :5173) ...
echo ================================================
start "reigeki-frontend" cmd /k "D:\workSpace\java\reigeki-anime\启动前端.bat"

echo.
echo Both launched in separate windows.
echo Frontend: http://localhost:5173
echo Backend : http://localhost:8081
timeout /t 5 >nul
