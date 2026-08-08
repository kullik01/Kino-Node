@echo off
:: BSD 3-Clause License
:: Copyright (c) 2026, Hannah Kullik
::
:: install_shortcut.bat
::
:: KinoNode Desktop & Start Menu Shortcut Installer for Windows

setlocal enabledelayedexpansion
set "SCRIPT_DIR=%~dp0"

echo Creating KinoNode Desktop and Start Menu shortcuts...

powershell -NoProfile -ExecutionPolicy Bypass -Command "^
$scriptDir = '%SCRIPT_DIR%'.TrimEnd('\'); ^
$execPath = Join-Path $scriptDir 'KinoNode.exe'; ^
if (-not (Test-Path $execPath)) { $execPath = Join-Path (Join-Path $scriptDir 'bin') 'KinoNode.exe'; } ^
if (-not (Test-Path $execPath)) { $execPath = Join-Path $scriptDir 'KinoNode.bat'; } ^
$wsh = New-Object -ComObject WScript.Shell; ^
$desktopPath = [System.IO.Path]::Combine([Environment]::GetFolderPath('Desktop'), 'KinoNode.lnk'); ^
$shortcut = $wsh.CreateShortcut($desktopPath); ^
$shortcut.TargetPath = $execPath; ^
$shortcut.WorkingDirectory = [System.IO.Path]::GetDirectoryName($execPath); ^
$shortcut.Description = 'KinoNode - Personal Movie Library Manager'; ^
$icoPath = Join-Path $scriptDir 'logo.ico'; ^
if (Test-Path $icoPath) { $shortcut.IconLocation = '$icoPath,0'; } ^
$shortcut.Save(); ^
Write-Host 'Created Desktop shortcut:' $desktopPath; ^
$startMenu = [System.IO.Path]::Combine([Environment]::GetFolderPath('StartMenu'), 'Programs', 'KinoNode.lnk'); ^
$shortcut2 = $wsh.CreateShortcut($startMenu); ^
$shortcut2.TargetPath = $execPath; ^
$shortcut2.WorkingDirectory = [System.IO.Path]::GetDirectoryName($execPath); ^
$shortcut2.Description = 'KinoNode - Personal Movie Library Manager'; ^
if (Test-Path $icoPath) { $shortcut2.IconLocation = '$icoPath,0'; } ^
$shortcut2.Save(); ^
Write-Host 'Created Start Menu shortcut:' $startMenu; ^
"

if %ERRORLEVEL% neq 0 (
    echo Shortcut creation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo Windows desktop integration complete!
pause
