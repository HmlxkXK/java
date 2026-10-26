@echo off
chcp 65001 >nul
title reigeki-anime - Spring Boot Backend (8081)
cd /d "%~dp0"

echo ================================================
echo  reigeki-anime backend - Spring Boot
echo  Port: 8081   Profile: dev
echo  MySQL required: localhost:3306 (root/123456)
echo ================================================
echo.

rem Clear SERVER__PORT: some host environments inject it and Spring Boot's
rem relaxed binding would override server.port=8081 with its value.
set "SERVER__PORT="

call mvnw.cmd spring-boot:run
pause
