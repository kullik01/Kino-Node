#!/bin/bash
# uninstall.sh

echo "Warning: This will completely remove KinoNode, including your movie database (kinonode.db) and all cached posters."
read -p "Are you sure you want to proceed? (y/N): " CONFIRM
if [[ "$CONFIRM" != [yY] && "$CONFIRM" != [yY][eE][sS] ]]; then
    echo "Uninstallation aborted."
    exit 1
fi

# 1. Remove Application Binaries
rm -rf "$HOME/.kinonode-app"

# 2. Remove Menu and Desktop Shortcuts
rm -f "$HOME/.local/share/applications/KinoNode.desktop"
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
rm -f "$DESKTOP_DIR/KinoNode.desktop"

# 3. Remove AppData (Database, settings, cached images)
# Note: Ensure this matches the exact path your DatabaseService uses for Linux
rm -rf "$HOME/.kinonode"

echo "KinoNode has been 100% removed from your system."
