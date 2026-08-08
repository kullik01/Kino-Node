#!/bin/bash
# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# install.sh
# 
# KinoNode Application Installer for Linux
# This script copies the application binaries to the user's home directory.

set -e
# Determine the installation directory (hidden app folder in home)
INSTALL_DIR="$HOME/.kinonode-app"
# Determine the directory where this script resides (inside the bundle)
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
# The name of the release folder
RELEASE_DIR_NAME="$(basename "$SCRIPT_DIR")"

echo "Installing KinoNode to $INSTALL_DIR ..."

# Remove any previous installation to ensure a clean upgrade
rm -rf "$INSTALL_DIR" 
mkdir -p "$INSTALL_DIR"

# Copy the entire release bundle into the installation directory
cp -r "$SCRIPT_DIR" "$INSTALL_DIR/$RELEASE_DIR_NAME"

# Ensure the main KinoNode executable has execute permissions
chmod +x "$INSTALL_DIR/$RELEASE_DIR_NAME/bin/KinoNode"

echo "Installation complete. Run install_shortcut.sh to add icons."
