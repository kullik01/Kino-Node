/**
 * BSD 3-Clause License
 *
 * <p>Copyright (c) 2026, Hannah Kullik
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.service;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.repository.MovieRepository;
import io.github.kullik01.kinonode.repository.SettingsRepository;
import io.github.kullik01.kinonode.util.PathUtils;
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
import javafx.concurrent.Task;

/**
 * Service class responsible for scanning configured workspace directories to import and catalog
 * movie files. Parses file names to extract titles and release years, queries online databases
 * (IMDb/Wikidata/YouTube) to enrich movies with posters and trailer links, and updates/inserts them
 * into the local repository.
 */
public class LibraryScanService {
  private static final Logger LOGGER = Logger.getLogger(LibraryScanService.class.getName());
  private final MovieRepository movieRepository;
  private final SettingsRepository settingsRepository;

  /**
   * Constructs a new LibraryScanService with the required repositories.
   *
   * @param movieRepository the repository storing movie metadata records
   * @param settingsRepository the repository holding application settings like scanned folders and
   *     valid extensions
   */
  public LibraryScanService(
      MovieRepository movieRepository, SettingsRepository settingsRepository) {
    this.movieRepository = movieRepository;
    this.settingsRepository = settingsRepository;
  }

  /**
   * Creates a JavaFX {@link Task} representing a background directory scan operation. The task
   * scans all configured workspace folders, matches video files against registered file extensions,
   * imports new files as movie records, and enriches existing ones missing critical metadata.
   *
   * @return a JavaFX Task containing the {@link ScanResult} of the scan operation
   */
  public Task<ScanResult> createScanTask() {
    return new Task<>() {
      @Override
      protected ScanResult call() throws Exception {
        updateProgress(-1, 1);
        updateMessage("Reading workspace settings...");

        // Load configured workspace folders and allowed video file extensions from settings
        String foldersSetting = settingsRepository.get("workspace_folders", "");
        String extensionsSetting =
            settingsRepository.get(
                "video_extensions", "3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv");

        if (foldersSetting.isBlank()) {
          return new ScanResult(
              0,
              0,
              List.of("No workspace folders configured. Please configure folders in Settings."));
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

        // Walk through the file tree for each configured workspace directory
        for (String folderPath : folders) {
          if (isCancelled()) break;
          if (folderPath.isBlank()) continue;
          Path path = Paths.get(folderPath.trim());
          if (Files.exists(path) && Files.isDirectory(path)) {
            Files.walkFileTree(
                path,
                new SimpleFileVisitor<>() {
                  @Override
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                      throws IOException {
                    if (isCancelled()) return FileVisitResult.TERMINATE;
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

        // Process each discovered video file
        for (int i = 0; i < totalFiles; i++) {
          if (isCancelled()) {
            updateMessage("Scan cancelled.");
            break;
          }
          Path file = filesToScan.get(i);
          String absolutePath = file.toAbsolutePath().toString().replace("\\", "/");

          updateProgress(i, totalFiles);
          updateMessage(
              "Processing file (" + (i + 1) + "/" + totalFiles + "): " + file.getFileName());

          // Case 1: The movie file is not yet registered in the repository
          if (!movieRepository.existsByFilePath(absolutePath)) {
            Movie movie = parseMovieFromFile(file.toFile());

            // Check and enrich movie details using IMDb suggestion and Wikidata APIs
            updateMessage("Checking IMDB for: " + movie.getTitle());
            ImdbService.enrichMovieData(movie);

            // Fetch and set a trailer URL from YouTube
            updateMessage("Checking YouTube for trailer: " + movie.getTitle());
            YoutubeService.fetchYoutubeTrailer(movie);

            movieRepository.insert(movie);
            newMoviesAdded++;
            logs.add("Added: " + movie.getTitle());
            LOGGER.log(Level.INFO, "Imported movie: {0}", movie.getTitle());
          } else {
            // Case 2: The movie is already registered, but we check if it is missing metadata that
            // can be enriched
            Movie existingMovie = movieRepository.findByFilePath(absolutePath);
            boolean needsEnrichment =
                existingMovie != null
                    && (existingMovie.getCoverImagePath() == null
                        || existingMovie.getCoverImagePath().isBlank()
                        || existingMovie.getDirector() == null
                        || existingMovie.getDirector().isBlank()
                        || existingMovie.getWriter() == null
                        || existingMovie.getWriter().isBlank()
                        || existingMovie.getTrailerUrl() == null
                        || existingMovie.getTrailerUrl().isBlank());

            if (needsEnrichment) {
              updateMessage("Checking IMDB for existing movie: " + existingMovie.getTitle());
              String originalTitle = existingMovie.getTitle();

              // Re-parse the title from the filename just in case the stored title is full of
              // garbage tags
              Movie freshlyParsed = parseMovieFromFile(file.toFile());
              existingMovie.setTitle(freshlyParsed.getTitle());

              ImdbService.enrichMovieData(existingMovie);

              if (existingMovie.getTrailerUrl() == null
                  || existingMovie.getTrailerUrl().isBlank()) {
                updateMessage("Checking YouTube for trailer: " + existingMovie.getTitle());
                YoutubeService.fetchYoutubeTrailer(existingMovie);
              }

              // Always update after attempting enrichment to save new fields (Director, Writer,
              // etc)
              movieRepository.update(existingMovie);
              logs.add("Enriched: " + existingMovie.getTitle());
              LOGGER.log(Level.INFO, "Enriched existing movie: {0}", existingMovie.getTitle());
            }
          }
        }

        updateProgress(totalFiles, totalFiles);
        updateMessage("Scan complete. Added " + newMoviesAdded + " new movies.");
        return new ScanResult(totalFiles, newMoviesAdded, logs);
      }
    };
  }

  /**
   * Parses raw file attributes to initialize a Movie entity. Strips common release group/encoding
   * tags and extracts the release year from the filename via regular expressions.
   *
   * @param file the video file to parse
   * @return a {@link Movie} entity populated with parsed title, year, and path properties
   */
  private Movie parseMovieFromFile(File file) {
    String filename = PathUtils.getFileNameWithoutExtension(file);

    // Clean up name: replace dots and underscores with spaces
    String cleanName = filename.replace(".", " ").replace("_", " ");

    // Remove common garbage tags from release names to get a clean search query
    String[] garbageTags = {
      "1080p",
      "720p",
      "480p",
      "2160p",
      "4k",
      "bluray",
      "blu-ray",
      "webrip",
      "hdrip",
      "dvdrip",
      "brrip",
      "x264",
      "x265",
      "hevc",
      "aac",
      "ac3",
      "dts",
      "remux",
      "dual",
      "audio",
      "deutsch",
      "german",
      "english",
      "hd",
      "horrorfilm",
      "uncut",
      "extended",
      "director's cut",
      "directors cut"
    };

    for (String tag : garbageTags) {
      cleanName = cleanName.replaceAll("(?i)\\b" + tag + "\\b", "");
    }

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
      } catch (NumberFormatException ignored) {
      }
    }

    // Remove multiple consecutive spaces and trailing hyphens
    title = title.replaceAll("\\s+", " ").replaceAll("-\\s*$", "").trim();
    if (title.isEmpty()) {
      title = filename;
    }

    Movie movie = new Movie(file.getAbsolutePath().replace("\\", "/"), title);
    movie.setYear(year);
    movie.setDateAdded(Instant.now());
    return movie;
  }

  /**
   * Represents the result of a library scan operation.
   *
   * @param totalScanned the total number of files scanned in the workspace folders
   * @param newAdded the number of new movies successfully imported and added to the database
   * @param logs a list of logging/operation messages describing the files processed
   */
  public record ScanResult(int totalScanned, int newAdded, List<String> logs) {}
}
