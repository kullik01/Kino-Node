/**
 * BSD 3-Clause License
 *
 * <p>
 * Copyright (c) 2026, Hannah Kullik
 *
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.uiutil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-platform desktop integration service for Linux, Windows, and macOS.
 * Automatically verifies and installs application menu entries, desktop icons,
 * and shortcuts upon application startup.
 */
public final class DesktopIntegration {

  private static final Logger LOGGER =
      Logger.getLogger(DesktopIntegration.class.getName());

  private DesktopIntegration() {
    // Utility class
  }

  /**
   * Asynchronously verifies and ensures desktop shortcuts and application icons
   * are created on the user's desktop and application menu across Linux,
   * Windows, and macOS.
   */
  public static void ensureDesktopIntegrationAsync() {
    Thread tmpThread = new Thread(DesktopIntegration::performIntegration,
        "KinoNode-DesktopIntegration");
    tmpThread.setDaemon(true);
    tmpThread.start();
  }

  private static void performIntegration() {
    String tmpOsName = System.getProperty("os.name", "").toLowerCase();
    try {
      if (tmpOsName.contains("win")) {
        performWindowsIntegration();
      } else if (tmpOsName.contains("mac")) {
        performMacIntegration();
      } else if (tmpOsName.contains("linux") || tmpOsName.contains("unix")) {
        performLinuxIntegration();
      }
    } catch (Exception tmpException) {
      LOGGER.log(Level.WARNING,
          "Desktop integration notice: " + tmpException.getMessage());
    }
  }

  private static void performLinuxIntegration() throws Exception {
    String tmpUserHome = System.getProperty("user.home");
    if (tmpUserHome == null || tmpUserHome.isBlank()) {
      return;
    }

    Path tmpExecPath = resolveExecutablePath("KinoNode");
    if (tmpExecPath == null || !Files.exists(tmpExecPath)) {
      return;
    }

    // Deploy icon to ~/.local/share/icons/hicolor/256x256/apps/kinonode.png
    Path tmpIconDir = Paths.get(tmpUserHome, ".local", "share", "icons",
        "hicolor", "256x256", "apps");
    Files.createDirectories(tmpIconDir);
    Path tmpIconPath = tmpIconDir.resolve("kinonode.png");

    if (!Files.exists(tmpIconPath)) {
      try (InputStream tmpStream = DesktopIntegration.class.getResourceAsStream(
          "/io/github/kullik01/kinonode/images/logos/logo_highres.png")) {
        if (tmpStream != null) {
          Files.copy(tmpStream, tmpIconPath);
        } else {
          try (InputStream tmpFallback =
              DesktopIntegration.class.getResourceAsStream(
                  "/io/github/kullik01/kinonode/images/logos/logo.png")) {
            if (tmpFallback != null) {
              Files.copy(tmpFallback, tmpIconPath);
            }
          }
        }
      }
    }

    String tmpExecString = tmpExecPath.toAbsolutePath().toString();
    String tmpIconString = tmpIconPath.toAbsolutePath().toString();

    String tmpDesktopContent = """
        [Desktop Entry]
        Version=1.0
        Type=Application
        Name=KinoNode
        GenericName=Movie Library Manager
        Comment=Personal Movie Library Manager & Media Center
        Exec=%s
        Icon=%s
        Terminal=false
        Categories=Utility;AudioVideo;Video;Player;Java;
        StartupWMClass=io-github-kullik01-kinonode-Launcher
        MimeType=video/mp4;video/x-matroska;video/webm;
        Keywords=Movie;Film;Media;Library;Watchlist;
        """.formatted(tmpExecString, tmpIconString);

    // Application Menu Entry
    Path tmpAppsDir = Paths.get(tmpUserHome, ".local", "share", "applications");
    Files.createDirectories(tmpAppsDir);
    Path tmpMenuDesktopFile = tmpAppsDir.resolve("KinoNode.desktop");
    Files.writeString(tmpMenuDesktopFile, tmpDesktopContent);
    tmpMenuDesktopFile.toFile().setExecutable(true);

    // Desktop Shortcut
    Path tmpDesktopDir = resolveDesktopDirectory(tmpUserHome);
    if (Files.exists(tmpDesktopDir) && Files.isDirectory(tmpDesktopDir)) {
      Path tmpShortcutFile = tmpDesktopDir.resolve("KinoNode.desktop");
      Files.writeString(tmpShortcutFile, tmpDesktopContent);
      tmpShortcutFile.toFile().setExecutable(true);

      try {
        new ProcessBuilder("gio", "set",
            tmpShortcutFile.toAbsolutePath().toString(), "metadata::trusted",
            "true").start().waitFor();
      } catch (Exception tmpException) {
        // optional gio metadata
      }
    }

    try {
      new ProcessBuilder("update-desktop-database",
          tmpAppsDir.toAbsolutePath().toString()).start().waitFor();
    } catch (Exception tmpException) {
    }
    try {
      new ProcessBuilder("gtk-update-icon-cache", "-f", "-t",
          Paths.get(tmpUserHome, ".local", "share", "icons", "hicolor")
              .toAbsolutePath().toString())
          .start().waitFor();
    } catch (Exception tmpException) {
    }

    LOGGER.info("Linux desktop shortcut and icon integration complete.");
  }

  private static void performWindowsIntegration() throws Exception {
    String tmpUserHome = System.getProperty("user.home");
    if (tmpUserHome == null || tmpUserHome.isBlank()) {
      return;
    }

    Path tmpExecPath = resolveExecutablePath("KinoNode.exe");
    if (tmpExecPath == null || !Files.exists(tmpExecPath)) {
      return;
    }

    Path tmpDesktopDir = resolveDesktopDirectory(tmpUserHome);
    Path tmpShortcutPath = tmpDesktopDir.resolve("KinoNode.lnk");

    if (!Files.exists(tmpShortcutPath)) {
      String tmpExecString = tmpExecPath.toAbsolutePath().toString();
      String tmpWorkDir = tmpExecPath.getParent().toAbsolutePath().toString();

      String tmpPowerShellCommand = String.format(
          "$wsh = New-Object -ComObject WScript.Shell; "
              + "$s = $wsh.CreateShortcut('%s'); " + "$s.TargetPath = '%s'; "
              + "$s.WorkingDirectory = '%s'; "
              + "$s.Description = 'KinoNode - Personal Movie Library Manager'; "
              + "$s.Save()",
          tmpShortcutPath.toAbsolutePath().toString().replace("'", "''"),
          tmpExecString.replace("'", "''"), tmpWorkDir.replace("'", "''"));

      new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy",
          "Bypass", "-Command", tmpPowerShellCommand).start().waitFor();

      LOGGER.info("Windows desktop shortcut created at: " + tmpShortcutPath);
    }
  }

  private static void performMacIntegration() throws Exception {
    String tmpUserHome = System.getProperty("user.home");
    if (tmpUserHome == null || tmpUserHome.isBlank()) {
      return;
    }

    Path tmpDesktopDir = resolveDesktopDirectory(tmpUserHome);
    Path tmpAppBundle = Paths.get("/Applications/KinoNode.app");

    if (Files.exists(tmpAppBundle)) {
      Path tmpDesktopAlias = tmpDesktopDir.resolve("KinoNode.app");
      if (!Files.exists(tmpDesktopAlias)) {
        try {
          new ProcessBuilder("osascript", "-e",
              "tell application \"Finder\" to make new alias file at POSIX file \""
                  + tmpDesktopDir.toAbsolutePath() + "\" to POSIX file \""
                  + tmpAppBundle.toAbsolutePath() + "\"")
              .start().waitFor();
        } catch (Exception tmpException) {
          try {
            new ProcessBuilder("ln", "-s",
                tmpAppBundle.toAbsolutePath().toString(),
                tmpDesktopAlias.toAbsolutePath().toString()).start().waitFor();
          } catch (Exception tmpIgnored) {
          }
        }
        LOGGER.info("macOS desktop shortcut alias created for KinoNode.app");
      }
    }
  }

  private static Path resolveExecutablePath(String tmpExecName) {
    try {
      var tmpCommand = ProcessHandle.current().info().command();
      if (tmpCommand.isPresent()) {
        Path tmpPath = Paths.get(tmpCommand.get());
        if (Files.exists(tmpPath) && tmpPath.getFileName() != null && tmpPath
            .getFileName().toString().toLowerCase().contains("kinonode")) {
          return tmpPath;
        }
      }
    } catch (Exception tmpException) {
    }

    Path tmpCurrentDirExec =
        Paths.get(System.getProperty("user.dir"), "bin", tmpExecName);
    if (Files.exists(tmpCurrentDirExec)) {
      return tmpCurrentDirExec;
    }

    Path tmpCurrentDirRootExec =
        Paths.get(System.getProperty("user.dir"), tmpExecName);
    if (Files.exists(tmpCurrentDirRootExec)) {
      return tmpCurrentDirRootExec;
    }

    return null;
  }

  private static Path resolveDesktopDirectory(String tmpUserHome) {
    String tmpOsName = System.getProperty("os.name", "").toLowerCase();
    if (tmpOsName.contains("linux") || tmpOsName.contains("unix")) {
      try {
        Process tmpProcess =
            new ProcessBuilder("xdg-user-dir", "DESKTOP").start();
        String tmpOutput =
            new String(tmpProcess.getInputStream().readAllBytes()).trim();
        if (tmpProcess.waitFor() == 0 && !tmpOutput.isBlank()) {
          return Paths.get(tmpOutput);
        }
      } catch (Exception tmpException) {
      }
    }
    return Paths.get(tmpUserHome, "Desktop");
  }
}
