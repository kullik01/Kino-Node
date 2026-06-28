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
 * Service class responsible for scanning configured workspace directories to
 * import and catalog movie files. Parses file names to extract titles and
 * release years, queries online databases (IMDb/Wikidata/YouTube) to enrich
 * movies with posters and trailer links, and updates/inserts them into the
 * local repository.
 */
public class LibraryScanService {
  private static final Logger LOGGER =
      Logger.getLogger(LibraryScanService.class.getName());
  private final MovieRepository movieRepository;
  private final SettingsRepository settingsRepository;

  /**
   * Constructs a new LibraryScanService with the required repositories.
   *
   * @param movieRepository the repository storing movie metadata records
   * @param settingsRepository the repository holding application settings like
   *        scanned folders and valid extensions
   */
  public LibraryScanService(MovieRepository movieRepository,
      SettingsRepository settingsRepository) {
    this.movieRepository = movieRepository;
    this.settingsRepository = settingsRepository;
  }

  /**
   * Creates a JavaFX {@link Task} representing a background directory scan
   * operation. The task scans all configured workspace folders, matches video
   * files against registered file extensions, imports new files as movie
   * records, and enriches existing ones missing critical metadata.
   *
   * @return a JavaFX Task containing the {@link ScanResult} of the scan
   *         operation
   */
  public Task<ScanResult> createScanTask() {
    return new Task<>() {
      @Override
      protected ScanResult call() throws Exception {
        updateProgress(-1, 1);
        updateMessage("Reading workspace settings...");

        // Load configured workspace folders and allowed video file extensions
        // from settings
        String tmpFoldersSetting =
            settingsRepository.get("workspace_folders", "");
        String tmpExtensionsSetting = settingsRepository.get("video_extensions",
            "3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv");

        if (tmpFoldersSetting.isBlank()) {
          return new ScanResult(0, 0, List.of(
              "No workspace folders configured. Please configure folders in Settings."));
        }

        String[] tmpFolders = tmpFoldersSetting.split(";");
        Set<String> tmpExtensions = new HashSet<>();
        for (String tmpExtension : tmpExtensionsSetting.split(",")) {
          if (!tmpExtension.isBlank()) {
            tmpExtensions.add(tmpExtension.trim().toLowerCase());
          }
        }

        List<Path> tmpFilesToScan = new ArrayList<>();
        updateMessage("Scanning directories...");

        // Walk through the file tree for each configured workspace directory
        for (String tmpFolderPath : tmpFolders) {
          if (isCancelled())
            break;
          if (tmpFolderPath.isBlank())
            continue;
          Path tmpPath = Paths.get(tmpFolderPath.trim());
          if (Files.exists(tmpPath) && Files.isDirectory(tmpPath)) {
            Files.walkFileTree(tmpPath, new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult visitFile(Path file,
                  BasicFileAttributes attrs) throws IOException {
                if (isCancelled())
                  return FileVisitResult.TERMINATE;
                String tmpExtension = PathUtils.getFileExtension(file.toFile());
                if (tmpExtensions.contains(tmpExtension)) {
                  tmpFilesToScan.add(file);
                }
                return FileVisitResult.CONTINUE;
              }
            });
          }
        }

        int tmpTotalFiles = tmpFilesToScan.size();
        int tmpNewMoviesAdded = 0;
        List<String> tmpLogs = new ArrayList<>();
        LOGGER.log(Level.INFO, "Found {0} potential video files to scan.",
            tmpTotalFiles);

        // Process each discovered video file
        for (int tmpIndex = 0; tmpIndex < tmpTotalFiles; tmpIndex++) {
          if (isCancelled()) {
            updateMessage("Scan cancelled.");
            break;
          }
          Path tmpFile = tmpFilesToScan.get(tmpIndex);
          String tmpAbsolutePath =
              tmpFile.toAbsolutePath().toString().replace("\\", "/");

          updateProgress(tmpIndex, tmpTotalFiles);
          updateMessage("Processing file (" + (tmpIndex + 1) + "/"
              + tmpTotalFiles + "): " + tmpFile.getFileName());

          // Case 1: The movie file is not yet registered in the repository
          if (!movieRepository.existsByFilePath(tmpAbsolutePath)) {
            Movie tmpMovie = parseMovieFromFile(tmpFile.toFile());

            // Check and enrich movie details using IMDb suggestion and Wikidata
            // APIs
            updateMessage("Checking IMDB for: " + tmpMovie.getTitle());
            ImdbService.enrichMovieData(tmpMovie);

            // Fetch and set a trailer URL from YouTube
            updateMessage(
                "Checking YouTube for trailer: " + tmpMovie.getTitle());
            YoutubeService.fetchYoutubeTrailer(tmpMovie);

            movieRepository.insert(tmpMovie);
            tmpNewMoviesAdded++;
            tmpLogs.add("Added: " + tmpMovie.getTitle());
            LOGGER.log(Level.INFO, "Imported movie: {0}", tmpMovie.getTitle());
          } else {
            // Case 2: The movie is already registered, but we check if it is
            // missing metadata that
            // can be enriched
            Movie tmpExistingMovie =
                movieRepository.findByFilePath(tmpAbsolutePath);
            boolean tmpNeedsEnrichment = tmpExistingMovie != null
                && (tmpExistingMovie.getCoverImagePath() == null
                    || tmpExistingMovie.getCoverImagePath().isBlank()
                    || tmpExistingMovie.getDirector() == null
                    || tmpExistingMovie.getDirector().isBlank()
                    || tmpExistingMovie.getWriter() == null
                    || tmpExistingMovie.getWriter().isBlank()
                    || tmpExistingMovie.getTrailerUrl() == null
                    || tmpExistingMovie.getTrailerUrl().isBlank());

            if (tmpNeedsEnrichment) {
              updateMessage("Checking IMDB for existing movie: "
                  + tmpExistingMovie.getTitle());
              String tmpOriginalTitle = tmpExistingMovie.getTitle();

              // Re-parse the title from the filename just in case the stored
              // title is full of
              // garbage tags
              Movie tmpFreshlyParsed = parseMovieFromFile(tmpFile.toFile());
              tmpExistingMovie.setTitle(tmpFreshlyParsed.getTitle());

              ImdbService.enrichMovieData(tmpExistingMovie);

              if (tmpExistingMovie.getTrailerUrl() == null
                  || tmpExistingMovie.getTrailerUrl().isBlank()) {
                updateMessage("Checking YouTube for trailer: "
                    + tmpExistingMovie.getTitle());
                YoutubeService.fetchYoutubeTrailer(tmpExistingMovie);
              }

              // Always update after attempting enrichment to save new fields
              // (Director, Writer,
              // etc)
              movieRepository.update(tmpExistingMovie);
              tmpLogs.add("Enriched: " + tmpExistingMovie.getTitle());
              LOGGER.log(Level.INFO, "Enriched existing movie: {0}",
                  tmpExistingMovie.getTitle());
            }
          }
        }

        updateProgress(tmpTotalFiles, tmpTotalFiles);
        updateMessage(
            "Scan complete. Added " + tmpNewMoviesAdded + " new movies.");
        return new ScanResult(tmpTotalFiles, tmpNewMoviesAdded, tmpLogs);
      }
    };
  }

  /**
   * Parses raw file attributes to initialize a Movie entity. Strips common
   * release group/encoding tags and extracts the release year from the filename
   * via regular expressions.
   *
   * @param file the video file to parse
   * @return a {@link Movie} entity populated with parsed title, year, and path
   *         properties
   */
  private Movie parseMovieFromFile(File file) {
    String tmpFilename = PathUtils.getFileNameWithoutExtension(file);

    // Clean up name: replace dots and underscores with spaces
    String tmpCleanName = tmpFilename.replace(".", " ").replace("_", " ");

    // Remove common garbage tags from release names to get a clean search query
    String[] tmpGarbageTags = {"1080p", "720p", "480p", "2160p", "4k", "bluray",
        "blu-ray", "webrip", "hdrip", "dvdrip", "brrip", "x264", "x265", "hevc",
        "aac", "ac3", "dts", "remux", "dual", "audio", "deutsch", "german",
        "english", "hd", "horrorfilm", "uncut", "extended", "director's cut",
        "directors cut"};

    for (String tmpTag : tmpGarbageTags) {
      tmpCleanName = tmpCleanName.replaceAll("(?i)\\b" + tmpTag + "\\b", "");
    }

    // Match year: matches space and 4-digit numbers in optional parentheses or
    // brackets
    Pattern tmpYearPattern =
        Pattern.compile("^(.*?)\\s*[\\(\\[]?(19\\d{2}|20\\d{2})[\\)\\]]?(.*)$");
    Matcher tmpMatcher = tmpYearPattern.matcher(tmpCleanName);

    String tmpTitle = tmpCleanName;
    Integer tmpYear = null;

    if (tmpMatcher.find()) {
      String tmpTempTitle = tmpMatcher.group(1).trim();
      if (!tmpTempTitle.isEmpty()) {
        tmpTitle = tmpTempTitle;
      }
      try {
        tmpYear = Integer.parseInt(tmpMatcher.group(2));
      } catch (NumberFormatException tmpIgnoredException) {
      }
    }

    // Remove multiple consecutive spaces and trailing hyphens
    tmpTitle = tmpTitle.replaceAll("\\s+", " ").replaceAll("-\\s*$", "").trim();
    if (tmpTitle.isEmpty()) {
      tmpTitle = tmpFilename;
    }

    Movie tmpMovie =
        new Movie(file.getAbsolutePath().replace("\\", "/"), tmpTitle);
    tmpMovie.setYear(tmpYear);
    tmpMovie.setDateAdded(Instant.now());
    return tmpMovie;
  }

  /**
   * Represents the result of a library scan operation.
   *
   * @param totalScanned the total number of files scanned in the workspace
   *        folders
   * @param newAdded the number of new movies successfully imported and added to
   *        the database
   * @param logs a list of logging/operation messages describing the files
   *        processed
   */
  public record ScanResult(int totalScanned, int newAdded, List<String> logs) {}
}
