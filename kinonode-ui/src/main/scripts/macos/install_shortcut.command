#!/usr/bin/env bash
# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# install_shortcut.command - KinoNode macOS Desktop Shortcut Script

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$(realpath "$0")")" && pwd)"
DESKTOP_DIR="${HOME}/Desktop"

echo "Creating KinoNode desktop shortcut on macOS..."

# Find target application bundle or binary
APP_PATH=""
if [ -d "/Applications/KinoNode.app" ]; then
    APP_PATH="/Applications/KinoNode.app"
elif [[ "$SCRIPT_DIR" == *".app"* ]]; then
    APP_PATH="${SCRIPT_DIR%.app*}".app
elif [ -d "${HOME}/Applications/KinoNode.app" ]; then
    APP_PATH="${HOME}/Applications/KinoNode.app"
fi

if [ -n "$APP_PATH" ] && [ -d "$APP_PATH" ]; then
    echo "Creating Finder alias for $APP_PATH on Desktop..."
    osascript -e "tell application \"Finder\" to make new alias file at POSIX file \"$DESKTOP_DIR\" to POSIX file \"$APP_PATH\"" 2>/dev/null || {
        ln -sf "$APP_PATH" "${DESKTOP_DIR}/KinoNode.app"
    }
else
    # Create executable shortcut launcher
    LAUNCHER="${DESKTOP_DIR}/Launch KinoNode.command"
    EXEC_PATH="${SCRIPT_DIR}/bin/KinoNode"
    if [ ! -f "$EXEC_PATH" ]; then
        EXEC_PATH="${SCRIPT_DIR}/KinoNode"
    fi
    
    cat << EOF > "$LAUNCHER"
#!/usr/bin/env bash
exec "$EXEC_PATH" "\$@"
EOF
    chmod +x "$LAUNCHER"
    echo "Created launcher script: $LAUNCHER"
fi

echo "macOS desktop integration complete."
