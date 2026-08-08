#!/bin/bash

# KinoNode Installer for Linux
# Removes any previous version and installs the current release to ~/.kinonode-app.

set -e

INSTALL_DIR="$HOME/.kinonode-app"

# Determine the directory where this script is located (inside the release archive)
SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# The release folder name follows the pattern KinoNode-x.y.z.
RELEASE_DIR_NAME="$(basename "$SCRIPT_DIR")"

# --- Validate release structure -------------------------------------------------
if [ ! -f "$SCRIPT_DIR/bin/KinoNode" ]; then
    echo "Error: bin/KinoNode not found in $SCRIPT_DIR"
    echo "Please run this script from within the extracted KinoNode release directory."
    exit 1
fi

# --- Remove previous installation -----------------------------------------------
if [ -d "$INSTALL_DIR" ]; then
    echo "Existing installation found at $INSTALL_DIR."

    # List old version directories (anything matching KinoNode-*)
    OLD_VERSIONS=$(find "$INSTALL_DIR" -maxdepth 1 -type d -name "KinoNode-*" 2>/dev/null)

    if [ -n "$OLD_VERSIONS" ]; then
        echo "Removing old version(s):"
        for OLD_DIR in $OLD_VERSIONS; do
            echo "  - $(basename "$OLD_DIR")"
            rm -rf "$OLD_DIR"
        done
    fi
else
    echo "No previous installation found. Creating $INSTALL_DIR."
    mkdir -p "$INSTALL_DIR"
fi

# --- Install new version --------------------------------------------------------
echo ""
echo "Installing $RELEASE_DIR_NAME to $INSTALL_DIR ..."
cp -r "$SCRIPT_DIR" "$INSTALL_DIR/$RELEASE_DIR_NAME"

# Ensure the launcher is executable
chmod +x "$INSTALL_DIR/$RELEASE_DIR_NAME/bin/KinoNode"

# Automatically run shortcut installer if present
if [ -f "$INSTALL_DIR/$RELEASE_DIR_NAME/install_shortcut.sh" ]; then
    bash "$INSTALL_DIR/$RELEASE_DIR_NAME/install_shortcut.sh"
fi

echo ""
echo "Installation complete!"
echo ""
echo "  Run KinoNode:"
echo "    sh $INSTALL_DIR/$RELEASE_DIR_NAME/bin/KinoNode"
echo ""
echo "  Add a desktop shortcut (optional):"
echo "    sh $INSTALL_DIR/$RELEASE_DIR_NAME/install_shortcut.sh"
echo ""
