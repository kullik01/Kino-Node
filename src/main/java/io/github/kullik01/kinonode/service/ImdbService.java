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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImdbService {
    private static final Logger LOGGER = Logger.getLogger(ImdbService.class.getName());
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Enriches a movie object with data from IMDb using the suggestion API.
     * Downloads the cover image to the local covers directory.
     */
    public static void enrichMovieData(Movie movie) {
        String query = movie.getTitle();
        if (query == null || query.isBlank()) {
            return;
        }

        try {
            // Clean query to improve search
            String cleanedQuery = query.replaceAll("[^a-zA-Z0-9 äöüÄÖÜß-]", " ").trim().replaceAll("\\s+", " ");
            if (cleanedQuery.isEmpty()) return;

            String encodedQuery = URLEncoder.encode(cleanedQuery.toLowerCase(), StandardCharsets.UTF_8).replace("+", "%20");
            String firstLetter = cleanedQuery.substring(0, 1).toLowerCase();
            // IMDb suggestion API format: suggestion/x/query.json
            String urlStr = "https://v3.sg.media-imdb.com/suggestion/" + firstLetter + "/" + encodedQuery + ".json";

            HttpRequest request = HttpRequest.newBuilder()
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
                        
                        // We want movies/series (skip actors which start with 'nm')
                        if (result.has("id")) {
                            String id = result.get("id").getAsString();
                            if (!id.startsWith("tt")) {
                                continue;
                            }
                        } else {
                            continue;
                        }

                        // Found a matching movie
                        if (result.has("l")) {
                            movie.setTitle(result.get("l").getAsString());
                        }

                        if (result.has("y")) {
                            movie.setYear(result.get("y").getAsInt());
                        }

                        // Get poster image
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
                        
                        // IMDb Suggestion API also returns "s" which contains actor names.
                        // But we don't have an actors field, maybe append to description? Or leave out.
                        
                        LOGGER.info("Successfully fetched IMDb data for: " + movie.getTitle());
                        break; // Only use the top result
                    }
                }
            } else {
                LOGGER.warning("Failed to fetch IMDb data. HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error enriching movie data from IMDb for query: " + query, e);
        }
    }

    private static String downloadPoster(String imageUrl, String imdbId) {
        try {
            Path appDataPath = AppContext.getInstance().getDatabaseService().getDbPath().getParent();
            if (appDataPath == null) return null;

            Path coversDir = appDataPath.resolve("covers");
            if (!Files.exists(coversDir)) {
                Files.createDirectories(coversDir);
            }

            Path imagePath = coversDir.resolve(imdbId + ".jpg");
            if (Files.exists(imagePath)) {
                return imagePath.toAbsolutePath().toString().replace("\\", "/");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                Files.copy(response.body(), imagePath, StandardCopyOption.REPLACE_EXISTING);
                return imagePath.toAbsolutePath().toString().replace("\\", "/");
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to download poster image: " + imageUrl, e);
        }
        return null;
    }
}
