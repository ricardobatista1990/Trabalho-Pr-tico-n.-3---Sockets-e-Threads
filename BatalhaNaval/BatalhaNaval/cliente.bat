@echo off
title Batalha Naval - Cliente
set JAVA_BIN=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin
set OUT=%~dp0out

if not exist "%OUT%\batalhanaval\client\ClienteGUI.class" (
    echo ERRO: Projeto nao compilado. Executa primeiro compilar.bat
    pause
    exit /b 1
)

"%JAVA_BIN%\java.exe" -cp "%OUT%" batalhanaval.client.ClienteGUI
