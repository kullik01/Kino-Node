package io.github.kullik01.kinonode.service;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.util.PathUtils;
import javafx.concurrent.Task;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibraryScanService {
    private static final Logger LOGGER = Logger.getLogger(LibraryScanService.class.getName());
    private final MovieRepository movieRepository;
    private final SettingsRepository settingsRepository;

    public LibraryScanService(MovieRepository movieRepository, SettingsRepository settingsRepository) {
        this.movieRepository = movieRepository;
        this.settingsRepository = settingsRepository;
    }

    public Task<ScanResult> createScanTask() {
        return new Task<>() {
            @Override
            protected ScanResult call() throws Exception {
                updateMessage("Reading workspace settings...");
                
                String foldersSetting = settingsRepository.get("workspace_folders", "");
                String extensionsSetting = settingsRepository.get("video_extensions", "mp4,mkv,avi,mov,m4v");
                
                if (foldersSetting.isBlank()) {
                    return new ScanResult(0, 0, List.of("No workspace folders configured. Please configure folders in Settings."));
                }

                String[] folders = foldersSetting.split(";");
                Set<String> extensions = new HashSet<>();
                for (String ext : extensionsSetting.split(",")) {
                    if (!ext.isBlank()) {
                        extensions.add(ext.trim().toLowerCase());
                    }
                }

                List<Path> filesToScan = new ArrayList<>();
                updateMessage("Scanning directories...");

                for (String folderPath : folders) {
                    if (folderPath.isBlank()) continue;
                    Path path = Paths.get(folderPath.trim());
                    if (Files.exists(path) && Files.isDirectory(path)) {
                        Files.walkFileTree(path, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                String ext = PathUtils.getFileExtension(file.toFile());
                                if (extensions.contains(ext)) {
                                    filesToScan.add(file);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                }

                int totalFiles = filesToScan.size();
                int newMoviesAdded = 0;
                List<String> logs = new ArrayList<>();
                LOGGER.log(Level.INFO, "Found {0} potential video files to scan.", totalFiles);

                for (int i = 0; i < totalFiles; i++) {
                    if (isCancelled()) {
                        updateMessage("Scan cancelled.");
                        break;
                    }
                    Path file = filesToScan.get(i);
                    String absolutePath = file.toAbsolutePath().toString().replace("\\", "/");
                    
                    updateProgress(i, totalFiles);
                    updateMessage("Processing file (" + (i + 1) + "/" + totalFiles + "): " + file.getFileName());

                    if (!movieRepository.existsByFilePath(absolutePath)) {
                        Movie movie = parseMovieFromFile(file.toFile());
                        movieRepository.insert(movie);
                        newMoviesAdded++;
                        logs.add("Added: " + movie.getTitle());
                        LOGGER.log(Level.INFO, "Imported movie: {0}", movie.getTitle());
                    }
                }

                updateProgress(totalFiles, totalFiles);
                updateMessage("Scan complete. Added " + newMoviesAdded + " new movies.");
                return new ScanResult(totalFiles, newMoviesAdded, logs);
            }
        };
    }

    private Movie parseMovieFromFile(File file) {
        String filename = PathUtils.getFileNameWithoutExtension(file);
        
        // Clean up name: replace dots and underscores with spaces
        String cleanName = filename.replace(".", " ").replace("_", " ");
        
        // Match year: matches space and 4-digit numbers in optional parentheses or brackets
        Pattern yearPattern = Pattern.compile("^(.*?)\\s*[\\(\\[]?(19\\d{2}|20\\d{2})[\\)\\]]?(.*)$");
        Matcher matcher = yearPattern.matcher(cleanName);
        
        String title = cleanName;
        Integer year = null;
        
        if (matcher.find()) {
            String tempTitle = matcher.group(1).trim();
            if (!tempTitle.isEmpty()) {
                title = tempTitle;
            }
            try {
                year = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {}
        }
        
        // Remove multiple consecutive spaces
        title = title.replaceAll("\\s+", " ").trim();
        if (title.isEmpty()) {
            title = filename;
        }

        Movie movie = new Movie(file.getAbsolutePath().replace("\\", "/"), title);
        movie.setYear(year);
        movie.setDateAdded(Instant.now());
        return movie;
    }

    public record ScanResult(int totalScanned, int newAdded, List<String> logs) {}
}
