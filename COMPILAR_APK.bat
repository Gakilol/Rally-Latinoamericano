@echo off
setlocal
title Nexo SOS - Compilador Automatico de APK Android (San Rafael del Sur)
echo ============================================================================
echo   NEXO SOS - COMPILADOR DE APK ANDROID (SAN RAFAEL DEL SUR)
echo ============================================================================

set "TC=%LOCALAPPDATA%\NexoAndroidToolchain"
set "JAVA_HOME=%TC%\jdk-17"
set "ANDROID_HOME=%TC%\android-sdk"
set "ANDROID_SDK_ROOT=%TC%\android-sdk"
set "GRADLE_BIN=%TC%\gradle-8.7\bin\gradle.bat"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%"

echo [1/3] Verificando JDK 17 y Android SDK (API 34)...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: No se encontro JDK 17 en %JAVA_HOME%.
    pause
    exit /b 1
)

echo [2/3] Compilando modulos :domain, :protocol, :data-local, :transport-mesh, :transport-ip y :app...
call "%GRADLE_BIN%" assembleDebug --no-daemon

if exist "%~dp0app\build\outputs\apk\debug\app-debug.apk" (
    copy /Y "%~dp0app\build\outputs\apk\debug\app-debug.apk" "%~dp0NexoSOS-SanRafaelDelSur.apk" >nul
    echo ============================================================================
    echo   APK GENERADO EXITOSAMENTE:
    echo   %~dp0NexoSOS-SanRafaelDelSur.apk
    echo ============================================================================
) else (
    echo Hubo un problema durante la compilacion del APK.
)
pause
