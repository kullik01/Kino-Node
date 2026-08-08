#!/usr/bin/env bash
# 
# BSD 3-Clause License
# Copyright (c) 2026, Hannah Kullik
#
# install.sh
# 
# KinoNode Application Installer for Linux
# Copies application binaries into user environment and sets execution rights.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$(realpath "$0")")" && pwd)"

# Target installation path in user home
INSTALL_BASE="${HOME}/.kinonode-app"
TARGET_DIR="${INSTALL_BASE}/KinoNode"

echo "Installing KinoNode to ${TARGET_DIR}..."

# Remove previous installation to guarantee clean update
rm -rf "${TARGET_DIR}"
mkdir -p "${INSTALL_BASE}"

# Copy release bundle into target installation directory
cp -r "${SCRIPT_DIR}" "${TARGET_DIR}"

# Ensure execution permissions on main launcher binary
if [ -f "${TARGET_DIR}/bin/KinoNode" ]; then
    chmod +x "${TARGET_DIR}/bin/KinoNode"
fi

# Automatically execute desktop shortcut installer if present
if [ -f "${SCRIPT_DIR}/install_shortcut.sh" ]; then
    bash "${SCRIPT_DIR}/install_shortcut.sh"
fi

echo "KinoNode installation complete! You can now launch KinoNode directly from your Applications menu or Desktop shortcut."
