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

import java.net.URL;


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

/**
 * Service class responsible for scraping and resolving YouTube video trailer
 * links for movie records. Performs a web search query on YouTube and uses
 * regular expressions to find the first matching video ID in the HTML response.
 */
public class YoutubeService {
  private static final Logger LOGGER =
      Logger.getLogger(YoutubeService.class.getName());
  private static final HttpClient client = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL).build();

  /**
   * Searches YouTube for an English trailer for the given movie and sets the
   * trailer URL if found. Compiles a search query consisting of the movie
   * title, the release year (if available), and the phrase "english trailer".
   *
   * @param movie the {@link Movie} object for which to find and attach a
   *        YouTube trailer URL
   */
  public static void fetchYoutubeTrailer(Movie movie) {
    if (movie.getTitle() == null || movie.getTitle().isBlank())
      return;

    // Append year to query only if it's a valid year, to refine search accuracy
    String tmpYearString = movie.getYear() != null && movie.getYear() > 0
        ? String.valueOf(movie.getYear())
        : "";
    String tmpQuery =
        movie.getTitle() + " " + tmpYearString + " english trailer";

    try {
      String tmpEncodedQuery = URLEncoder
          .encode(tmpQuery, StandardCharsets.UTF_8).replace("+", "%20");
      String tmpUrlString =
          "https://www.youtube.com/results?search_query=" + tmpEncodedQuery;

      HttpRequest tmpRequest = HttpRequest.newBuilder()
          .uri(URI.create(tmpUrlString))
          // Set headers and a pre-configured consent cookie to bypass Google's
          // redirect screens
          // and scrape directly
          .header("User-Agent",
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
          .header("Accept-Language", "en-US,en;q=0.9")
          .header("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+478").GET()
          .build();

      HttpResponse<String> tmpResponse =
          client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());

      if (tmpResponse.statusCode() == 200) {
        String tmpHtml = tmpResponse.body();
        // Regex pattern to extract the first videoId field out of the script
        // block (ytInitialData)
        // in the response page
        Pattern tmpPattern =
            Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"");
        Matcher tmpMatcher = tmpPattern.matcher(tmpHtml);
        if (tmpMatcher.find()) {
          String tmpVideoId = tmpMatcher.group(1);
          movie.setTrailerUrl("https://www.youtube.com/watch?v=" + tmpVideoId);
          LOGGER.info("Found YouTube trailer for " + movie.getTitle() + ": "
              + movie.getTrailerUrl());
        }
      }
    } catch (Exception tmpException) {
      LOGGER.log(Level.WARNING,
          "Failed to fetch YouTube trailer for " + movie.getTitle(),
          tmpException);
    }
  }
}
