@echo off
title Batalha Naval - Servidor
set JAVA_BIN=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin
set OUT=%~dp0out

if not exist "%OUT%\batalhanaval\server\Servidor.class" (
    echo ERRO: Projeto nao compilado. Executa primeiro compilar.bat
    pause
    exit /b 1
)

if not exist "%~dp0saves" mkdir "%~dp0saves"

"%JAVA_BIN%\java.exe" -cp "%OUT%" batalhanaval.server.Servidor
pause
