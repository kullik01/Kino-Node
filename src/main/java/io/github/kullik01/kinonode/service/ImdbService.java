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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kullik01.kinonode.model.Movie;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class responsible for fetching movie details from external online
 * databases. Uses the IMDb Suggestion API to retrieve basic movie information
 * (like titles, release years, and poster images) and the Wikidata SPARQL API
 * to enrich the movie objects with additional metadata (such as directors,
 * writers, producers, composers, and cinematographers).
 */
public class ImdbService {
  private static final Logger LOGGER =
      Logger.getLogger(ImdbService.class.getName());
  private static final HttpClient client = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL).build();

  /**
   * Enriches a movie object with data from IMDb using the suggestion API.
   * Downloads the cover image to the local covers directory.
   *
   * @param movie the {@link Movie} object to enrich with fetched IMDb and
   *        Wikidata metadata
   */
  public static void enrichMovieData(Movie movie) {
    String tmpQuery = movie.getTitle();
    if (tmpQuery == null || tmpQuery.isBlank()) {
      return;
    }

    try {
      // Clean query to remove special characters and reduce multiple spaces,
      // which improves search
      // API match success rate
      String tmpCleanedQuery = tmpQuery.replaceAll("[^a-zA-Z0-9 äöüÄÖÜß-]", " ")
          .trim().replaceAll("\\s+", " ");
      if (tmpCleanedQuery.isEmpty())
        return;

      String tmpEncodedQuery = URLEncoder
          .encode(tmpCleanedQuery.toLowerCase(), StandardCharsets.UTF_8)
          .replace("+", "%20");
      String tmpFirstLetter = tmpCleanedQuery.substring(0, 1).toLowerCase();

      // IMDb suggestion API format: suggestion/x/query.json where 'x' is the
      // first letter of query
      String tmpUrlStr = "https://v3.sg.media-imdb.com/suggestion/"
          + tmpFirstLetter + "/" + tmpEncodedQuery + ".json";

      HttpRequest tmpRequest = HttpRequest.newBuilder()
          // Use standard browser headers to bypass simple bot detection blocks
          .uri(URI.create(tmpUrlStr))
          .header("User-Agent",
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
          .header("Accept", "application/json")
          .header("Accept-Language", "en-US,en;q=0.5").GET().build();

      HttpResponse<String> tmpResponse =
          client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());

      if (tmpResponse.statusCode() == 200) {
        JsonObject tmpJsonObject =
            JsonParser.parseString(tmpResponse.body()).getAsJsonObject();
        if (tmpJsonObject.has("d") && tmpJsonObject.get("d").isJsonArray()) {
          JsonArray tmpResults = tmpJsonObject.getAsJsonArray("d");
          for (JsonElement tmpElement : tmpResults) {
            JsonObject tmpResult = tmpElement.getAsJsonObject();

            // Filter results to only keep movies/series (IMDb IDs starting with
            // 'tt')
            if (tmpResult.has("id")) {
              String tmpIdentifier = tmpResult.get("id").getAsString();
              if (!tmpIdentifier.startsWith("tt")) {
                continue;
              }
            } else {
              continue;
            }

            // Found a matching movie entry, start mapping metadata fields
            if (tmpResult.has("l")) {
              movie.setTitle(tmpResult.get("l").getAsString());
            }

            if (tmpResult.has("y")) {
              movie.setYear(tmpResult.get("y").getAsInt());
            }

            // Extract and download poster image if available
            if (tmpResult.has("i")) {
              JsonObject tmpImageObj = tmpResult.getAsJsonObject("i");
              if (tmpImageObj.has("imageUrl")) {
                String tmpImageUrl = tmpImageObj.get("imageUrl").getAsString();
                String tmpLocalImagePath = downloadPoster(tmpImageUrl,
                    tmpResult.get("id").getAsString());
                if (tmpLocalImagePath != null) {
                  movie.setCoverImagePath(tmpLocalImagePath);
                }
              }
            }

            if (tmpResult.has("s")) {
              String tmpStarring = tmpResult.get("s").getAsString();
              if (tmpStarring != null && !tmpStarring.isBlank()) {
                String[] tmpActorArray = tmpStarring.split(",");
                Set<String> tmpUniqueStarring = new java.util.LinkedHashSet<>();
                for (String tmpActor : tmpActorArray) {
                  if (tmpActor != null && !tmpActor.isBlank()) {
                    tmpUniqueStarring.add(tmpActor.trim());
                  }
                }
                if (!tmpUniqueStarring.isEmpty()) {
                  movie.setActors(
                      String.join(", ", tmpUniqueStarring) + ", ...");
                }
              }
            }

            // Fetch additional details from IMDb title page (duration & rating) and Wikidata
            String tmpImdbIdentifierString = tmpResult.get("id").getAsString();
            fetchImdbTitleDetails(tmpImdbIdentifierString, movie);
            fetchWikidataDetails(tmpImdbIdentifierString, movie);

            LOGGER.info(
                "Successfully fetched IMDb data for: " + movie.getTitle());
            break; // Stop after processing the first matching, highly-relevant
                   // result
          }
        }
      } else {
        LOGGER.warning(
            "Failed to fetch IMDb data. HTTP " + tmpResponse.statusCode());
      }

    } catch (Exception tmpException) {
      LOGGER.log(Level.WARNING,
          "Error enriching movie data from IMDb for query: " + tmpQuery,
          tmpException);
    }
  }

  /**
   * Fetches title page details directly from IMDb to extract movie duration and IMDb rating.
   *
   * @param imdbId the IMDb identifier of the movie (starting with 'tt')
   * @param movie the {@link Movie} object to enrich
   */
  /**
   * Fetches title page details directly from IMDb to extract movie duration and IMDb rating.
   *
   * @param imdbId the IMDb identifier of the movie (starting with 'tt')
   * @param movie the {@link Movie} object to enrich
   */
  private static void fetchImdbTitleDetails(String imdbId, Movie movie) {
    try {
      String tmpUrl = "https://www.imdb.com/title/" + imdbId + "/";
      HttpRequest tmpRequest = HttpRequest.newBuilder()
          .uri(URI.create(tmpUrl))
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
          .header("Accept-Language", "en-US,en;q=0.9")
          .GET().build();

      HttpResponse<String> tmpResponse = client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());
      if (tmpResponse.statusCode() == 200) {
        String body = tmpResponse.body();

        // 1. ISO 8601 duration in JSON-LD e.g. "duration":"PT2H28M" or "duration":"PT148M"
        if (movie.getDuration() == null || movie.getDuration() <= 0) {
          java.util.regex.Pattern durPattern1 = java.util.regex.Pattern.compile("\"duration\"\\s*:\\s*\"PT(?:(\\d+)H)?(?:(\\d+)M)?\"");
          java.util.regex.Matcher durMatcher1 = durPattern1.matcher(body);
          if (durMatcher1.find()) {
            int hours = durMatcher1.group(1) != null ? Integer.parseInt(durMatcher1.group(1)) : 0;
            int mins = durMatcher1.group(2) != null ? Integer.parseInt(durMatcher1.group(2)) : 0;
            int totalMins = (hours * 60) + mins;
            if (totalMins > 0) {
              movie.setDuration(totalMins);
              LOGGER.info("Extracted IMDb ISO duration for " + movie.getTitle() + ": " + totalMins + " mins");
            }
          }
        }

        // 2. Next.js JSON / GraphQL runtime e.g. "runtime":{"seconds":8880}
        if (movie.getDuration() == null || movie.getDuration() <= 0) {
          java.util.regex.Pattern durPattern2 = java.util.regex.Pattern.compile("\"runtime\"\\s*:\\s*\\{\\s*\"seconds\"\\s*:\\s*(\\d+)");
          java.util.regex.Matcher durMatcher2 = durPattern2.matcher(body);
          if (durMatcher2.find()) {
            int seconds = Integer.parseInt(durMatcher2.group(1));
            int totalMins = seconds / 60;
            if (totalMins > 0) {
              movie.setDuration(totalMins);
              LOGGER.info("Extracted IMDb runtime seconds for " + movie.getTitle() + ": " + totalMins + " mins");
            }
          }
        }

        // 3. HTML text pattern e.g. "2h 28m" or "1h 45m"
        if (movie.getDuration() == null || movie.getDuration() <= 0) {
          java.util.regex.Pattern durPattern3 = java.util.regex.Pattern.compile("(\\d+)\\s*h(?:ours?)?\\s*(\\d+)\\s*m(?:in(?:ute)?s?)?");
          java.util.regex.Matcher durMatcher3 = durPattern3.matcher(body);
          if (durMatcher3.find()) {
            int hours = Integer.parseInt(durMatcher3.group(1));
            int mins = Integer.parseInt(durMatcher3.group(2));
            int totalMins = (hours * 60) + mins;
            if (totalMins > 0) {
              movie.setDuration(totalMins);
              LOGGER.info("Extracted IMDb text duration for " + movie.getTitle() + ": " + totalMins + " mins");
            }
          }
        }

        // 4. Rating parsing: JSON-LD aggregateRating ratingValue e.g. "ratingValue":8.8 or "ratingValue": "8.8"
        if (movie.getRating() == null) {
          java.util.regex.Pattern ratingPattern = java.util.regex.Pattern.compile("\"ratingValue\"\\s*:\\s*\"?([\\d\\.]+)\"?");
          java.util.regex.Matcher ratingMatcher = ratingPattern.matcher(body);
          if (ratingMatcher.find()) {
            try {
              double score = Double.parseDouble(ratingMatcher.group(1));
              int ratingInt = (int) Math.round(score);
              if (ratingInt >= 1 && ratingInt <= 10) {
                movie.setRating(ratingInt);
                LOGGER.info("Extracted IMDb rating for " + movie.getTitle() + ": " + ratingInt + "/10");
              }
            } catch (NumberFormatException ignored) {}
          }
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error fetching IMDb title page for " + imdbId, e);
    }
  }

  /**
   * Cleans a movie title string by stripping file extensions, year numbers, and video quality tags.
   *
   * @param rawTitle the raw movie title string
   * @return cleaned movie title for IMDb queries
   */
  public static String cleanMovieTitle(String rawTitle) {
    if (rawTitle == null) return "";
    String cleaned = rawTitle;
    cleaned = cleaned.replaceAll("(?i)\\.(mkv|mp4|avi|mov|wmv|flv|webm|m4v)$", "");
    cleaned = cleaned.replaceAll("[._-]", " ");
    cleaned = cleaned.replaceAll("(?i)\\b(19|20)\\d{2}\\b", "");
    cleaned = cleaned.replaceAll("(?i)\\b(1080p|720p|2160p|4k|uhd|bluray|web-?dl|h264|x264|hevc|x265|dvdrip|brrip|repack|proper)\\b", "");
    cleaned = cleaned.replaceAll("\\s+", " ").trim();
    return cleaned.isEmpty() ? rawTitle.trim() : cleaned;
  }

  /**
   * Directly fetches and fills the movie duration from IMDb (imdb.com).
   *
   * @param movie the {@link Movie} object to fetch duration for
   * @return true if duration was successfully fetched from IMDb and set, false otherwise
   */
  public static boolean fetchImdbDurationDirectly(Movie movie) {
    if (movie == null || movie.getTitle() == null || movie.getTitle().isBlank()) {
      return false;
    }
    String[] queriesToTry = new String[] {
        cleanMovieTitle(movie.getTitle()),
        movie.getTitle().trim()
    };

    for (String tmpQuery : queriesToTry) {
      if (tmpQuery.isEmpty()) continue;
      try {
        String tmpCleanedQuery = tmpQuery.replaceAll("[^a-zA-Z0-9 äöüÄÖÜß-]", " ")
            .trim().replaceAll("\\s+", " ");
        if (tmpCleanedQuery.isEmpty()) continue;

        String tmpEncodedQuery = URLEncoder
            .encode(tmpCleanedQuery.toLowerCase(), StandardCharsets.UTF_8)
            .replace("+", "%20");
        String tmpFirstLetter = tmpCleanedQuery.substring(0, 1).toLowerCase();
        String tmpUrlStr = "https://v3.sg.media-imdb.com/suggestion/"
            + tmpFirstLetter + "/" + tmpEncodedQuery + ".json";

        HttpRequest tmpRequest = HttpRequest.newBuilder()
            .uri(URI.create(tmpUrlStr))
            .header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US,en;q=0.5").GET().build();

        HttpResponse<String> tmpResponse =
            client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());

        if (tmpResponse.statusCode() == 200) {
          JsonObject tmpJsonObject =
              JsonParser.parseString(tmpResponse.body()).getAsJsonObject();
          if (tmpJsonObject.has("d") && tmpJsonObject.get("d").isJsonArray()) {
            JsonArray tmpResults = tmpJsonObject.getAsJsonArray("d");
            for (JsonElement tmpElement : tmpResults) {
              JsonObject tmpResult = tmpElement.getAsJsonObject();
              if (tmpResult.has("id")) {
                String tmpIdentifier = tmpResult.get("id").getAsString();
                if (tmpIdentifier.startsWith("tt")) {
                  fetchImdbTitleDetails(tmpIdentifier, movie);
                  if (movie.getDuration() != null && movie.getDuration() > 0) {
                    return true;
                  }
                }
              }
            }
          }
        }
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Failed to fetch IMDb duration for: " + tmpQuery, e);
      }
    }
    return movie.getDuration() != null && movie.getDuration() > 0;
  }

  /**
   * Extends details (crew information like directors, composers, etc.) from
   * Wikidata using a SPARQL query matching the IMDb identifier (P345).
   *
   * @param imdbId the IMDb identifier of the movie (starting with 'tt')
   * @param movie the {@link Movie} object to enrich with Wikidata results
   */
  private static void fetchWikidataDetails(String imdbId, Movie movie) {
    try {
      // Build a SPARQL query searching for a movie containing the specified
      // IMDb ID (P345)
      // and optionally fetch directors, writers, producers, cinematographers,
      // composers, and genres.
      String tmpSparqlQuery =
          "SELECT ?directorLabel ?writerLabel ?producerLabel ?cinematographerLabel ?composerLabel ?genreLabel WHERE { "
              + "?movie wdt:P345 '" + imdbId + "'. "
              + "OPTIONAL { ?movie wdt:P57 ?director. } "
              + "OPTIONAL { ?movie wdt:P58 ?writer. } "
              + "OPTIONAL { ?movie wdt:P162 ?producer. } "
              + "OPTIONAL { ?movie wdt:P344 ?cinematographer. } "
              + "OPTIONAL { ?movie wdt:P86 ?composer. } "
              + "OPTIONAL { ?movie wdt:P136 ?genre. } "
              + "SERVICE wikibase:label { bd:serviceParam wikibase:language 'en'. } "
              + "}";

      String tmpUrlStr = "https://query.wikidata.org/sparql?query=" + URLEncoder
          .encode(tmpSparqlQuery, StandardCharsets.UTF_8).replace("+", "%20")
          + "&format=json";

      HttpRequest tmpRequest =
          HttpRequest.newBuilder().uri(URI.create(tmpUrlStr))
              .header("User-Agent",
                  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KinoNode/1.0")
              .header("Accept", "application/json").GET().build();

      HttpResponse<String> tmpResponse =
          client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());

      if (tmpResponse.statusCode() == 200) {
        JsonObject tmpJsonObject =
            JsonParser.parseString(tmpResponse.body()).getAsJsonObject();
        if (tmpJsonObject.has("results")) {
          JsonArray tmpBindings = tmpJsonObject.getAsJsonObject("results")
              .getAsJsonArray("bindings");

          // Maintain insertion order to display crew members and genres in
          // order of
          // returned relevance
          Set<String> tmpDirectors = new LinkedHashSet<>();
          Set<String> tmpWriters = new LinkedHashSet<>();
          Set<String> tmpProducers = new LinkedHashSet<>();
          Set<String> tmpCinematographers = new LinkedHashSet<>();
          Set<String> tmpComposers = new LinkedHashSet<>();
          Set<String> tmpGenres = new LinkedHashSet<>();

          for (JsonElement tmpElement : tmpBindings) {
            JsonObject tmpRow = tmpElement.getAsJsonObject();
            if (tmpRow.has("directorLabel"))
              tmpDirectors.add(tmpRow.getAsJsonObject("directorLabel")
                  .get("value").getAsString());
            if (tmpRow.has("writerLabel"))
              tmpWriters.add(tmpRow.getAsJsonObject("writerLabel").get("value")
                  .getAsString());
            if (tmpRow.has("producerLabel"))
              tmpProducers.add(tmpRow.getAsJsonObject("producerLabel")
                  .get("value").getAsString());
            if (tmpRow.has("cinematographerLabel"))
              tmpCinematographers
                  .add(tmpRow.getAsJsonObject("cinematographerLabel")
                      .get("value").getAsString());
            if (tmpRow.has("composerLabel"))
              tmpComposers.add(tmpRow.getAsJsonObject("composerLabel")
                  .get("value").getAsString());
            if (tmpRow.has("genreLabel")) {
              String tmpGenreVal = tmpRow.getAsJsonObject("genreLabel")
                  .get("value").getAsString();
              if (tmpGenreVal != null && !tmpGenreVal.isBlank()) {
                String tmpCleaned =
                    tmpGenreVal.replaceAll("(?i)\\b(film|movie)s?\\b", "")
                        .replaceAll("\\s+", " ").trim();
                if (!tmpCleaned.isEmpty()) {
                  String[] tmpWords = tmpCleaned.split("\\s+");
                  StringBuilder tmpSb = new StringBuilder();
                  for (int tmpI = 0; tmpI < tmpWords.length; tmpI++) {
                    String tmpWord = tmpWords[tmpI];
                    if (!tmpWord.isEmpty()) {
                      tmpSb.append(Character.toUpperCase(tmpWord.charAt(0)));
                      if (tmpWord.length() > 1) {
                        tmpSb.append(tmpWord.substring(1).toLowerCase());
                      }
                      if (tmpI < tmpWords.length - 1) {
                        tmpSb.append(" ");
                      }
                    }
                  }
                  String tmpFinalGenre = tmpSb.toString().trim();
                  if (!tmpFinalGenre.isEmpty()) {
                    tmpGenres.add(tmpFinalGenre);
                  }
                }
              }
            }
          }

          // Format multiple values as comma-separated strings
          if (!tmpDirectors.isEmpty())
            movie.setDirector(String.join(", ", tmpDirectors));
          if (!tmpWriters.isEmpty())
            movie.setWriter(String.join(", ", tmpWriters));
          if (!tmpProducers.isEmpty())
            movie.setProducer(String.join(", ", tmpProducers));
          if (!tmpCinematographers.isEmpty())
            movie.setCinematographer(String.join(", ", tmpCinematographers));
          if (!tmpComposers.isEmpty())
            movie.setComposer(String.join(", ", tmpComposers));
          if (!tmpGenres.isEmpty())
            movie.setGenre(String.join(", ", tmpGenres));

          LOGGER.info("Successfully fetched Wikidata for IMDb ID: " + imdbId);
          fetchWikidataActors(imdbId, movie);
        }
      } else {
        LOGGER.warning(
            "Failed to fetch Wikidata. HTTP " + tmpResponse.statusCode());
      }
    } catch (Exception tmpException) {
      LOGGER.log(Level.WARNING,
          "Error fetching Wikidata for IMDb ID: " + imdbId, tmpException);
    }
  }

  /**
   * Fetches cast members from Wikidata for the given IMDb ID and populates
   * missing actors (actor1, actor2, actor3) on the movie.
   *
   * @param imdbId the IMDb ID (tt...)
   * @param movie the Movie object to enrich
   */
  private static void fetchWikidataActors(String imdbId, Movie movie) {
    try {
      String tmpSparqlQuery = "SELECT ?actorLabel ?ordinal WHERE { "
          + "?movie wdt:P345 '" + imdbId + "'. " + "?movie p:P161 ?statement. "
          + "?statement ps:P161 ?actor. "
          + "OPTIONAL { ?statement pq:P1545 ?ordinal. } "
          + "SERVICE wikibase:label { bd:serviceParam wikibase:language 'en'. } "
          + "} ORDER BY ASC(xsd:integer(?ordinal))";

      String tmpUrlStr = "https://query.wikidata.org/sparql?query=" + URLEncoder
          .encode(tmpSparqlQuery, StandardCharsets.UTF_8).replace("+", "%20")
          + "&format=json";

      HttpRequest tmpRequest =
          HttpRequest.newBuilder().uri(URI.create(tmpUrlStr))
              .header("User-Agent",
                  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KinoNode/1.0")
              .header("Accept", "application/json").GET().build();

      HttpResponse<String> tmpResponse =
          client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());

      if (tmpResponse.statusCode() == 200) {
        JsonObject tmpJsonObject =
            JsonParser.parseString(tmpResponse.body()).getAsJsonObject();
        if (tmpJsonObject.has("results")) {
          JsonArray tmpBindings = tmpJsonObject.getAsJsonObject("results")
              .getAsJsonArray("bindings");

          Set<String> tmpActors = new java.util.LinkedHashSet<>();
          for (JsonElement tmpElement : tmpBindings) {
            JsonObject tmpRow = tmpElement.getAsJsonObject();
            if (tmpRow.has("actorLabel")) {
              tmpActors.add(tmpRow.getAsJsonObject("actorLabel").get("value")
                  .getAsString());
            }
          }

          java.util.List<String> tmpExistingList = new java.util.ArrayList<>();
          if (movie.getActors() != null && !movie.getActors().isBlank()) {
            for (String val : movie.getActors().split(",")) {
              String trimmed = val.trim();
              if (!trimmed.isEmpty() && !trimmed.equals("...")) {
                tmpExistingList.add(trimmed);
              }
            }
          }

          for (String tmpActor : tmpActors) {
            if (tmpActor == null || tmpActor.isBlank()) {
              continue;
            }
            String tmpTrimmed = tmpActor.trim();
            if (!tmpExistingList.contains(tmpTrimmed)) {
              tmpExistingList.add(tmpTrimmed);
            }
          }

          if (!tmpExistingList.isEmpty()) {
            movie.setActors(String.join(", ", tmpExistingList) + ", ...");
          }
          LOGGER.info(
              "Successfully fetched Wikidata actors for IMDb ID: " + imdbId);
        }
      } else {
        LOGGER.warning("Failed to fetch Wikidata actors. HTTP "
            + tmpResponse.statusCode());
      }
    } catch (Exception tmpException) {
      LOGGER.log(Level.WARNING,
          "Error fetching Wikidata actors for IMDb ID: " + imdbId,
          tmpException);
    }
  }

  /**
   * Downloads a poster image from the given URL and saves it locally in the
   * "covers" directory.
   *
   * @param imageUrl the remote URL of the image to download
   * @param imdbId the IMDb ID associated with the movie, used as the local
   *        filename
   * @return the absolute local file path of the downloaded image, or
   *         {@code null} if downloading failed
   */
  private static String downloadPoster(String imageUrl, String imdbId) {
    try {
      Path tmpAppDataPath =
          AppContext.getInstance().getDatabaseService().getDbPath().getParent();
      if (tmpAppDataPath == null)
        return null;

      // Ensure directory exists
      Path tmpCoversDir = tmpAppDataPath.resolve("covers");
      if (!Files.exists(tmpCoversDir)) {
        Files.createDirectories(tmpCoversDir);
      }

      // Check if cover image already exists locally to prevent redundant
      // downloads
      Path tmpImagePath = tmpCoversDir.resolve(imdbId + ".jpg");
      if (Files.exists(tmpImagePath)) {
        return tmpImagePath.toAbsolutePath().toString().replace("\\", "/");
      }

      HttpRequest tmpRequest =
          HttpRequest.newBuilder().uri(URI.create(imageUrl))
              .header("User-Agent", "Mozilla/5.0").GET().build();

      HttpResponse<InputStream> tmpResponse =
          client.send(tmpRequest, HttpResponse.BodyHandlers.ofInputStream());

      if (tmpResponse.statusCode() == 200) {
        // Stream and save image file to disk
        Files.copy(tmpResponse.body(), tmpImagePath,
            StandardCopyOption.REPLACE_EXISTING);
        return tmpImagePath.toAbsolutePath().toString().replace("\\", "/");
      }

    } catch (Exception tmpException) {
      LOGGER.log(Level.WARNING, "Failed to download poster image: " + imageUrl,
          tmpException);
    }
    return null;
  }
}
