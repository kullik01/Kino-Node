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
 * Service class responsible for fetching movie details from external online databases. Uses the
 * IMDb Suggestion API to retrieve basic movie information (like titles, release years, and poster
 * images) and the Wikidata SPARQL API to enrich the movie objects with additional metadata (such as
 * directors, writers, producers, composers, and cinematographers).
 */
public class ImdbService {
  private static final Logger LOGGER = Logger.getLogger(ImdbService.class.getName());
  private static final HttpClient client =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

  /**
   * Enriches a movie object with data from IMDb using the suggestion API. Downloads the cover image
   * to the local covers directory.
   *
   * @param movie the {@link Movie} object to enrich with fetched IMDb and Wikidata metadata
   */
  public static void enrichMovieData(Movie movie) {
    String query = movie.getTitle();
    if (query == null || query.isBlank()) {
      return;
    }

    try {
      // Clean query to remove special characters and reduce multiple spaces, which improves search
      // API match success rate
      String cleanedQuery =
          query.replaceAll("[^a-zA-Z0-9 äöüÄÖÜß-]", " ").trim().replaceAll("\\s+", " ");
      if (cleanedQuery.isEmpty()) return;

      String encodedQuery =
          URLEncoder.encode(cleanedQuery.toLowerCase(), StandardCharsets.UTF_8).replace("+", "%20");
      String firstLetter = cleanedQuery.substring(0, 1).toLowerCase();

      // IMDb suggestion API format: suggestion/x/query.json where 'x' is the first letter of query
      String urlStr =
          "https://v3.sg.media-imdb.com/suggestion/" + firstLetter + "/" + encodedQuery + ".json";

      HttpRequest request =
          HttpRequest.newBuilder()
              // Use standard browser headers to bypass simple bot detection blocks
              .uri(URI.create(urlStr))
              .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
              .header("Accept", "application/json")
              .header("Accept-Language", "en-US,en;q=0.5")
              .GET()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        if (jsonObject.has("d") && jsonObject.get("d").isJsonArray()) {
          JsonArray results = jsonObject.getAsJsonArray("d");
          for (JsonElement element : results) {
            JsonObject result = element.getAsJsonObject();

            // Filter results to only keep movies/series (IMDb IDs starting with 'tt')
            if (result.has("id")) {
              String id = result.get("id").getAsString();
              if (!id.startsWith("tt")) {
                continue;
              }
            } else {
              continue;
            }

            // Found a matching movie entry, start mapping metadata fields
            if (result.has("l")) {
              movie.setTitle(result.get("l").getAsString());
            }

            if (result.has("y")) {
              movie.setYear(result.get("y").getAsInt());
            }

            // Extract and download poster image if available
            if (result.has("i")) {
              JsonObject imageObj = result.getAsJsonObject("i");
              if (imageObj.has("imageUrl")) {
                String imageUrl = imageObj.get("imageUrl").getAsString();
                String localImagePath = downloadPoster(imageUrl, result.get("id").getAsString());
                if (localImagePath != null) {
                  movie.setCoverImagePath(localImagePath);
                }
              }
            }

            // Fetch additional details from Wikidata (director, writer, etc.) using the IMDb ID
            // ('tt...')
            String imdbIdStr = result.get("id").getAsString();
            fetchWikidataDetails(imdbIdStr, movie);

            LOGGER.info("Successfully fetched IMDb data for: " + movie.getTitle());
            break; // Stop after processing the first matching, highly-relevant result
          }
        }
      } else {
        LOGGER.warning("Failed to fetch IMDb data. HTTP " + response.statusCode());
      }

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error enriching movie data from IMDb for query: " + query, e);
    }
  }

  /**
   * Fetches extended details (crew information like directors, composers, etc.) from Wikidata using
   * a SPARQL query matching the IMDb identifier (P345).
   *
   * @param imdbId the IMDb identifier of the movie (starting with 'tt')
   * @param movie the {@link Movie} object to enrich with Wikidata results
   */
  private static void fetchWikidataDetails(String imdbId, Movie movie) {
    try {
      // Build a SPARQL query searching for a movie containing the specified IMDb ID (P345)
      // and optionally fetch directors, writers, producers, cinematographers, and composers.
      String sparqlQuery =
          "SELECT ?directorLabel ?writerLabel ?producerLabel ?cinematographerLabel ?composerLabel WHERE { "
              + "?movie wdt:P345 '"
              + imdbId
              + "'. "
              + "OPTIONAL { ?movie wdt:P57 ?director. } "
              + "OPTIONAL { ?movie wdt:P58 ?writer. } "
              + "OPTIONAL { ?movie wdt:P162 ?producer. } "
              + "OPTIONAL { ?movie wdt:P344 ?cinematographer. } "
              + "OPTIONAL { ?movie wdt:P86 ?composer. } "
              + "SERVICE wikibase:label { bd:serviceParam wikibase:language 'en'. } "
              + "}";

      String urlStr =
          "https://query.wikidata.org/sparql?query="
              + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8).replace("+", "%20")
              + "&format=json";

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(urlStr))
              .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KinoNode/1.0")
              .header("Accept", "application/json")
              .GET()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        if (jsonObject.has("results")) {
          JsonArray bindings = jsonObject.getAsJsonObject("results").getAsJsonArray("bindings");

          // Maintain insertion order to display crew members in order of returned relevance
          Set<String> directors = new LinkedHashSet<>();
          Set<String> writers = new LinkedHashSet<>();
          Set<String> producers = new LinkedHashSet<>();
          Set<String> cinematographers = new LinkedHashSet<>();
          Set<String> composers = new LinkedHashSet<>();

          for (JsonElement element : bindings) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("directorLabel"))
              directors.add(row.getAsJsonObject("directorLabel").get("value").getAsString());
            if (row.has("writerLabel"))
              writers.add(row.getAsJsonObject("writerLabel").get("value").getAsString());
            if (row.has("producerLabel"))
              producers.add(row.getAsJsonObject("producerLabel").get("value").getAsString());
            if (row.has("cinematographerLabel"))
              cinematographers.add(
                  row.getAsJsonObject("cinematographerLabel").get("value").getAsString());
            if (row.has("composerLabel"))
              composers.add(row.getAsJsonObject("composerLabel").get("value").getAsString());
          }

          // Format multiple values as comma-separated strings
          if (!directors.isEmpty()) movie.setDirector(String.join(", ", directors));
          if (!writers.isEmpty()) movie.setWriter(String.join(", ", writers));
          if (!producers.isEmpty()) movie.setProducer(String.join(", ", producers));
          if (!cinematographers.isEmpty())
            movie.setCinematographer(String.join(", ", cinematographers));
          if (!composers.isEmpty()) movie.setComposer(String.join(", ", composers));

          LOGGER.info("Successfully fetched Wikidata for IMDb ID: " + imdbId);
        }
      } else {
        LOGGER.warning("Failed to fetch Wikidata. HTTP " + response.statusCode());
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error fetching Wikidata for IMDb ID: " + imdbId, e);
    }
  }

  /**
   * Downloads a poster image from the given URL and saves it locally in the "covers" directory.
   *
   * @param imageUrl the remote URL of the image to download
   * @param imdbId the IMDb ID associated with the movie, used as the local filename
   * @return the absolute local file path of the downloaded image, or {@code null} if downloading
   *     failed
   */
  private static String downloadPoster(String imageUrl, String imdbId) {
    try {
      Path appDataPath = AppContext.getInstance().getDatabaseService().getDbPath().getParent();
      if (appDataPath == null) return null;

      // Ensure directory exists
      Path coversDir = appDataPath.resolve("covers");
      if (!Files.exists(coversDir)) {
        Files.createDirectories(coversDir);
      }

      // Check if cover image already exists locally to prevent redundant downloads
      Path imagePath = coversDir.resolve(imdbId + ".jpg");
      if (Files.exists(imagePath)) {
        return imagePath.toAbsolutePath().toString().replace("\\", "/");
      }

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(imageUrl))
              .header("User-Agent", "Mozilla/5.0")
              .GET()
              .build();

      HttpResponse<InputStream> response =
          client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() == 200) {
        // Stream and save image file to disk
        Files.copy(response.body(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        return imagePath.toAbsolutePath().toString().replace("\\", "/");
      }

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to download poster image: " + imageUrl, e);
    }
    return null;
  }
}
