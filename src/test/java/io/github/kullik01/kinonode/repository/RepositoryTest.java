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
package io.github.kullik01.kinonode.repository;

import static org.junit.jupiter.api.Assertions.*;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.service.DatabaseService;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RepositoryTest {

  private DatabaseService databaseService;
  private MovieRepository movieRepository;
  private WatchlistRepository watchlistRepository;
  private SettingsRepository settingsRepository;

  @BeforeEach
  public void setUp(@TempDir Path tempDir) {
    Path tmpDatabasePath = tempDir.resolve("test_kinonode.db");
    databaseService = new DatabaseService(tmpDatabasePath);
    movieRepository = new MovieRepository(databaseService);
    watchlistRepository =
        new WatchlistRepository(databaseService, movieRepository);
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
    Movie tmpMovie = new Movie("C:/movies/inception.mp4", "Inception");
    tmpMovie.setYear(2010);
    tmpMovie.setGenre("Sci-Fi");
    tmpMovie.setDescription("A thief who steals corporate secrets...");
    tmpMovie.setRating(9);

    // Insert
    Movie tmpInserted = movieRepository.insert(tmpMovie);
    assertNotNull(tmpInserted.getId());
    assertEquals("Inception", tmpInserted.getTitle());

    // Find by ID
    Movie tmpFound = movieRepository.findById(tmpInserted.getId());
    assertNotNull(tmpFound);
    assertEquals("Inception", tmpFound.getTitle());
    assertEquals(2010, tmpFound.getYear());
    assertEquals(9, tmpFound.getRating());

    // Check exists
    assertTrue(movieRepository.existsByFilePath("C:/movies/inception.mp4"));

    // Find by File Path
    Movie tmpFoundByPath =
        movieRepository.findByFilePath("C:/movies/inception.mp4");
    assertNotNull(tmpFoundByPath);
    assertEquals(tmpInserted.getId(), tmpFoundByPath.getId());

    // Update
    tmpFound.setTitle("Inception: Special Edition");
    tmpFound.setRating(10);
    movieRepository.update(tmpFound);

    Movie tmpUpdated = movieRepository.findById(tmpInserted.getId());
    assertEquals("Inception: Special Edition", tmpUpdated.getTitle());
    assertEquals(10, tmpUpdated.getRating());

    // Delete
    movieRepository.delete(tmpInserted.getId());
    assertNull(movieRepository.findById(tmpInserted.getId()));
  }

  @Test
  public void testWatchlistCrud() {
    Watchlist tmpWatchlist = new Watchlist("Action Movies");
    Watchlist tmpInserted = watchlistRepository.insert(tmpWatchlist);
    assertNotNull(tmpInserted.getId());
    assertEquals("Action Movies", tmpInserted.getName());

    // Insert movie
    Movie tmpMovie1 =
        movieRepository.insert(new Movie("C:/movies/diehard.mp4", "Die Hard"));
    Movie tmpMovie2 =
        movieRepository.insert(new Movie("C:/movies/matrix.mp4", "The Matrix"));

    // Add to watchlist
    watchlistRepository.addMovieToWatchlist(tmpInserted.getId(),
        tmpMovie1.getId());
    watchlistRepository.addMovieToWatchlist(tmpInserted.getId(),
        tmpMovie2.getId());

    // Check movies in watchlist
    List<Movie> tmpMovies =
        watchlistRepository.findMoviesInWatchlist(tmpInserted.getId());
    assertEquals(2, tmpMovies.size());
    assertEquals("Die Hard", tmpMovies.get(0).getTitle());
    assertEquals("The Matrix", tmpMovies.get(1).getTitle());

    // Reorder
    watchlistRepository.updateMoviePositionsInWatchlist(tmpInserted.getId(),
        List.of(tmpMovie2.getId(), tmpMovie1.getId()));
    List<Movie> tmpReordered =
        watchlistRepository.findMoviesInWatchlist(tmpInserted.getId());
    assertEquals("The Matrix", tmpReordered.get(0).getTitle());
    assertEquals("Die Hard", tmpReordered.get(1).getTitle());

    // Remove movie
    watchlistRepository.removeMovieFromWatchlist(tmpInserted.getId(),
        tmpMovie2.getId());
    List<Movie> tmpAfterRemoval =
        watchlistRepository.findMoviesInWatchlist(tmpInserted.getId());
    assertEquals(1, tmpAfterRemoval.size());
    assertEquals("Die Hard", tmpAfterRemoval.get(0).getTitle());

    // Delete watchlist
    watchlistRepository.delete(tmpInserted.getId());
    assertNull(watchlistRepository.findById(tmpInserted.getId()));
  }

  @Test
  public void testSettingsCrud() {
    settingsRepository.set("workspace_folders", "C:/Movies;D:/Videos");
    assertEquals("C:/Movies;D:/Videos",
        settingsRepository.get("workspace_folders", ""));

    settingsRepository.set("workspace_folders", "C:/NewMovies");
    assertEquals("C:/NewMovies",
        settingsRepository.get("workspace_folders", ""));
  }
}
