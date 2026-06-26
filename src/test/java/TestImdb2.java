import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.service.ImdbService;

public class TestImdb2 {
    public static void main(String[] args) {
        Movie movie = new Movie("C:/test/Halloween 5.mp4", "Halloween 5");
        ImdbService.enrichMovieData(movie);
        System.out.println("Title: " + movie.getTitle());
        System.out.println("Year: " + movie.getYear());
        System.out.println("Cover: " + movie.getCoverImagePath());
    }
}
