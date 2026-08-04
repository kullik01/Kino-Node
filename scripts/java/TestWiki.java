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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TestWiki {
    public static void main(String[] args) throws Exception {
        // Inception IMDb ID
        String imdbId = "tt1375666";
        String tmpSparqlQuery =
            "SELECT ?duration ?ratingVal ?criticsLabel ?directorLabel ?writerLabel ?producerLabel ?cinematographerLabel ?composerLabel ?genreLabel WHERE { "
                + "?movie wdt:P345 '" + imdbId + "'. "
                + "OPTIONAL { ?movie wdt:P2047 ?duration. } "
                + "OPTIONAL { ?movie p:P444 ?ratingStatement. ?ratingStatement ps:P444 ?ratingVal. OPTIONAL { ?ratingStatement pq:P447 ?critics. } } "
                + "OPTIONAL { ?movie wdt:P57 ?director. } "
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
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KinoNode/1.0")
                .header("Accept", "application/json").GET().build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> tmpResponse =
            client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println(tmpResponse.body());
    }
}
