@echo off
setlocal enabledelayedexpansion
title Vedic Mitra - Release

REM =====================================================================
REM  Vedic Mitra interactive release script
REM
REM  The flow, in one sentence: update CHANGELOG.md and write
REM  docs/releases/<version>.md, leave them uncommitted, run this.
REM
REM  Steps: preflight -> bump version -> ONE commit + push -> tag/push ->
REM         build APK -> create/update the GitHub release with the APK.
REM
REM  The release lands as a single commit containing the version bump,
REM  the closed changelog and the release note. They describe one event,
REM  so they belong in one commit -- and it is the commit the tag points
REM  at, which is what makes the release reproducible from the tag.
REM
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
if not exist "CHANGELOG.md"       (echo [ERROR] CHANGELOG.md not found. & goto :fail)

REM ---- Preflight: on main, and level with the remote ------------------
REM  0.10.0 was tagged and built from a feature branch that happened to be
REM  checked out. The tag and the APK were fine; main got nothing, because
REM  `git push origin main` pushed a main ref that had never moved. The
REM  script had no idea which branch it was standing on. Now it does.
set "BRANCH="
for /f "delims=" %%a in ('git rev-parse --abbrev-ref HEAD') do set "BRANCH=%%a"
REM  This file must stay CRLF - .gitattributes enforces it. Saved with LF,
REM  cmd mis-parses every multi-line ( ) block: the echoes still run but a
REM  goto inside one silently does not, so a guard prints its error and then
REM  carries on releasing anyway. Found the hard way while testing this guard.
if /i not "!BRANCH!"=="main" (
  echo [ERROR] On branch "!BRANCH!" - releases are cut from main.
  echo         Run: git checkout main, then git pull, then this script again.
  goto :fail
)

echo Fetching origin ...
git fetch --quiet origin main || (echo [ERROR] Could not fetch origin. & goto :fail)
set "LOCAL_SHA="
set "REMOTE_SHA="
for /f "delims=" %%a in ('git rev-parse HEAD') do set "LOCAL_SHA=%%a"
for /f "delims=" %%a in ('git rev-parse origin/main') do set "REMOTE_SHA=%%a"
if not "!LOCAL_SHA!"=="!REMOTE_SHA!" (
  echo [ERROR] main is not level with origin/main.
  echo           local  !LOCAL_SHA!
  echo           remote !REMOTE_SHA!
  echo         Push or pull first - a release must be cut from what everyone else can see.
  goto :fail
)
echo [OK] On main, level with origin.
echo.

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
set "NOTES=docs\releases\!NEW_NAME!.md"

REM ---- Preflight: the release documents must already exist -----------
REM  Both are written before the script runs. Checking here means a
REM  half-prepared release stops now, rather than after it has been
REM  tagged and pushed.
if not exist "!NOTES!" (
  echo [ERROR] No release note at !NOTES!
  echo         Write it first - it becomes the body of the GitHub release.
  goto :fail
)
findstr /c:"## [!NEW_NAME!]" CHANGELOG.md >nul 2>&1
if errorlevel 1 (
  echo [ERROR] CHANGELOG.md has no "## [!NEW_NAME!]" heading.
  echo         Close the Unreleased section on !NEW_NAME! first.
  goto :fail
)
echo [OK] Release note and changelog heading both present.

echo.
echo   New version : !NEW_NAME!  (code !NEW_CODE!)
echo   Tag         : !TAG!
echo   APK asset   : !APK_OUT!
echo   Notes       : !NOTES!
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

REM ---- 2. One commit, one push --------------------------------------
REM  Exactly three paths are staged. Anything else you have in progress is
REM  listed and left alone -- a release commit should carry the release and
REM  nothing that happened to be open at the time.
echo [2/6] Staging the release ...
git add version.properties CHANGELOG.md "!NOTES!" || goto :fail
echo.
echo   Staged for the release commit:
git diff --cached --name-only
echo.
git diff --quiet
if errorlevel 1 (
  echo   [WARN] Also modified, and NOT part of this release:
  git diff --name-only
  echo.
)
git diff --cached --quiet && (
  echo [ERROR] Nothing staged - version.properties, CHANGELOG.md and the note are all unchanged.
  echo         Did you mean to prepare the release first?
  goto :fail
)
set /p "docommit=Commit as 'chore(release): !NEW_NAME!' and push to main? (y/N): "
if /i not "!docommit!"=="y" (
  echo [ABORT] Not committing.
  echo         Stopping here on purpose: tagging a commit that is not on main is
  echo         how 0.10.0 ended up with a tag no branch contained.
  goto :fail
)
git commit -m "chore(release): !NEW_NAME!" || goto :fail
git push origin main || goto :fail

REM ---- Guard: the commit really is on origin/main --------------------
git fetch --quiet origin main || (echo [ERROR] Could not fetch origin. & goto :fail)
set "LOCAL_SHA="
set "REMOTE_SHA="
for /f "delims=" %%a in ('git rev-parse HEAD') do set "LOCAL_SHA=%%a"
for /f "delims=" %%a in ('git rev-parse origin/main') do set "REMOTE_SHA=%%a"
if not "!LOCAL_SHA!"=="!REMOTE_SHA!" (
  echo [ERROR] The release commit is not on origin/main. Refusing to tag it.
  goto :fail
)
echo [OK] Release commit is on origin/main: !LOCAL_SHA!

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
REM  Defaults to the note checked above; overridable, but there is rarely
REM  a reason to point the release body at anything else.
echo.
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
