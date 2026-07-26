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
