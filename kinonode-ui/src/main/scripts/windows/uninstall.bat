@echo off
:: BSD 3-Clause License
:: Copyright (c) 2026, Hannah Kullik
::
:: uninstall.bat
::
:: KinoNode Deep Uninstaller Wrapper for Windows
:: Invokes PowerShell uninstallation script with execution policy bypass.

setlocal enabledelayedexpansion
set "SCRIPT_DIR=%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%uninstall.ps1" %*
if %ERRORLEVEL% neq 0 (
    echo Uninstallation failed or was aborted.
    pause
    exit /b %ERRORLEVEL%
)

echo KinoNode cleanup completed.
pause
