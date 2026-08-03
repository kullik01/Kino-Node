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
package io.github.kullik01.kinonode.service;

import java.io.InputStream;


import io.github.kullik01.kinonode.repository.SettingsRepository;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.inject.Inject;

/**
 * Service class responsible for handling media file playback using external
 * players (such as MPV or VLC). Handles detection of available players on the
 * system PATH and triggers OS-specific process commands to start video files.
 */
public class PlaybackService {
  private static final Logger LOGGER =
      Logger.getLogger(PlaybackService.class.getName());
  private final SettingsRepository settingsRepository;

  /**
   * Constructs a PlaybackService and initializes it with user settings.
   *
   * @param settingsRepository the repository to read configured custom player
   *        paths from
   */
  @Inject
  public PlaybackService(SettingsRepository settingsRepository) {
    this.settingsRepository = settingsRepository;
  }

  /**
   * Resolves the configured player executable path, or attempts to auto-detect
   * one. Checks the user settings first, then searches the system PATH for
   * 'mpv' and 'vlc', and finally looks in typical installation folders
   * depending on the operating system.
   *
   * @return an {@link Optional} containing the {@link Path} to the media player
   *         executable if found, or {@link Optional#empty()}
   */
  public Optional<Path> getPlayerPath() {
    String tmpConfigured = settingsRepository.get("player_executable_path", "");
    if (!tmpConfigured.isBlank()) {
      Path tmpPath = Paths.get(tmpConfigured);
      if (Files.exists(tmpPath)) {
        return Optional.of(tmpPath);
      }
    }

    // Try to auto-detect mpv on PATH
    Optional<Path> tmpMpvOnPath = findExecutableOnPath("mpv");
    if (tmpMpvOnPath.isPresent()) {
      return tmpMpvOnPath;
    }

    // Try to auto-detect vlc on PATH
    Optional<Path> tmpVlcOnPath = findExecutableOnPath("vlc");
    if (tmpVlcOnPath.isPresent()) {
      return tmpVlcOnPath;
    }

    // Try common installation directories for VLC depending on OS platforms
    String tmpOperatingSystemName = System.getProperty("os.name").toLowerCase();
    if (tmpOperatingSystemName.contains("win")) {
      Path tmpVlcDefault64 =
          Paths.get("C:\\Program Files\\VideoLAN\\VLC\\vlc.exe");
      if (Files.exists(tmpVlcDefault64)) {
        return Optional.of(tmpVlcDefault64);
      }
      Path tmpVlcDefault32 =
          Paths.get("C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe");
      if (Files.exists(tmpVlcDefault32)) {
        return Optional.of(tmpVlcDefault32);
      }
    } else if (tmpOperatingSystemName.contains("mac")) {
      Path tmpVlcMac = Paths.get("/Applications/VLC.app/Contents/MacOS/VLC");
      if (Files.exists(tmpVlcMac)) {
        return Optional.of(tmpVlcMac);
      }
    }

    return Optional.empty();
  }

  /**
   * Play the movie in the configured/detected external media player. Launces an
   * external OS process to execute the media player.
   *
   * @param movieFilePath the absolute file path of the movie file to play
   * @throws Exception if player path is not resolved, the movie file is
   *         missing, or starting the process fails
   */
  public void play(String movieFilePath) throws Exception {
    Optional<Path> tmpPlayerPathOptional = getPlayerPath();
    if (tmpPlayerPathOptional.isEmpty()) {
      throw new IllegalStateException(
          "No external media player executable configured or auto-detected.");
    }

    Path tmpPlayerPath = tmpPlayerPathOptional.get();
    File tmpMovieFile = new File(movieFilePath);
    if (!tmpMovieFile.exists()) {
      throw new IllegalArgumentException(
          "Movie file does not exist: " + movieFilePath);
    }

    LOGGER.log(Level.INFO, "Launching movie playback: {0} using player {1}",
        new Object[] {movieFilePath, tmpPlayerPath.toAbsolutePath()});

    ProcessBuilder tmpProcessBuilder;
    // Windows requires starting process asynchronously via cmd start command to
    // prevent
    // blocking/freezing the parent UI
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      tmpProcessBuilder = new ProcessBuilder("cmd", "/c", "start", "\"\"",
          tmpPlayerPath.toAbsolutePath().toString(),
          tmpMovieFile.getAbsolutePath());
    } else {
      // Unix-based systems execute the binary directly and discard std outputs
      // to decouple process
      // lifecycle
      tmpProcessBuilder =
          new ProcessBuilder(tmpPlayerPath.toAbsolutePath().toString(),
              tmpMovieFile.getAbsolutePath());
      tmpProcessBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
      tmpProcessBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    }
    tmpProcessBuilder.start();
  }

  /**
   * Searches the system PATH environment variables for a given
   * command/executable. Employs OS-specific search binaries ('where' on
   * Windows, 'which' on Linux/macOS).
   *
   * @param name the name of the executable to search for (e.g. "mpv", "vlc")
   * @return an {@link Optional} containing the resolved path, or
   *         {@link Optional#empty()}
   */
  private Optional<Path> findExecutableOnPath(String name) {
    String tmpCommand =
        System.getProperty("os.name").toLowerCase().contains("win") ? "where"
            : "which";
    try {
      Process tmpProcess = new ProcessBuilder(tmpCommand, name).start();
      try (BufferedReader tmpReader = new BufferedReader(
          new InputStreamReader(tmpProcess.getInputStream()))) {
        String tmpLine = tmpReader.readLine();
        if (tmpLine != null && !tmpLine.trim().isEmpty()) {
          Path tmpPath = Paths.get(tmpLine.trim());
          if (Files.exists(tmpPath)) {
            return Optional.of(tmpPath);
          }
        }
      }
      tmpProcess.waitFor();
    } catch (Exception tmpIgnoredException) {
    }
    return Optional.empty();
  }
}
