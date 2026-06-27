package io.github.kullik01.kinonode.service;

import io.github.kullik01.kinonode.model.Movie;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeService {
    private static final Logger LOGGER = Logger.getLogger(YoutubeService.class.getName());
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Searches YouTube for an English trailer for the given movie and sets the trailer URL if found.
     */
    public static void fetchYoutubeTrailer(Movie movie) {
        if (movie.getTitle() == null || movie.getTitle().isBlank()) return;
        
        String yearStr = movie.getYear() != null && movie.getYear() > 0 ? String.valueOf(movie.getYear()) : "";
        String query = movie.getTitle() + " " + yearStr + " english trailer";
        
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
            String urlStr = "https://www.youtube.com/results?search_query=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+478")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String html = response.body();
                // Find the first videoId in ytInitialData
                Pattern pattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"");
                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    String videoId = matcher.group(1);
                    movie.setTrailerUrl("https://www.youtube.com/watch?v=" + videoId);
                    LOGGER.info("Found YouTube trailer for " + movie.getTitle() + ": " + movie.getTrailerUrl());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch YouTube trailer for " + movie.getTitle(), e);
        }
    }
}
