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
ICON_TARGET_DIR="${HOME}/.local/share/icons/hicolor/256x256/apps"
ICON_PATH="${ICON_TARGET_DIR}/kinonode.png"
MENU_DESKTOP_FILE="${HOME}/.local/share/applications/KinoNode.desktop"

# Resolve Executable Path
if [ -f "${SCRIPT_DIR}/bin/KinoNode" ]; then
    EXEC_PATH="${SCRIPT_DIR}/bin/KinoNode"
elif [ -f "${HOME}/.kinonode-app/KinoNode/bin/KinoNode" ]; then
    EXEC_PATH="${HOME}/.kinonode-app/KinoNode/bin/KinoNode"
elif [ -f "/opt/KinoNode/bin/KinoNode" ]; then
    EXEC_PATH="/opt/KinoNode/bin/KinoNode"
else
    EXEC_PATH="${SCRIPT_DIR}/KinoNode"
fi

# Resolve Icon Source Image
if [ -f "${SCRIPT_DIR}/logo.png" ]; then
    ICON_SRC="${SCRIPT_DIR}/logo.png"
elif [ -f "${HOME}/.kinonode-app/KinoNode/logo.png" ]; then
    ICON_SRC="${HOME}/.kinonode-app/KinoNode/logo.png"
else
    ICON_SRC=""
fi

# Resolve user's localized Desktop directory
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "${HOME}/Desktop")"
DESKTOP_DESKTOP_FILE="${DESKTOP_DIR}/KinoNode.desktop"

echo "Configuring KinoNode Linux desktop integration..."

# Ensure target directories exist
mkdir -p "${HOME}/.local/share/applications"
mkdir -p "${ICON_TARGET_DIR}"

# Install application icon into FreeDesktop hicolor theme path
if [ -n "${ICON_SRC}" ] && [ -f "${ICON_SRC}" ]; then
    cp "${ICON_SRC}" "${ICON_PATH}"
fi

# FreeDesktop .desktop specification with absolute Icon path and StartupWMClass
DESKTOP_CONTENT="[Desktop Entry]
Version=1.0
Type=Application
Name=KinoNode
GenericName=Movie Library Manager
Comment=Personal Movie Library Manager & Media Center
Exec=${EXEC_PATH}
Icon=${ICON_PATH}
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
