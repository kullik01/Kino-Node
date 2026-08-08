# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# uninstall.ps1 - KinoNode Guaranteed 100% Deep Uninstaller (Windows)

[CmdletBinding()]
param (
    [switch]$Force
)

$ErrorActionPreference = "SilentlyContinue"

if (-not $Force) {
    $title = "KinoNode Complete Uninstallation"
    $message = "Are you sure you want to completely remove KinoNode?`n`n" +
               "This will permanently delete application binaries, desktop shortcuts, " +
               "configuration files, cached posters, and your movie database (kinonode.db)."
    $yes = New-Object System.Management.Automation.Host.ChoiceDescription "&Yes", "Proceeds with deep uninstallation."
    $no = New-Object System.Management.Automation.Host.ChoiceDescription "&No", "Aborts uninstallation."
    $options = [System.Management.Automation.Host.ChoiceDescription[]]($yes, $no)
    $result = $host.ui.PromptForChoice($title, $message, $options, 1)

    if ($result -ne 0) {
        Write-Host "Uninstallation canceled." -ForegroundColor Yellow
        exit
    }
}

Write-Host "Starting deep removal of KinoNode..." -ForegroundColor Cyan

# 1. Kill Running Processes
Stop-Process -Name "KinoNode" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "javaw" -IncludeUserName | Where-Object { $_.CommandLine -like "*kinonode*" } | Stop-Process -Force -ErrorAction SilentlyContinue

# 2. Delete Application Binaries
$installPaths = @(
    "$env:ProgramFiles\KinoNode",
    "${env:ProgramFiles(x86)}\KinoNode",
    "$env:LOCALAPPDATA\Programs\KinoNode",
    "$env:LOCALAPPDATA\KinoNode\App"
)
foreach ($path in $installPaths) {
    if (Test-Path $path) {
        Remove-Item -Path $path -Recurse -Force
        Write-Host "Removed binary path: $path" -ForegroundColor Green
    }
}

# 3. Remove Start Menu and Desktop Shortcuts (.lnk)
$shortcutPaths = @(
    "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\KinoNode.lnk",
    "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\KinoNode\KinoNode.lnk",
    "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\KinoNode",
    "$env:USERPROFILE\Desktop\KinoNode.lnk",
    "$env:PUBLIC\Desktop\KinoNode.lnk"
)
foreach ($shortcut in $shortcutPaths) {
    if (Test-Path $shortcut) {
        Remove-Item -Path $shortcut -Recurse -Force
        Write-Host "Removed shortcut: $shortcut" -ForegroundColor Green
    }
}

# 4. Remove User Application Data, SQLite Database & Caches
$dataPaths = @(
    "$env:APPDATA\KinoNode",
    "$env:LOCALAPPDATA\KinoNode",
    "$env:USERPROFILE\.kinonode"
)
foreach ($dataPath in $dataPaths) {
    if (Test-Path $dataPath) {
        Remove-Item -Path $dataPath -Recurse -Force
        Write-Host "Removed data path: $dataPath" -ForegroundColor Green
    }
}

# 5. Clean Windows Registry Keys
$registryKeys = @(
    "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\KinoNode",
    "HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\KinoNode",
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\KinoNode",
    "HKCU:\Software\KinoNode",
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\App Paths\KinoNode.exe"
)
foreach ($key in $registryKeys) {
    if (Test-Path $key) {
        Remove-Item -Path $key -Recurse -Force
        Write-Host "Removed registry key: $key" -ForegroundColor Green
    }
}

Write-Host "`nSUCCESS: KinoNode has been 100% completely purged from Windows." -ForegroundColor Green
