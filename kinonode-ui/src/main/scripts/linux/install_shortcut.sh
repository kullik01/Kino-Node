#!/bin/bash
# install_shortcut.sh
INSTALL_DIR="$HOME/.kinonode-app"
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
RELEASE_DIR_NAME="$(basename "$SCRIPT_DIR")"
INSTALLED_DIR="$INSTALL_DIR/$RELEASE_DIR_NAME"

ICON_PATH="$INSTALLED_DIR/logo.png"
EXEC_PATH="$INSTALLED_DIR/bin/KinoNode"
MENU_DESKTOP_FILE="$HOME/.local/share/applications/KinoNode.desktop"
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
DESKTOP_DESKTOP_FILE="$DESKTOP_DIR/KinoNode.desktop"

DESKTOP_CONTENT="[Desktop Entry]
Name=KinoNode
Comment=Personal Movie Library Manager
Exec=$EXEC_PATH
Icon=$ICON_PATH
Terminal=false
Type=Application
Categories=Utility;AudioVideo;"

# 1. Application Menu Shortcut
mkdir -p "$HOME/.local/share/applications"
echo "$DESKTOP_CONTENT" > "$MENU_DESKTOP_FILE"
chmod +x "$MENU_DESKTOP_FILE"
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null
echo "Added KinoNode to application menu."

# 2. Desktop Icon
if [ -d "$DESKTOP_DIR" ]; then
    echo "$DESKTOP_CONTENT" > "$DESKTOP_DESKTOP_FILE"
    chmod +x "$DESKTOP_DESKTOP_FILE"
    gio set "$DESKTOP_DESKTOP_FILE" "metadata::trusted" true 2>/dev/null
    echo "Added KinoNode desktop shortcut."
fi
