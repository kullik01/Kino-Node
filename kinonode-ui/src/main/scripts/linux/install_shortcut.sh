#!/usr/bin/env bash
# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# install_shortcut.sh
#
# KinoNode Desktop Shortcut & Integration Script for Linux

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$(realpath "$0")")" && pwd)"
INSTALL_DIR="${HOME}/.kinonode-app/KinoNode"
EXEC_PATH="${INSTALL_DIR}/bin/KinoNode"
ICON_TARGET_DIR="${HOME}/.local/share/icons/hicolor/256x256/apps"
ICON_PATH="${ICON_TARGET_DIR}/kinonode.png"
MENU_DESKTOP_FILE="${HOME}/.local/share/applications/KinoNode.desktop"

# Resolve user's localized Desktop directory
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "${HOME}/Desktop")"
DESKTOP_DESKTOP_FILE="${DESKTOP_DIR}/KinoNode.desktop"

echo "Configuring KinoNode Linux desktop integration..."

# Ensure target directories exist
mkdir -p "${HOME}/.local/share/applications"
mkdir -p "${ICON_TARGET_DIR}"

# Install application icon into FreeDesktop hicolor theme path
if [ -f "${SCRIPT_DIR}/logo.png" ]; then
    cp "${SCRIPT_DIR}/logo.png" "${ICON_PATH}"
fi

# FreeDesktop .desktop specification with StartupWMClass to prevent fallback Java icon
DESKTOP_CONTENT="[Desktop Entry]
Version=1.0
Type=Application
Name=KinoNode
GenericName=Movie Library Manager
Comment=Personal Movie Library Manager & Media Center
Exec=${EXEC_PATH}
Icon=kinonode
Terminal=false
Categories=Utility;AudioVideo;Video;Player;Java;
StartupWMClass=io-github-kullik01-kinonode-Launcher
MimeType=video/mp4;video/x-matroska;video/webm;
Keywords=Movie;Film;Media;Library;Watchlist;"

# 1. Install Application Menu Shortcut
echo "${DESKTOP_CONTENT}" > "${MENU_DESKTOP_FILE}"
chmod +x "${MENU_DESKTOP_FILE}"

# 2. Install User Desktop Shortcut (if Desktop folder exists)
if [ -d "${DESKTOP_DIR}" ]; then
    echo "${DESKTOP_CONTENT}" > "${DESKTOP_DESKTOP_FILE}"
    chmod +x "${DESKTOP_DESKTOP_FILE}"
    
    # Modern Desktop Environment Trust & Metadata Assignment
    if command -v gio >/dev/null 2>&1; then
        gio set "${DESKTOP_DESKTOP_FILE}" "metadata::trusted" true 2>/dev/null || true
    fi

    if command -v kwriteconfig5 >/dev/null 2>&1; then
        kwriteconfig5 --file "${DESKTOP_DESKTOP_FILE}" --group "Desktop Entry" --key "X-KDE-SubstituteUID" "false" 2>/dev/null || true
    elif command -v kwriteconfig6 >/dev/null 2>&1; then
        kwriteconfig6 --file "${DESKTOP_DESKTOP_FILE}" --group "Desktop Entry" --key "X-KDE-SubstituteUID" "false" 2>/dev/null || true
    fi
    
    echo "Added desktop shortcut: ${DESKTOP_DESKTOP_FILE}"
fi

# 3. Flush Desktop & Icon Databases
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "${HOME}/.local/share/applications" 2>/dev/null || true
fi

if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "${HOME}/.local/share/icons/hicolor" 2>/dev/null || true
fi

touch --no-create "${HOME}/.local/share/icons/hicolor" 2>/dev/null || true

echo "KinoNode desktop integration complete."
