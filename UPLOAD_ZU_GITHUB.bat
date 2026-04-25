@echo off
chcp 65001 >nul
title TodoPro - Upload zu GitHub
color 0A

echo ============================================
echo   TodoPro - Automatischer GitHub Upload
echo ============================================
echo.

REM Prüfe ob Git installiert ist
where git >nul 2>&1
if %errorlevel% neq 0 (
    echo [FEHLER] Git ist nicht installiert!
    echo.
    echo Bitte installiere Git von: https://git-scm.com/download/win
    echo Nach der Installation dieses Script nochmal starten.
    echo.
    pause
    start https://git-scm.com/download/win
    exit /b 1
)

echo [OK] Git gefunden!
echo.

REM GitHub Benutzername abfragen
set /p GITHUB_USER="Gib deinen GitHub-Benutzernamen ein: "
if "%GITHUB_USER%"=="" (
    echo Kein Benutzername eingegeben!
    pause
    exit /b 1
)

REM Repository Name
set REPO_NAME=Todo-pro-android-app

echo.
echo [INFO] Dein Repository wird: https://github.com/%GITHUB_USER%/%REPO_NAME%
echo.
echo WICHTIG: Du musst zuerst das Repository auf GitHub erstellen!
echo.
echo 1. Gehe zu: https://github.com/new
echo 2. Repository Name: %REPO_NAME%
echo 3. Klicke "Create repository"
echo.
echo Druecke eine Taste wenn du das Repository erstellt hast...
pause >nul

echo.
echo [INFO] Starte Upload...
echo.

cd /d c:\Users\Benutze\Desktop\test

REM Git konfigurieren
git config user.email "todopro@github.com"
git config user.name "%GITHUB_USER%"

REM Alle Dateien hinzufügen
git add .

REM Commit erstellen
git commit -m "TodoPro App - Native Android mit Benachrichtigungen" 2>nul
if %errorlevel% neq 0 (
    echo [INFO] Keine neuen Änderungen oder Commit bereits vorhanden.
)

REM Remote setzen (falls noch nicht gesetzt)
git remote remove origin 2>nul
git remote add origin https://github.com/%GITHUB_USER%/%REPO_NAME%.git

REM Branch auf main setzen
git branch -M main

echo.
echo [INFO] Lade hoch zu GitHub...
echo [INFO] Ein Browser-Fenster öffnet sich für die Anmeldung.
echo.

REM Push
git push -u origin main

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo   ERFOLG! Code ist auf GitHub!
    echo ============================================
    echo.
    echo Dein Repository: https://github.com/%GITHUB_USER%/%REPO_NAME%
    echo.
    echo GitHub Actions baut jetzt automatisch die APK.
    echo Das dauert ca. 5-10 Minuten.
    echo.
    echo Danach APK herunterladen unter:
    echo https://github.com/%GITHUB_USER%/%REPO_NAME%/releases
    echo.
    start https://github.com/%GITHUB_USER%/%REPO_NAME%/actions
) else (
    echo.
    echo [FEHLER] Upload fehlgeschlagen!
    echo.
    echo Mögliche Lösung:
    echo 1. Gehe zu: https://github.com/settings/tokens/new
    echo 2. Note: TodoPro
    echo 3. Haken bei "repo" setzen
    echo 4. "Generate token" klicken
    echo 5. Token kopieren
    echo 6. Beim nächsten Versuch als Passwort eingeben
    echo.
)

pause
