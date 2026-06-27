/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2026, Hannah Kullik
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.service;

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

public class PlaybackService {
    private static final Logger LOGGER = Logger.getLogger(PlaybackService.class.getName());
    private final SettingsRepository settingsRepository;

    public PlaybackService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Resolves the configured player executable path, or attempts to auto-detect one.
     */
    public Optional<Path> getPlayerPath() {
        String configured = settingsRepository.get("player_executable_path", "");
        if (!configured.isBlank()) {
            Path path = Paths.get(configured);
            if (Files.exists(path)) {
                return Optional.of(path);
            }
        }

        // Try to auto-detect mpv on PATH
        Optional<Path> mpvOnPath = findExecutableOnPath("mpv");
        if (mpvOnPath.isPresent()) {
            return mpvOnPath;
        }

        // Try to auto-detect vlc on PATH
        Optional<Path> vlcOnPath = findExecutableOnPath("vlc");
        if (vlcOnPath.isPresent()) {
            return vlcOnPath;
        }

        // Try common installation directories for VLC on Windows
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            Path vlcDefault64 = Paths.get("C:\\Program Files\\VideoLAN\\VLC\\vlc.exe");
            if (Files.exists(vlcDefault64)) {
                return Optional.of(vlcDefault64);
            }
            Path vlcDefault32 = Paths.get("C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe");
            if (Files.exists(vlcDefault32)) {
                return Optional.of(vlcDefault32);
            }
        } else if (osName.contains("mac")) {
            Path vlcMac = Paths.get("/Applications/VLC.app/Contents/MacOS/VLC");
            if (Files.exists(vlcMac)) {
                return Optional.of(vlcMac);
            }
        }

        return Optional.empty();
    }

    /**
     * Play the movie in the configured/detected external media player.
     */
    public void play(String movieFilePath) throws Exception {
        Optional<Path> playerPathOpt = getPlayerPath();
        if (playerPathOpt.isEmpty()) {
            throw new IllegalStateException("No external media player executable configured or auto-detected.");
        }

        Path playerPath = playerPathOpt.get();
        File movieFile = new File(movieFilePath);
        if (!movieFile.exists()) {
            throw new IllegalArgumentException("Movie file does not exist: " + movieFilePath);
        }

        LOGGER.log(Level.INFO, "Launching movie playback: {0} using player {1}", 
                new Object[]{movieFilePath, playerPath.toAbsolutePath()});

        ProcessBuilder pb;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            pb = new ProcessBuilder(
                    "cmd", "/c", "start", "\"\"",
                    playerPath.toAbsolutePath().toString(),
                    movieFile.getAbsolutePath()
            );
        } else {
            pb = new ProcessBuilder(
                    playerPath.toAbsolutePath().toString(),
                    movieFile.getAbsolutePath()
            );
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }
        pb.start();
    }

    private Optional<Path> findExecutableOnPath(String name) {
        String cmd = System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which";
        try {
            Process p = new ProcessBuilder(cmd, name).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    Path path = Paths.get(line.trim());
                    if (Files.exists(path)) {
                        return Optional.of(path);
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return Optional.empty();
    }
}
