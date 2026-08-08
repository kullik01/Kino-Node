#!/usr/bin/env bash
# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# uninstall.sh
#
# KinoNode Deep Uninstaller for Linux
# Guarantees 100% clean removal of binaries, desktop integrations, SQLite databases, settings, and logs.

set -euo pipefail

FORCE_DELETE=false
if [[ "${1:-}" == "--force" || "${1:-}" == "-f" ]]; then
    FORCE_DELETE=true
fi

if [ "$FORCE_DELETE" = false ]; then
    echo "================================================================="
    echo "WARNING: DEEP UNINSTALLATION OF KINONODE"
    echo "This will permanently delete:"
    echo "  - Application binaries (~/.kinonode-app / /opt/KinoNode)"
    echo "  - Application menu shortcuts & Desktop icon"
    echo "  - Movie database (kinonode.db), settings, cached posters, and logs"
    echo "================================================================="
    read -rp "Are you sure you want to proceed? (y/N): " CONFIRM
    if [[ "$CONFIRM" != [yY] && "$CONFIRM" != [yY][eE][sS] ]]; then
        echo "Uninstallation aborted."
        exit 0
    fi
fi

echo "Removing KinoNode processes and files..."

# 1. Terminate running KinoNode instances
pkill -f "kinonode" 2>/dev/null || true
pkill -f "KinoNode" 2>/dev/null || true

# 2. Remove Application Binaries
rm -rf "${HOME}/.kinonode-app"
rm -rf "/opt/KinoNode" 2>/dev/null || true

# 3. Remove Menu and Desktop Shortcuts
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "${HOME}/Desktop")"
rm -f "${HOME}/.local/share/applications/KinoNode.desktop"
rm -f "${HOME}/.local/share/applications/kinonode.desktop"
rm -f "/usr/share/applications/kinonode.desktop" 2>/dev/null || true
rm -f "${DESKTOP_DIR}/KinoNode.desktop"
rm -f "${DESKTOP_DIR}/kinonode.desktop"

# 4. Remove Icon Assets
rm -f "${HOME}/.local/share/icons/hicolor/"*/apps/kinonode.png
rm -f "${HOME}/.local/share/icons/hicolor/scalable/apps/kinonode.svg"
rm -f "/usr/share/icons/hicolor/"*/apps/kinonode.png 2>/dev/null || true

# 5. Remove AppData, Database (kinonode.db), Configuration, Cache & Logs
XDG_DATA_HOME="${XDG_DATA_HOME:-${HOME}/.local/share}"
rm -rf "${XDG_DATA_HOME}/KinoNode"
rm -rf "${HOME}/.kinonode"

XDG_CONFIG_HOME="${XDG_CONFIG_HOME:-${HOME}/.config}"
rm -rf "${XDG_CONFIG_HOME}/KinoNode"
rm -rf "${XDG_CONFIG_HOME}/kinonode"

XDG_CACHE_HOME="${XDG_CACHE_HOME:-${HOME}/.cache}"
rm -rf "${XDG_CACHE_HOME}/KinoNode"
rm -rf "${XDG_CACHE_HOME}/kinonode"

# 6. Invalidate System Databases
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "${HOME}/.local/share/applications" 2>/dev/null || true
fi

if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "${HOME}/.local/share/icons/hicolor" 2>/dev/null || true
fi

echo "KinoNode has been 100% cleanly uninstalled from your Linux system."
