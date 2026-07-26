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
import io.github.kullik01.kinonode.model.Watchlist;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service providing export and backup capabilities for the KinoNode catalog.
 * Supports exporting movies and watchlists to JSON, CSV, and formatted HTML
 * reports, as well as creating SQLite database snapshots.
 */
public class AnalyticsService {
  private static final Logger LOGGER =
      Logger.getLogger(AnalyticsService.class.getName());

  /**
   * Exports the movie catalog to a formatted JSON file.
   *
   * @param targetFile the destination path for the JSON file
   * @param movies the list of movies to export
   * @throws IOException if an I/O error occurs while writing the file
   */
  public void exportToJson(Path targetFile, List<Movie> movies)
      throws IOException {
    StringBuilder tmpBuilder = new StringBuilder();
    tmpBuilder.append("{\n  \"movies\": [\n");
    for (int i = 0; i < movies.size(); i++) {
      Movie tmpMovie = movies.get(i);
      tmpBuilder.append("    {\n");
      tmpBuilder.append("      \"id\": ").append(tmpMovie.getId())
          .append(",\n");
      tmpBuilder.append("      \"title\": ")
          .append(escapeJson(tmpMovie.getTitle())).append(",\n");
      tmpBuilder.append("      \"year\": ").append(tmpMovie.getYear())
          .append(",\n");
      tmpBuilder.append("      \"rating\": ").append(tmpMovie.getRating())
          .append(",\n");
      tmpBuilder.append("      \"duration\": ").append(tmpMovie.getDuration())
          .append(",\n");
      tmpBuilder.append("      \"genre\": ")
          .append(escapeJson(tmpMovie.getGenre())).append(",\n");
      tmpBuilder.append("      \"description\": ")
          .append(escapeJson(tmpMovie.getDescription())).append(",\n");
      tmpBuilder.append("      \"director\": ")
          .append(escapeJson(tmpMovie.getDirector())).append(",\n");
      tmpBuilder.append("      \"writer\": ")
          .append(escapeJson(tmpMovie.getWriter())).append(",\n");
      tmpBuilder.append("      \"producer\": ")
          .append(escapeJson(tmpMovie.getProducer())).append(",\n");
      tmpBuilder.append("      \"cinematographer\": ")
          .append(escapeJson(tmpMovie.getCinematographer())).append(",\n");
      tmpBuilder.append("      \"composer\": ")
          .append(escapeJson(tmpMovie.getComposer())).append(",\n");
      tmpBuilder.append("      \"actors\": ")
          .append(escapeJson(tmpMovie.getActors())).append(",\n");
      tmpBuilder.append("      \"trailerUrl\": ")
          .append(escapeJson(tmpMovie.getTrailerUrl())).append(",\n");
      tmpBuilder.append("      \"coverImagePath\": ")
          .append(escapeJson(tmpMovie.getCoverImagePath())).append(",\n");
      tmpBuilder.append("      \"filePath\": ")
          .append(escapeJson(tmpMovie.getFilePath())).append("\n");
      tmpBuilder.append("    }");
      if (i < movies.size() - 1) {
        tmpBuilder.append(",");
      }
      tmpBuilder.append("\n");
    }
    tmpBuilder.append("  ]\n}\n");
    Files.writeString(targetFile, tmpBuilder.toString(),
        StandardCharsets.UTF_8);
    LOGGER.info("Exported JSON catalog to: " + targetFile.toAbsolutePath());
  }

  /**
   * Exports the movie catalog to a spreadsheet-compatible CSV file.
   *
   * @param targetFile the destination path for the CSV file
   * @param movies the list of movies to export
   * @throws IOException if an I/O error occurs while writing the file
   */
  public void exportToCsv(Path targetFile, List<Movie> movies)
      throws IOException {
    StringBuilder tmpBuilder = new StringBuilder();
    tmpBuilder.append(
        "ID,Title,Year,Rating,Duration,Genre,Description,Director,Writer,"
            + "Producer,Cinematographer,Composer,Actors,TrailerUrl,"
            + "CoverImagePath,FilePath\n");
    for (Movie tmpMovie : movies) {
      tmpBuilder.append(tmpMovie.getId() != null ? tmpMovie.getId() : "")
          .append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getTitle())).append(",");
      tmpBuilder.append(tmpMovie.getYear() != null ? tmpMovie.getYear() : "")
          .append(",");
      tmpBuilder
          .append(tmpMovie.getRating() != null ? tmpMovie.getRating() : "")
          .append(",");
      tmpBuilder
          .append(tmpMovie.getDuration() != null ? tmpMovie.getDuration() : "")
          .append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getGenre())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getDescription())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getDirector())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getWriter())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getProducer())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getCinematographer())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getComposer())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getActors())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getTrailerUrl())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getCoverImagePath())).append(",");
      tmpBuilder.append(escapeCsv(tmpMovie.getFilePath())).append("\n");
    }
    Files.writeString(targetFile, tmpBuilder.toString(),
        StandardCharsets.UTF_8);
    LOGGER.info("Exported CSV catalog to: " + targetFile.toAbsolutePath());
  }

  /**
   * Exports the movie catalog to a standalone HTML visual report.
   *
   * @param targetFile the destination path for the HTML file
   * @param movies the list of movies to export
   * @throws IOException if an I/O error occurs while writing the file
   */
  public void exportToHtml(Path targetFile, List<Movie> movies)
      throws IOException {
    StringBuilder tmpBuilder = new StringBuilder();
    tmpBuilder.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
    tmpBuilder.append("  <meta charset=\"UTF-8\">\n");
    tmpBuilder.append("  <title>KinoNode Library Catalog</title>\n");
    tmpBuilder.append("  <style>\n");
    tmpBuilder.append(
        "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0d1117; color: #c9d1d9; margin: 40px; }\n");
    tmpBuilder.append("    h1 { color: #2178dd; }\n");
    tmpBuilder.append(
        "    table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
    tmpBuilder.append(
        "    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #30363d; }\n");
    tmpBuilder.append("    th { background: #161b22; color: #8b949e; }\n");
    tmpBuilder.append("    tr:hover { background: #21262d; }\n");
    tmpBuilder.append("    .rating { font-weight: bold; color: #ED8B00; }\n");
    tmpBuilder.append("  </style>\n</head>\n<body>\n");
    tmpBuilder.append("  <h1>🎬 KinoNode Library Report</h1>\n");
    tmpBuilder.append("  <p>Generated on ")
        .append(LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        .append(" — Total Titles: ").append(movies.size()).append("</p>\n");
    tmpBuilder.append(
        "  <table>\n    <thead>\n      <tr>\n        <th>Title</th>\n        <th>Year</th>\n        <th>Rating</th>\n        <th>Duration</th>\n        <th>Genre</th>\n        <th>Director</th>\n        <th>Writer</th>\n        <th>Producer</th>\n        <th>Cinematographer</th>\n        <th>Composer</th>\n        <th>Actors</th>\n        <th>Description</th>\n      </tr>\n    </thead>\n    <tbody>\n");
    for (Movie tmpMovie : movies) {
      tmpBuilder.append("      <tr>\n");
      tmpBuilder.append("        <td>").append(escapeHtml(tmpMovie.getTitle()))
          .append("</td>\n");
      tmpBuilder.append("        <td>")
          .append(tmpMovie.getYear() != null ? tmpMovie.getYear() : "—")
          .append("</td>\n");
      tmpBuilder.append("        <td class=\"rating\">")
          .append(
              tmpMovie.getRating() != null ? tmpMovie.getRating() + "/10" : "—")
          .append("</td>\n");
      tmpBuilder.append("        <td>")
          .append(
              tmpMovie.getDuration() != null ? tmpMovie.getDuration() + " min"
                  : "—")
          .append("</td>\n");
      tmpBuilder.append("        <td>").append(escapeHtml(tmpMovie.getGenre()))
          .append("</td>\n");
      tmpBuilder.append("        <td>")
          .append(escapeHtml(tmpMovie.getDirector())).append("</td>\n");
      tmpBuilder.append("        <td>").append(escapeHtml(tmpMovie.getWriter()))
          .append("</td>\n");
      tmpBuilder.append("        <td>")
          .append(escapeHtml(tmpMovie.getProducer())).append("</td>\n");
      tmpBuilder.append("        <td>")
          .append(escapeHtml(tmpMovie.getCinematographer())).append("</td>\n");
      tmpBuilder.append("        <td>")
          .append(escapeHtml(tmpMovie.getComposer())).append("</td>\n");
      tmpBuilder.append("        <td>").append(escapeHtml(tmpMovie.getActors()))
          .append("</td>\n");
      tmpBuilder.append("        <td>")
          .append(escapeHtml(tmpMovie.getDescription())).append("</td>\n");
      tmpBuilder.append("      </tr>\n");
    }
    tmpBuilder.append("    </tbody>\n  </table>\n</body>\n</html>\n");
    Files.writeString(targetFile, tmpBuilder.toString(),
        StandardCharsets.UTF_8);
    LOGGER.info("Exported HTML report to: " + targetFile.toAbsolutePath());
  }

  /**
   * Creates a snapshot copy of the SQLite database file.
   *
   * @param destinationFile the destination path for the database backup file
   * @throws IOException if copying fails
   */
  public void backupDatabase(Path destinationFile) throws IOException {
    Path tmpDbPath = AppContext.getInstance().getDatabaseService().getDbPath();
    if (Files.exists(tmpDbPath)) {
      Files.copy(tmpDbPath, destinationFile,
          StandardCopyOption.REPLACE_EXISTING);
      LOGGER.info(
          "Database backup created at: " + destinationFile.toAbsolutePath());
    } else {
      throw new IOException(
          "Source database file does not exist at: " + tmpDbPath);
    }
  }

  private String escapeJson(String str) {
    if (str == null) {
      return "null";
    }
    String tmpEscaped = str.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "");
    return "\"" + tmpEscaped + "\"";
  }

  private String escapeCsv(String str) {
    if (str == null) {
      return "";
    }
    String tmpValue = str.replace("\"", "\"\"");
    if (tmpValue.contains(",") || tmpValue.contains("\"")
        || tmpValue.contains("\n")) {
      return "\"" + tmpValue + "\"";
    }
    return tmpValue;
  }

  private String escapeHtml(String str) {
    if (str == null || str.isBlank()) {
      return "—";
    }
    return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
