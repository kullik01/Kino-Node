#!/usr/bin/env bash
# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# uninstall.command - KinoNode Guaranteed 100% Deep Uninstaller (macOS)

set -euo pipefail

echo "================================================================="
echo "KinoNode Deep Uninstaller for macOS"
echo "This script will completely erase KinoNode application bundles,"
echo "user settings, database (kinonode.db), and cache files."
echo "================================================================="

read -rp "Proceed with complete removal? (y/N): " CONFIRM
if [[ "$CONFIRM" != [yY] && "$CONFIRM" != [yY][eE][sS] ]]; then
    echo "Uninstallation canceled."
    exit 0
fi

echo "Removing KinoNode..."

# 1. Terminate Application
osascript -e 'quit app "KinoNode"' 2>/dev/null || true
killall KinoNode 2>/dev/null || true

# 2. Remove Application Bundles (.app)
rm -rf "/Applications/KinoNode.app"
rm -rf "${HOME}/Applications/KinoNode.app"

# 3. Remove macOS Dock Icon (if dockutil is installed)
if command -v dockutil >/dev/null 2>&1; then
    dockutil --remove 'KinoNode' --allhomes 2>/dev/null || true
fi

# 4. Delete Application Support (SQLite DB & Data)
rm -rf "${HOME}/Library/Application Support/KinoNode"
rm -rf "${HOME}/Library/Application Support/io.github.kullik01.kinonode"
rm -rf "${HOME}/.kinonode"

# 5. Delete Preferences (.plist)
rm -f "${HOME}/Library/Preferences/io.github.kullik01.kinonode.plist"
rm -f "${HOME}/Library/Preferences/io.github.kullik01.kinonode.LSSharedFileList.plist"

# 6. Delete Caches, Logs & Saved Application State
rm -rf "${HOME}/Library/Caches/io.github.kullik01.kinonode"
rm -rf "${HOME}/Library/Caches/KinoNode"
rm -rf "${HOME}/Library/Logs/KinoNode"
rm -rf "${HOME}/Library/Logs/io.github.kullik01.kinonode"
rm -rf "${HOME}/Library/Saved Application State/io.github.kullik01.kinonode.savedState"

# 7. Flush macOS CFPreferences Daemon Cache
killall cfprefsd 2>/dev/null || true

echo "SUCCESS: KinoNode has been 100% removed from macOS."
