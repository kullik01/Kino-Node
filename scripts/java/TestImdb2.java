import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestImdb2 {
    public static void main(String[] args) throws Exception {
        String tmpUrl = "https://www.imdb.com/title/tt1375666/";
        HttpRequest tmpRequest = HttpRequest.newBuilder().uri(URI.create(tmpUrl))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9").GET().build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> tmpResponse =
            client.send(tmpRequest, HttpResponse.BodyHandlers.ofString());

        String body = tmpResponse.body();
        if (body.contains("ratingValue")) {
            System.out.println("HAS ratingValue");
        } else {
            System.out.println("NO ratingValue");
        }
        if (body.contains("aggregateRating")) {
            System.out.println("HAS aggregateRating");
        } else {
            System.out.println("NO aggregateRating");
        }
    }
}
