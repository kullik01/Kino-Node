#!/bin/bash

# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# KinoNode Desktop Shortcut Installer
# Creates application menu and desktop shortcuts that point to the
# installed location at ~/.kinonode-app or current release folder.

INSTALL_DIR="$HOME/.kinonode-app"

# Determine the release directory name from this script's parent folder
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
RELEASE_DIR_NAME="$(basename "$SCRIPT_DIR")"

# Shortcuts prefer the installed copy, but fallback to script directory
INSTALLED_DIR="$INSTALL_DIR/$RELEASE_DIR_NAME"
if [ -d "$INSTALLED_DIR" ]; then
    TARGET_DIR="$INSTALLED_DIR"
else
    TARGET_DIR="$SCRIPT_DIR"
fi

ICON_PATH="$TARGET_DIR/logo.png"
EXEC_PATH="$TARGET_DIR/bin/KinoNode"
MENU_DESKTOP_FILE="$HOME/.local/share/applications/KinoNode.desktop"

# Detect the user's desktop directory (respects localized folder names)
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
DESKTOP_DESKTOP_FILE="$DESKTOP_DIR/KinoNode.desktop"

# Check if icon exists at target location
if [ ! -f "$ICON_PATH" ]; then
    if [ -f "$SCRIPT_DIR/logo.png" ]; then
        ICON_PATH="$SCRIPT_DIR/logo.png"
    fi
fi

# Check if executable exists at target location
if [ ! -f "$EXEC_PATH" ]; then
    echo "Error: bin/KinoNode not found in $TARGET_DIR"
    exit 1
fi

echo "Installing KinoNode shortcuts..."

# Also copy icon to hicolor theme path as kinonode.png
mkdir -p "$HOME/.local/share/icons/hicolor/256x256/apps"
if [ -f "$ICON_PATH" ]; then
    cp "$ICON_PATH" "$HOME/.local/share/icons/hicolor/256x256/apps/kinonode.png"
fi

# Build the .desktop file content once
DESKTOP_CONTENT="[Desktop Entry]
Name=KinoNode
GenericName=Movie Library Manager
Comment=Personal Movie Library Manager & Media Center
Exec=$EXEC_PATH
Icon=$ICON_PATH
Terminal=false
Type=Application
Categories=Utility;AudioVideo;
StartupWMClass=io-github-kullik01-kinonode-Launcher"

# --- Application menu shortcut ---
mkdir -p "$HOME/.local/share/applications"
echo "$DESKTOP_CONTENT" > "$MENU_DESKTOP_FILE"
chmod +x "$MENU_DESKTOP_FILE"

# Update desktop database to refresh the menu immediately
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null
gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" 2>/dev/null || true

echo "  Added to application menu."

# --- Desktop icon ---
if [ -d "$DESKTOP_DIR" ]; then
    echo "$DESKTOP_CONTENT" > "$DESKTOP_DESKTOP_FILE"
    chmod +x "$DESKTOP_DESKTOP_FILE"

    # Mark as trusted so the desktop environment shows the icon without prompting
    gio set "$DESKTOP_DESKTOP_FILE" "metadata::trusted" true 2>/dev/null || true

    echo "  Added desktop icon to $DESKTOP_DIR."
else
    echo "  Desktop directory ($DESKTOP_DIR) not found, skipping desktop icon."
fi

echo ""
echo "Success! KinoNode has been added to your application menu and desktop."
