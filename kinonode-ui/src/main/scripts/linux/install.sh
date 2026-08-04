#!/bin/bash
# install.sh
set -e
INSTALL_DIR="$HOME/.kinonode-app"
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
RELEASE_DIR_NAME="$(basename "$SCRIPT_DIR")"

echo "Installing KinoNode to $INSTALL_DIR ..."
rm -rf "$INSTALL_DIR" # Remove any previous installation safely
mkdir -p "$INSTALL_DIR"
cp -r "$SCRIPT_DIR" "$INSTALL_DIR/$RELEASE_DIR_NAME"

chmod +x "$INSTALL_DIR/$RELEASE_DIR_NAME/bin/KinoNode"
echo "Installation complete. Run install_shortcut.sh to add icons."
