@echo off
chcp 65001 >nul
title GalSpace Engine

cd /d "%~dp0"

netstat -ano | findstr ":10081 " >nul
if %ERRORLEVEL% EQU 0 (
    echo GalSpace is already running...
    start http://localhost:10081
    timeout /t 2 >nul
    exit /b 0
)

set JAVA_EXE=java

if exist "jre\bin\java.exe" (
    set "JAVA_EXE=%~dp0jre\bin\java.exe"
    echo Using built-in JRE...
    goto CHECK_JAR
)

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [Error] Java environment not found! Please install Java 17+.
    pause
    exit /b 1
)
echo Using System Java...

:CHECK_JAR
set JAR_FILE=GalSpace-1.0.0-SNAPSHOT.jar
if exist "%JAR_FILE%" goto RUN_SERVER

set JAR_FILE=target\GalSpace-1.0.0-SNAPSHOT.jar
if exist "%JAR_FILE%" goto RUN_SERVER

echo [Error] Core JAR file not found!
pause
exit /b 1

:RUN_SERVER
echo Starting engine, browser will open shortly...
start /b cmd /c "timeout /t 4 /nobreak >nul & start http://localhost:10081"

"%JAVA_EXE%" -jar "%JAR_FILE%"

echo Service stopped.
pause