@echo off
setlocal enabledelayedexpansion
title Vedic Mitra - Release

REM =====================================================================
REM  Vedic Mitra interactive release script
REM  Steps: bump version -> commit/push -> tag/push -> build APK ->
REM         create/update GitHub release and upload the APK.
REM  Every step asks before it does anything irreversible. Answer y/N.
REM =====================================================================

cd /d "%~dp0"
echo.
echo ============================================
echo   Vedic Mitra  -  Release
echo   Repo: %cd%
echo ============================================
echo.

REM ---- Preflight: required tools -------------------------------------
where git   >nul 2>&1 || (echo [ERROR] git not found on PATH.   & goto :fail)
where gh    >nul 2>&1 || (echo [ERROR] GitHub CLI ^(gh^) not found on PATH. & goto :fail)
if not exist "version.properties" (echo [ERROR] version.properties not found - run this from the repo root. & goto :fail)
if not exist "gradlew.bat"        (echo [ERROR] gradlew.bat not found. & goto :fail)

REM ---- Read current version -----------------------------------------
set "CUR_NAME="
set "CUR_CODE="
for /f "tokens=2 delims==" %%a in ('findstr /b /c:"VERSION_NAME=" version.properties') do set "CUR_NAME=%%a"
for /f "tokens=2 delims==" %%a in ('findstr /b /c:"VERSION_CODE=" version.properties') do set "CUR_CODE=%%a"
echo Current version : %CUR_NAME%  (code %CUR_CODE%)
echo.

REM ---- Prompt for the new version -----------------------------------
set /p "NEW_NAME=New version name (e.g. 0.3.0): "
if "!NEW_NAME!"=="" (echo [ABORT] No version name entered. & goto :fail)

set /a "SUGGEST_CODE=%CUR_CODE%+1"
set /p "NEW_CODE=New version code [!SUGGEST_CODE!]: "
if "!NEW_CODE!"=="" set "NEW_CODE=!SUGGEST_CODE!"

set "TAG=v!NEW_NAME!"
set "APK_OUT=vedic-mitra-!NEW_NAME!.apk"

echo.
echo   New version : !NEW_NAME!  (code !NEW_CODE!)
echo   Tag         : !TAG!
echo   APK asset   : !APK_OUT!
echo.
set /p "ok=Proceed with these values? (y/N): "
if /i not "!ok!"=="y" (echo [ABORT] Cancelled. & goto :fail)

REM ---- Guard: code must strictly increase ---------------------------
if !NEW_CODE! LEQ %CUR_CODE% (
  echo [WARN] New code !NEW_CODE! is not greater than current %CUR_CODE%.
  echo        Android rejects installs whose code is not higher than the installed build.
  set /p "codeok=Continue anyway? (y/N): "
  if /i not "!codeok!"=="y" goto :fail
)

REM ---- 1. Bump version.properties -----------------------------------
echo.
echo [1/6] Updating version.properties ...
powershell -NoProfile -Command "(Get-Content 'version.properties') -replace '^VERSION_NAME=.*','VERSION_NAME=!NEW_NAME!' -replace '^VERSION_CODE=.*','VERSION_CODE=!NEW_CODE!' | Set-Content 'version.properties'"
if errorlevel 1 (echo [ERROR] Failed to update version.properties. & goto :fail)
findstr /b /c:"VERSION_" version.properties
echo.

REM ---- 2. Commit + push main ----------------------------------------
set /p "docommit=[2/6] Commit the bump and push to main? (y/N): "
if /i "!docommit!"=="y" (
  git add version.properties        || goto :fail
  git diff --cached --quiet && (echo [SKIP] Nothing to commit - version.properties unchanged.) || (
    git commit -m "chore(release): bump version to !NEW_NAME!" || goto :fail
    git push origin main || goto :fail
  )
) else (
  echo [SKIP] Not committing. You can commit manually later.
)

REM ---- 3. Tag + push tag --------------------------------------------
echo.
git rev-parse "!TAG!" >nul 2>&1
if not errorlevel 1 (
  echo [3/6] Tag !TAG! already exists locally - skipping tag creation.
) else (
  set /p "dotag=[3/6] Create annotated tag !TAG! and push it? (y/N): "
  if /i "!dotag!"=="y" (
    git tag -a "!TAG!" -m "Vedic Mitra !NEW_NAME!" || goto :fail
    git push origin "!TAG!" || goto :fail
  ) else (
    echo [SKIP] Not tagging.
  )
)

REM ---- 4. Build the signed release APK ------------------------------
echo.
set /p "dobuild=[4/6] Build signed release APK (gradlew clean assembleRelease)? (y/N): "
if /i "!dobuild!"=="y" (
  call gradlew.bat clean assembleRelease
  if errorlevel 1 (echo [ERROR] Release build failed. & goto :fail)
  if not exist "app\build\outputs\apk\release\app-release.apk" (
    echo [ERROR] Expected APK not found at app\build\outputs\apk\release\app-release.apk
    goto :fail
  )
  copy /y "app\build\outputs\apk\release\app-release.apk" "!APK_OUT!" >nul
  echo [OK] APK ready: !APK_OUT!
) else (
  echo [SKIP] Not building. Expecting !APK_OUT! to already exist for upload.
)

REM ---- 5. Release notes ---------------------------------------------
echo.
set "NOTES=release-notes.md"
set /p "notesin=[5/6] Notes file [!NOTES!]: "
if not "!notesin!"=="" set "NOTES=!notesin!"
if not exist "!NOTES!" (
  echo [WARN] Notes file "!NOTES!" not found. The release will be created with an empty body.
  set "NOTES="
)

REM ---- 6. Create or update the GitHub release -----------------------
echo.
gh release view "!TAG!" >nul 2>&1
if errorlevel 1 (
  echo [6/6] Creating release !TAG! ...
  if defined NOTES (
    gh release create "!TAG!" "!APK_OUT!" --title "Vedic Mitra !NEW_NAME!" --notes-file "!NOTES!" || goto :fail
  ) else (
    gh release create "!TAG!" "!APK_OUT!" --title "Vedic Mitra !NEW_NAME!" --generate-notes || goto :fail
  )
) else (
  echo [6/6] Release !TAG! already exists - updating title/notes and uploading APK ...
  if defined NOTES gh release edit "!TAG!" --title "Vedic Mitra !NEW_NAME!" --notes-file "!NOTES!" || goto :fail
  if exist "!APK_OUT!" (
    gh release upload "!TAG!" "!APK_OUT!" --clobber || goto :fail
  ) else (
    echo [WARN] !APK_OUT! not found - skipping asset upload.
  )
)

echo.
echo ============================================
echo   Done. Release !TAG! is published.
echo ============================================
gh release view "!TAG!" --web
goto :end

:fail
echo.
echo *** Release aborted. Nothing further was done. ***
exit /b 1

:end
endlocal
pause
