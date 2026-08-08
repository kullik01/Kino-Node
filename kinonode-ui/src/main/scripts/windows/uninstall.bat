@echo off
:: BSD 3-Clause License
:: Copyright (c) 2026, Hannah Kullik
::
:: uninstall.bat
::
:: KinoNode Deep Uninstaller for Windows
:: Removes application shortcuts and all associated user data.
echo Warning: This will completely remove KinoNode, including your movie database (kinonode.db) and all cached posters.
set /p CONFIRM="Are you sure you want to proceed? (Y/N): "
if /I "%CONFIRM%" neq "Y" (
    echo Uninstallation aborted.
    exit /b 1
)

:: 1. Remove Desktop Shortcut
del /F /Q "%USERPROFILE%\Desktop\KinoNode.lnk" 2>nul
del /F /Q "%PUBLIC%\Desktop\KinoNode.lnk" 2>nul

:: 2. Remove Start Menu Shortcut
del /F /Q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\KinoNode.lnk" 2>nul

:: 3. Remove AppData (Database & Settings)
:: Assumes AppData is located in %LOCALAPPDATA%\KinoNode
rmdir /S /Q "%LOCALAPPDATA%\KinoNode" 2>nul
rmdir /S /Q "%APPDATA%\KinoNode" 2>nul

:: 4. Remove App Binaries (Self-deleting script trick)
:: (Requires the script to run outside the directory it is deleting)
echo KinoNode user data removed. Please manually delete the application folder.
pause
