#!/bin/bash
# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# install_shortcut.sh
#
# KinoNode Shortcut Installer for Linux
# This script generates a .desktop file to provide application menu
# and desktop shortcuts for KinoNode.
# Define the installation directory paths
INSTALL_DIR="$HOME/.kinonode-app"
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
RELEASE_DIR_NAME="$(basename "$SCRIPT_DIR")"
INSTALLED_DIR="$INSTALL_DIR/$RELEASE_DIR_NAME"

# Define paths to the application resources
ICON_PATH="$INSTALLED_DIR/logo.png"
EXEC_PATH="$INSTALLED_DIR/bin/KinoNode"

# Define destination paths for the shortcuts
MENU_DESKTOP_FILE="$HOME/.local/share/applications/KinoNode.desktop"
# Safely detect the user's localized Desktop directory
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
DESKTOP_DESKTOP_FILE="$DESKTOP_DIR/KinoNode.desktop"

# Desktop file template definition
DESKTOP_CONTENT="[Desktop Entry]
Name=KinoNode
Comment=Personal Movie Library Manager
Exec=$EXEC_PATH
Icon=$ICON_PATH
Terminal=false
Type=Application
Categories=Utility;AudioVideo;"

# 1. Application Menu Shortcut Creation
mkdir -p "$HOME/.local/share/applications"
echo "$DESKTOP_CONTENT" > "$MENU_DESKTOP_FILE"
chmod +x "$MENU_DESKTOP_FILE"

# Refresh the desktop database to make the new menu entry visible immediately
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null
echo "Added KinoNode to application menu."

# 2. Desktop Icon Creation
if [ -d "$DESKTOP_DIR" ]; then
    echo "$DESKTOP_CONTENT" > "$DESKTOP_DESKTOP_FILE"
    chmod +x "$DESKTOP_DESKTOP_FILE"
    
    # Mark the shortcut as trusted so modern desktop environments display it correctly
    gio set "$DESKTOP_DESKTOP_FILE" "metadata::trusted" true 2>/dev/null
    
    echo "Added KinoNode desktop shortcut."
fi
