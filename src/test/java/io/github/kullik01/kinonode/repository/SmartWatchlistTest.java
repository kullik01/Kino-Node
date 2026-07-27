package io.github.kullik01.kinonode.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.service.DatabaseService;
import io.github.kullik01.kinonode.model.SmartRule;
import com.google.gson.Gson;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SmartWatchlistTest {
  private DatabaseService databaseService;
  private MovieRepository movieRepository;
  private WatchlistRepository watchlistRepository;

  @BeforeEach
  public void setUp(@TempDir Path tempDir) {
    Path tmpDatabasePath = tempDir.resolve("test_kinonode_smart.db");
    databaseService = new DatabaseService(tmpDatabasePath);
    movieRepository = new MovieRepository(databaseService);
    watchlistRepository =
        new WatchlistRepository(databaseService, movieRepository);
  }

  @AfterEach
  public void tearDown() {
    if (databaseService != null) {
      databaseService.close();
    }
  }

  @Test
  public void testSmartWatchlist() {
    Movie m1 = new Movie("C:/movies/inception.mp4", "Inception");
    m1.setYear(2010);
    movieRepository.insert(m1);

    Movie m2 = new Movie("C:/movies/matrix.mp4", "The Matrix");
    m2.setYear(1999);
    movieRepository.insert(m2);

    Watchlist w = new Watchlist("2010 Movies");
    w.setSmart(true);
    SmartRule r = new SmartRule("Year", "EQUALS", "2010");
    w.setSmartRulesJson(new Gson().toJson(List.of(r)));

    Watchlist inserted = watchlistRepository.insert(w);

    Watchlist found = watchlistRepository.findById(inserted.getId());
    assertNotNull(found);
    assertTrue(found.isSmart());
    assertEquals(1, found.getMovies().size());
    assertEquals("Inception", found.getMovies().get(0).getTitle());
  }
}
