@echo off
rem format.bat - auto-format the codebase with Spotless (ktlint under the hood).
rem Usage: scripts\format.bat
setlocal
cd /d "%~dp0.."
echo ==^> Running spotlessApply
call gradlew.bat spotlessApply %*
echo ==^> Done. Review and commit the changes.
endlocal
