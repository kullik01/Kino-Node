package io.github.kullik01.kinonode.repository;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.service.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryTest {

    private DatabaseService databaseService;
    private MovieRepository movieRepository;
    private WatchlistRepository watchlistRepository;
    private SettingsRepository settingsRepository;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) {
        Path dbPath = tempDir.resolve("test_kinonode.db");
        databaseService = new DatabaseService(dbPath);
        movieRepository = new MovieRepository(databaseService);
        watchlistRepository = new WatchlistRepository(databaseService, movieRepository);
        settingsRepository = new SettingsRepository(databaseService);
    }

    @AfterEach
    public void tearDown() {
        if (databaseService != null) {
            databaseService.close();
        }
    }

    @Test
    public void testMovieCrud() {
        Movie movie = new Movie("C:/movies/inception.mp4", "Inception");
        movie.setYear(2010);
        movie.setGenre("Sci-Fi");
        movie.setDescription("A thief who steals corporate secrets...");
        movie.setRating(9);

        // Insert
        Movie inserted = movieRepository.insert(movie);
        assertNotNull(inserted.getId());
        assertEquals("Inception", inserted.getTitle());

        // Find by ID
        Movie found = movieRepository.findById(inserted.getId());
        assertNotNull(found);
        assertEquals("Inception", found.getTitle());
        assertEquals(2010, found.getYear());
        assertEquals(9, found.getRating());

        // Check exists
        assertTrue(movieRepository.existsByFilePath("C:/movies/inception.mp4"));

        // Find by File Path
        Movie foundByPath = movieRepository.findByFilePath("C:/movies/inception.mp4");
        assertNotNull(foundByPath);
        assertEquals(inserted.getId(), foundByPath.getId());

        // Update
        found.setTitle("Inception: Special Edition");
        found.setRating(10);
        movieRepository.update(found);

        Movie updated = movieRepository.findById(inserted.getId());
        assertEquals("Inception: Special Edition", updated.getTitle());
        assertEquals(10, updated.getRating());

        // Delete
        movieRepository.delete(inserted.getId());
        assertNull(movieRepository.findById(inserted.getId()));
    }

    @Test
    public void testWatchlistCrud() {
        Watchlist watchlist = new Watchlist("Action Movies");
        Watchlist inserted = watchlistRepository.insert(watchlist);
        assertNotNull(inserted.getId());
        assertEquals("Action Movies", inserted.getName());

        // Insert movie
        Movie movie1 = movieRepository.insert(new Movie("C:/movies/diehard.mp4", "Die Hard"));
        Movie movie2 = movieRepository.insert(new Movie("C:/movies/matrix.mp4", "The Matrix"));

        // Add to watchlist
        watchlistRepository.addMovieToWatchlist(inserted.getId(), movie1.getId());
        watchlistRepository.addMovieToWatchlist(inserted.getId(), movie2.getId());

        // Check movies in watchlist
        List<Movie> movies = watchlistRepository.findMoviesInWatchlist(inserted.getId());
        assertEquals(2, movies.size());
        assertEquals("Die Hard", movies.get(0).getTitle());
        assertEquals("The Matrix", movies.get(1).getTitle());

        // Reorder
        watchlistRepository.updateMoviePositionsInWatchlist(inserted.getId(), List.of(movie2.getId(), movie1.getId()));
        List<Movie> reordered = watchlistRepository.findMoviesInWatchlist(inserted.getId());
        assertEquals("The Matrix", reordered.get(0).getTitle());
        assertEquals("Die Hard", reordered.get(1).getTitle());

        // Remove movie
        watchlistRepository.removeMovieFromWatchlist(inserted.getId(), movie2.getId());
        List<Movie> afterRemoval = watchlistRepository.findMoviesInWatchlist(inserted.getId());
        assertEquals(1, afterRemoval.size());
        assertEquals("Die Hard", afterRemoval.get(0).getTitle());

        // Delete watchlist
        watchlistRepository.delete(inserted.getId());
        assertNull(watchlistRepository.findById(inserted.getId()));
    }

    @Test
    public void testSettingsCrud() {
        settingsRepository.set("workspace_folders", "C:/Movies;D:/Videos");
        assertEquals("C:/Movies;D:/Videos", settingsRepository.get("workspace_folders", ""));

        settingsRepository.set("workspace_folders", "C:/NewMovies");
        assertEquals("C:/NewMovies", settingsRepository.get("workspace_folders", ""));
    }
}
