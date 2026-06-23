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

        ProcessBuilder pb = new ProcessBuilder(
                playerPath.toAbsolutePath().toString(),
                movieFile.getAbsolutePath()
        );
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
