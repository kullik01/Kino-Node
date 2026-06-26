package io.github.kullik01.kinonode.service;

import io.github.kullik01.kinonode.model.Movie;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ImdbServiceTest {
    @Test
    public void testEnrich() {
        Movie movie = new Movie("C:/test/Halloween 5.mp4", "Halloween 5");
        ImdbService.enrichMovieData(movie);
        System.out.println("Title: " + movie.getTitle());
        System.out.println("Year: " + movie.getYear());
        System.out.println("Cover: " + movie.getCoverImagePath());
        
        assertNotNull(movie.getYear(), "Year should not be null");
    }
}
