@echo off
title Batalha Naval - Compilar
echo ================================================
echo  Batalha Naval - Compilar
echo ================================================
echo.

set JAVA_BIN=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin
set SRC=%~dp0src\main\java
set OUT=%~dp0out

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo A compilar...

dir /s /b "%SRC%\*.java" | findstr /v "Cliente.java" > "%TEMP%\bn_sources.txt"

"%JAVA_BIN%\javac.exe" -encoding UTF-8 -d "%OUT%" @"%TEMP%\bn_sources.txt"

if %ERRORLEVEL% == 0 (
    echo.
    echo  OK - Compilacao concluida com sucesso!
) else (
    echo.
    echo  ERRO - Falha na compilacao!
)
echo.
pause
