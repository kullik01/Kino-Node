/**
 * BSD 3-Clause License
 *
 * <p>Copyright (c) 2026, Hannah Kullik
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.repository;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.service.DatabaseService;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access repository for managing {@link Watchlist} entities in the database. Handles watchlist
 * records and the many-to-many relationship with {@link Movie} entities in the junction table
 * `watchlist_movies` with position tracking.
 */
public class WatchlistRepository {
  private static final Logger LOGGER = Logger.getLogger(WatchlistRepository.class.getName());
  private final DatabaseService databaseService;
  private final MovieRepository movieRepository;

  /**
   * Constructs a WatchlistRepository with the specified database service and movie repository.
   *
   * @param databaseService the service providing the active database connection
   * @param movieRepository the repository for managing movies
   */
  public WatchlistRepository(DatabaseService databaseService, MovieRepository movieRepository) {
    this.databaseService = databaseService;
    this.movieRepository = movieRepository;
  }

  /**
   * Retrieves all watchlists stored in the database, ordered alphabetically by name. The associated
   * movies list for each watchlist is also eagerly loaded.
   *
   * @return a list of all {@link Watchlist} entities, or an empty list if none exist or on error
   */
  public List<Watchlist> findAll() {
    List<Watchlist> watchlists = new ArrayList<>();
    String sql = "SELECT * FROM watchlists ORDER BY name ASC";
    try (Statement stmt = databaseService.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        Watchlist w = new Watchlist();
        w.setId(rs.getInt("id"));
        w.setName(rs.getString("name"));
        // Retrieve the timestamp as epoch seconds and map to Instant
        w.setCreatedAt(Instant.ofEpochSecond(rs.getLong("created_at")));
        // Eagerly fetch and associate movies belonging to this watchlist
        w.setMovies(findMoviesInWatchlist(w.getId()));
        watchlists.add(w);
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to find all watchlists", e);
    }
    return watchlists;
  }

  /**
   * Finds a single watchlist by its database identifier. The associated movies list is also eagerly
   * loaded.
   *
   * @param id the database ID of the watchlist to retrieve
   * @return the {@link Watchlist} entity, or null if not found or on error
   */
  public Watchlist findById(int id) {
    String sql = "SELECT * FROM watchlists WHERE id = ?";
    try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          Watchlist w = new Watchlist();
          w.setId(rs.getInt("id"));
          w.setName(rs.getString("name"));
          // Retrieve the timestamp as epoch seconds and map to Instant
          w.setCreatedAt(Instant.ofEpochSecond(rs.getLong("created_at")));
          // Eagerly fetch and associate movies belonging to this watchlist
          w.setMovies(findMoviesInWatchlist(id));
          return w;
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to find watchlist by ID: " + id, e);
    }
    return null;
  }

  /**
   * Inserts a new watchlist record into the database and updates the entity's ID field with the
   * generated key.
   *
   * @param watchlist the {@link Watchlist} entity to insert
   * @return the inserted {@link Watchlist} entity containing the generated database ID
   */
  public Watchlist insert(Watchlist watchlist) {
    String sql = "INSERT INTO watchlists (name, created_at) VALUES (?, ?)";
    try (PreparedStatement stmt =
        databaseService.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, watchlist.getName());
      stmt.setLong(2, watchlist.getCreatedAt().getEpochSecond());
      int affected = stmt.executeUpdate();
      // Fetch and set generated database key back on the entity if insertion succeeded
      if (affected > 0) {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            watchlist.setId(generatedKeys.getInt(1));
          }
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to insert watchlist: " + watchlist.getName(), e);
    }
    return watchlist;
  }

  /**
   * Updates an existing watchlist record's name in the database.
   *
   * @param watchlist the {@link Watchlist} entity to update
   */
  public void update(Watchlist watchlist) {
    String sql = "UPDATE watchlists SET name = ? WHERE id = ?";
    try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
      stmt.setString(1, watchlist.getName());
      stmt.setInt(2, watchlist.getId());
      stmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to update watchlist: " + watchlist.getId(), e);
    }
  }

  /**
   * Deletes a watchlist record from the database. Note that cascade deletion constraints in the
   * database schema should clean up association records.
   *
   * @param id the database ID of the watchlist to delete
   */
  public void delete(int id) {
    String sql = "DELETE FROM watchlists WHERE id = ?";
    try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
      stmt.setInt(1, id);
      stmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to delete watchlist: " + id, e);
    }
  }

  /**
   * Retrieves all movies belonging to a specified watchlist, ordered by their display position in
   * the watchlist.
   *
   * @param watchlistId the database ID of the watchlist
   * @return a list of {@link Movie} entities associated with the watchlist, ordered by position
   */
  public List<Movie> findMoviesInWatchlist(int watchlistId) {
    List<Movie> movies = new ArrayList<>();
    String sql =
        """
            SELECT m.* FROM movies m
            JOIN watchlist_movies wm ON m.id = wm.movie_id
            WHERE wm.watchlist_id = ?
            ORDER BY wm.position ASC
            """;
    try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
      stmt.setInt(1, watchlistId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          // Map the ResultSet row to a new Movie instance locally
          Movie movie = new Movie();
          movie.setId(rs.getInt("id"));
          movie.setFilePath(rs.getString("file_path"));
          movie.setTitle(rs.getString("title"));

          int year = rs.getInt("year");
          movie.setYear(rs.wasNull() ? null : year);

          movie.setGenre(rs.getString("genre"));
          movie.setDescription(rs.getString("description"));

          int rating = rs.getInt("rating");
          movie.setRating(rs.wasNull() ? null : rating);

          movie.setCoverImagePath(rs.getString("cover_image_path"));
          movie.setTrailerUrl(rs.getString("trailer_url"));
          movie.setDateAdded(Instant.ofEpochSecond(rs.getLong("date_added")));
          movies.add(movie);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to find movies in watchlist: " + watchlistId, e);
    }
    return movies;
  }

  /**
   * Associates a movie with a watchlist. If the association already exists, the operation is
   * skipped. The movie is appended to the end of the watchlist by calculating the next sequential
   * position.
   *
   * @param watchlistId the ID of the watchlist
   * @param movieId the ID of the movie to add
   */
  public void addMovieToWatchlist(int watchlistId, int movieId) {
    // Step 1: Check if the relationship already exists to prevent duplicate entries
    String checkSql = "SELECT 1 FROM watchlist_movies WHERE watchlist_id = ? AND movie_id = ?";
    try (PreparedStatement checkStmt = databaseService.getConnection().prepareStatement(checkSql)) {
      checkStmt.setInt(1, watchlistId);
      checkStmt.setInt(2, movieId);
      try (ResultSet rs = checkStmt.executeQuery()) {
        if (rs.next()) {
          return; // Association already exists, exit early
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to check watchlist relation", e);
    }

    // Step 2: Determine the next position value by finding the maximum current position plus one.
    // If the watchlist is currently empty, we fall back to a starting position of 0.
    int nextPosition = 0;
    String maxSql =
        "SELECT IFNULL(MAX(position), -1) + 1 FROM watchlist_movies WHERE watchlist_id = ?";
    try (PreparedStatement maxStmt = databaseService.getConnection().prepareStatement(maxSql)) {
      maxStmt.setInt(1, watchlistId);
      try (ResultSet rs = maxStmt.executeQuery()) {
        if (rs.next()) {
          nextPosition = rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to get max position", e);
    }

    // Step 3: Insert the new association with the calculated next position index
    String sql = "INSERT INTO watchlist_movies (watchlist_id, movie_id, position) VALUES (?, ?, ?)";
    try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
      stmt.setInt(1, watchlistId);
      stmt.setInt(2, movieId);
      stmt.setInt(3, nextPosition);
      stmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to add movie to watchlist", e);
    }
  }

  /**
   * Removes a movie from a watchlist. If the watchlist becomes empty as a result of this removal,
   * the watchlist itself is deleted. Otherwise, the remaining movies in the watchlist are
   * re-ordered sequentially to ensure continuous position indices.
   *
   * @param watchlistId the ID of the watchlist
   * @param movieId the ID of the movie to remove
   */
  public void removeMovieFromWatchlist(int watchlistId, int movieId) {
    String sql = "DELETE FROM watchlist_movies WHERE watchlist_id = ? AND movie_id = ?";
    try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
      stmt.setInt(1, watchlistId);
      stmt.setInt(2, movieId);
      stmt.executeUpdate();

      // Fetch remaining movies in the watchlist to check if it's empty
      List<Movie> remaining = findMoviesInWatchlist(watchlistId);
      if (remaining.isEmpty()) {
        // If there are no movies left, delete the empty watchlist
        delete(watchlistId);
      } else {
        // Re-sequence remaining movies to maintain continuous position indexes [0, 1, 2...]
        List<Integer> ids = remaining.stream().map(Movie::getId).toList();
        updateMoviePositionsInWatchlist(watchlistId, ids);
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to remove movie from watchlist", e);
    }
  }

  /**
   * Updates the ordering/position index of movies in a watchlist. Performs a batch database update
   * within a single transaction to ensure atomicity.
   *
   * @param watchlistId the ID of the watchlist
   * @param movieIdsInOrder a list of movie IDs in their desired order
   */
  public void updateMoviePositionsInWatchlist(int watchlistId, List<Integer> movieIdsInOrder) {
    String sql = "UPDATE watchlist_movies SET position = ? WHERE watchlist_id = ? AND movie_id = ?";
    Connection conn = databaseService.getConnection();
    try {
      // Disable auto-commit to begin a new transaction manually
      conn.setAutoCommit(false);
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        // Add update operations to the batch sequentially based on the list index
        for (int i = 0; i < movieIdsInOrder.size(); i++) {
          stmt.setInt(1, i);
          stmt.setInt(2, watchlistId);
          stmt.setInt(3, movieIdsInOrder.get(i));
          stmt.addBatch();
        }
        stmt.executeBatch();
        conn.commit(); // Commit transaction if all updates succeed
      } catch (SQLException e) {
        conn.rollback(); // Rollback transaction on failure to ensure state integrity
        throw e;
      } finally {
        // Restore auto-commit configuration to default behavior
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to update movie positions in watchlist: " + watchlistId, e);
    }
  }
}
