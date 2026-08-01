@echo off
rem check.bat - run the full local quality gate (mirrors CI).
rem Usage: scripts\check.bat
setlocal
cd /d "%~dp0.."

echo ==^> Spotless (formatting check)
call gradlew.bat spotlessCheck || exit /b 1

echo ==^> Detekt (static analysis)
call gradlew.bat detekt || exit /b 1

echo ==^> Unit tests
call gradlew.bat testDebugUnitTest || exit /b 1

echo ==^> All checks passed.
endlocal
