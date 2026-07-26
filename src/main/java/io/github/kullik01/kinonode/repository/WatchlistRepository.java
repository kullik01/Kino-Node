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

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.model.Watchlist;
import io.github.kullik01.kinonode.service.DatabaseService;
import io.github.kullik01.kinonode.model.SmartRule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access repository for managing {@link Watchlist} entities in the
 * database. Handles watchlist records and the many-to-many relationship with
 * {@link Movie} entities in the junction table `watchlist_movies` with position
 * tracking.
 */
public class WatchlistRepository {
  private static final Logger LOGGER =
      Logger.getLogger(WatchlistRepository.class.getName());
  private final DatabaseService databaseService;
  private final MovieRepository movieRepository;

  /**
   * Constructs a WatchlistRepository with the specified database service and
   * movie repository.
   *
   * @param databaseService the service providing the active database connection
   * @param movieRepository the repository for managing movies
   */
  public WatchlistRepository(DatabaseService databaseService,
      MovieRepository movieRepository) {
    this.databaseService = databaseService;
    this.movieRepository = movieRepository;
  }

  /**
   * Retrieves all watchlists stored in the database, ordered alphabetically by
   * name. The associated movies list for each watchlist is also eagerly loaded.
   *
   * @return a list of all {@link Watchlist} entities, or an empty list if none
   *         exist or on error
   */
  public List<Watchlist> findAll() {
    List<Watchlist> tmpWatchlists = new ArrayList<>();
    String tmpSql = "SELECT * FROM watchlists ORDER BY name ASC";
    try (
        Statement tmpStatement =
            databaseService.getConnection().createStatement();
        ResultSet tmpResultSet = tmpStatement.executeQuery(tmpSql)) {
      while (tmpResultSet.next()) {
        Watchlist tmpWatchlist = new Watchlist();
        tmpWatchlist.setId(tmpResultSet.getInt("id"));
        tmpWatchlist.setName(tmpResultSet.getString("name"));
        // Retrieve the timestamp as epoch seconds and map to Instant
        tmpWatchlist.setCreatedAt(
            Instant.ofEpochSecond(tmpResultSet.getLong("created_at")));

        boolean tmpIsSmart = tmpResultSet.getBoolean("is_smart");
        tmpWatchlist.setSmart(tmpIsSmart);
        tmpWatchlist
            .setSmartRulesJson(tmpResultSet.getString("smart_rules_json"));

        if (tmpIsSmart) {
          tmpWatchlist.setMovies(
              findMoviesForSmartWatchlist(tmpWatchlist.getSmartRulesJson()));
        } else {
          // Eagerly fetch and associate movies belonging to this watchlist
          tmpWatchlist.setMovies(findMoviesInWatchlist(tmpWatchlist.getId()));
        }
        tmpWatchlists.add(tmpWatchlist);
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to find all watchlists", tmpException);
    }
    return tmpWatchlists;
  }

  /**
   * Finds a single watchlist by its database identifier. The associated movies
   * list is also eagerly loaded.
   *
   * @param id the database ID of the watchlist to retrieve
   * @return the {@link Watchlist} entity, or null if not found or on error
   */
  public Watchlist findById(int id) {
    String tmpSql = "SELECT * FROM watchlists WHERE id = ?";
    try (PreparedStatement tmpPreparedStatement =
        databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpPreparedStatement.setInt(1, id);
      try (ResultSet tmpResultSet = tmpPreparedStatement.executeQuery()) {
        if (tmpResultSet.next()) {
          Watchlist tmpWatchlist = new Watchlist();
          tmpWatchlist.setId(tmpResultSet.getInt("id"));
          tmpWatchlist.setName(tmpResultSet.getString("name"));
          // Retrieve the timestamp as epoch seconds and map to Instant
          tmpWatchlist.setCreatedAt(
              Instant.ofEpochSecond(tmpResultSet.getLong("created_at")));

          boolean tmpIsSmart = tmpResultSet.getBoolean("is_smart");
          tmpWatchlist.setSmart(tmpIsSmart);
          tmpWatchlist
              .setSmartRulesJson(tmpResultSet.getString("smart_rules_json"));

          if (tmpIsSmart) {
            tmpWatchlist.setMovies(
                findMoviesForSmartWatchlist(tmpWatchlist.getSmartRulesJson()));
          } else {
            // Eagerly fetch and associate movies belonging to this watchlist
            tmpWatchlist.setMovies(findMoviesInWatchlist(id));
          }
          return tmpWatchlist;
        }
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to find watchlist by ID: " + id,
          tmpException);
    }
    return null;
  }

  /**
   * Inserts a new watchlist record into the database and updates the entity's
   * ID field with the generated key.
   *
   * @param watchlist the {@link Watchlist} entity to insert
   * @return the inserted {@link Watchlist} entity containing the generated
   *         database ID
   */
  public Watchlist insert(Watchlist watchlist) {
    String tmpSql =
        "INSERT INTO watchlists (name, created_at, is_smart, smart_rules_json) VALUES (?, ?, ?, ?)";
    try (
        PreparedStatement tmpPreparedStatement = databaseService.getConnection()
            .prepareStatement(tmpSql, Statement.RETURN_GENERATED_KEYS)) {
      tmpPreparedStatement.setString(1, watchlist.getName());
      tmpPreparedStatement.setLong(2,
          watchlist.getCreatedAt().getEpochSecond());
      tmpPreparedStatement.setBoolean(3, watchlist.isSmart());
      tmpPreparedStatement.setString(4, watchlist.getSmartRulesJson());
      int tmpAffected = tmpPreparedStatement.executeUpdate();
      // Fetch and set generated database key back on the entity if insertion
      // succeeded
      if (tmpAffected > 0) {
        try (ResultSet tmpGeneratedKeys =
            tmpPreparedStatement.getGeneratedKeys()) {
          if (tmpGeneratedKeys.next()) {
            watchlist.setId(tmpGeneratedKeys.getInt(1));
          }
        }
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE,
          "Failed to insert watchlist: " + watchlist.getName(), tmpException);
    }
    return watchlist;
  }

  /**
   * Updates an existing watchlist record's name in the database.
   *
   * @param watchlist the {@link Watchlist} entity to update
   */
  public void update(Watchlist watchlist) {
    String tmpSql =
        "UPDATE watchlists SET name = ?, is_smart = ?, smart_rules_json = ? WHERE id = ?";
    try (PreparedStatement tmpPreparedStatement =
        databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpPreparedStatement.setString(1, watchlist.getName());
      tmpPreparedStatement.setBoolean(2, watchlist.isSmart());
      tmpPreparedStatement.setString(3, watchlist.getSmartRulesJson());
      tmpPreparedStatement.setInt(4, watchlist.getId());
      tmpPreparedStatement.executeUpdate();
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE,
          "Failed to update watchlist: " + watchlist.getId(), tmpException);
    }
  }

  /**
   * Deletes a watchlist record from the database. Note that cascade deletion
   * constraints in the database schema should clean up association records.
   *
   * @param id the database ID of the watchlist to delete
   */
  public void delete(int id) {
    String tmpSql = "DELETE FROM watchlists WHERE id = ?";
    try (PreparedStatement tmpPreparedStatement =
        databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpPreparedStatement.setInt(1, id);
      tmpPreparedStatement.executeUpdate();
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to delete watchlist: " + id,
          tmpException);
    }
  }

  /**
   * Retrieves all movies belonging to a specified watchlist, ordered by their
   * display position in the watchlist.
   *
   * @param watchlistId the database ID of the watchlist
   * @return a list of {@link Movie} entities associated with the watchlist,
   *         ordered by position
   */
  public List<Movie> findMoviesInWatchlist(int watchlistId) {
    List<Movie> tmpMovies = new ArrayList<>();
    String tmpSql = """
        SELECT m.* FROM movies m
        JOIN watchlist_movies wm ON m.id = wm.movie_id
        WHERE wm.watchlist_id = ?
        ORDER BY wm.position ASC
        """;
    try (PreparedStatement tmpPreparedStatement =
        databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpPreparedStatement.setInt(1, watchlistId);
      try (ResultSet tmpResultSet = tmpPreparedStatement.executeQuery()) {
        while (tmpResultSet.next()) {
          // Map the ResultSet row to a new Movie instance locally
          Movie tmpMovie = new Movie();
          tmpMovie.setId(tmpResultSet.getInt("id"));
          tmpMovie.setFilePath(tmpResultSet.getString("file_path"));
          tmpMovie.setTitle(tmpResultSet.getString("title"));

          int tmpYear = tmpResultSet.getInt("year");
          tmpMovie.setYear(tmpResultSet.wasNull() ? null : tmpYear);

          tmpMovie.setGenre(tmpResultSet.getString("genre"));
          tmpMovie.setDescription(tmpResultSet.getString("description"));

          int tmpRating = tmpResultSet.getInt("rating");
          tmpMovie.setRating(tmpResultSet.wasNull() ? null : tmpRating);

          tmpMovie
              .setCoverImagePath(tmpResultSet.getString("cover_image_path"));
          tmpMovie.setTrailerUrl(tmpResultSet.getString("trailer_url"));
          tmpMovie.setDateAdded(
              Instant.ofEpochSecond(tmpResultSet.getLong("date_added")));
          tmpMovies.add(tmpMovie);
        }
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE,
          "Failed to find movies in watchlist: " + watchlistId, tmpException);
    }
    return tmpMovies;
  }

  /**
   * Associates a movie with a watchlist. If the association already exists, the
   * operation is skipped. The movie is appended to the end of the watchlist by
   * calculating the next sequential position.
   *
   * @param watchlistId the ID of the watchlist
   * @param movieId the ID of the movie to add
   */
  public void addMovieToWatchlist(int watchlistId, int movieId) {
    // Step 1: Check if the relationship already exists to prevent duplicate
    // entries
    String tmpCheckSql =
        "SELECT 1 FROM watchlist_movies WHERE watchlist_id = ? AND movie_id = ?";
    try (PreparedStatement tmpCheckStatement =
        databaseService.getConnection().prepareStatement(tmpCheckSql)) {
      tmpCheckStatement.setInt(1, watchlistId);
      tmpCheckStatement.setInt(2, movieId);
      try (ResultSet tmpResultSet = tmpCheckStatement.executeQuery()) {
        if (tmpResultSet.next()) {
          return // Association already exists, exit early
          ;
        }
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to check watchlist relation",
          tmpException);
    }

    // Step 2: Determine the next position value by finding the maximum current
    // position plus one.
    // If the watchlist is currently empty, we fall back to a starting position
    // of 0.
    int tmpNextPosition = 0;
    String tmpMaxSql =
        "SELECT IFNULL(MAX(position), -1) + 1 FROM watchlist_movies WHERE watchlist_id = ?";
    try (PreparedStatement tmpMaxStatement =
        databaseService.getConnection().prepareStatement(tmpMaxSql)) {
      tmpMaxStatement.setInt(1, watchlistId);
      try (ResultSet tmpResultSet = tmpMaxStatement.executeQuery()) {
        if (tmpResultSet.next()) {
          tmpNextPosition = tmpResultSet.getInt(1);
        }
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to get max position", tmpException);
    }

    // Step 3: Insert the new association with the calculated next position
    // index
    String tmpSql =
        "INSERT INTO watchlist_movies (watchlist_id, movie_id, position) VALUES (?, ?, ?)";
    try (PreparedStatement tmpPreparedStatement =
        databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpPreparedStatement.setInt(1, watchlistId);
      tmpPreparedStatement.setInt(2, movieId);
      tmpPreparedStatement.setInt(3, tmpNextPosition);
      tmpPreparedStatement.executeUpdate();
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to add movie to watchlist",
          tmpException);
    }
  }

  /**
   * Removes a movie from a watchlist. If the watchlist becomes empty as a
   * result of this removal, the watchlist itself is deleted. Otherwise, the
   * remaining movies in the watchlist are re-ordered sequentially to ensure
   * continuous position indices.
   *
   * @param watchlistId the ID of the watchlist
   * @param movieId the ID of the movie to remove
   */
  public void removeMovieFromWatchlist(int watchlistId, int movieId) {
    String tmpSql =
        "DELETE FROM watchlist_movies WHERE watchlist_id = ? AND movie_id = ?";
    try (PreparedStatement tmpPreparedStatement =
        databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpPreparedStatement.setInt(1, watchlistId);
      tmpPreparedStatement.setInt(2, movieId);
      tmpPreparedStatement.executeUpdate();

      // Fetch remaining movies in the watchlist to check if it's empty
      List<Movie> tmpRemaining = findMoviesInWatchlist(watchlistId);
      if (!tmpRemaining.isEmpty()) {
        // Re-sequence remaining movies to maintain continuous position indexes
        // [0, 1, 2...]
        List<Integer> tmpIds = tmpRemaining.stream().map(Movie::getId).toList();
        updateMoviePositionsInWatchlist(watchlistId, tmpIds);
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to remove movie from watchlist",
          tmpException);
    }
  }

  /**
   * Updates the ordering/position index of movies in a watchlist. Performs a
   * batch database update within a single transaction to ensure atomicity.
   *
   * @param watchlistId the ID of the watchlist
   * @param movieIdsInOrder a list of movie IDs in their desired order
   */
  public void updateMoviePositionsInWatchlist(int watchlistId,
      List<Integer> movieIdsInOrder) {
    String tmpSql =
        "UPDATE watchlist_movies SET position = ? WHERE watchlist_id = ? AND movie_id = ?";
    Connection tmpConnection = databaseService.getConnection();
    try {
      // Disable auto-commit to begin a new transaction manually
      tmpConnection.setAutoCommit(false);
      try (PreparedStatement tmpPreparedStatement =
          tmpConnection.prepareStatement(tmpSql)) {
        // Add update operations to the batch sequentially based on the list
        // index
        for (int tmpIndex = 0; tmpIndex < movieIdsInOrder.size(); tmpIndex++) {
          tmpPreparedStatement.setInt(1, tmpIndex);
          tmpPreparedStatement.setInt(2, watchlistId);
          tmpPreparedStatement.setInt(3, movieIdsInOrder.get(tmpIndex));
          tmpPreparedStatement.addBatch();
        }
        tmpPreparedStatement.executeBatch();
        tmpConnection.commit(); // Commit transaction if all updates succeed
      } catch (SQLException tmpException) {
        tmpConnection.rollback(); // Rollback transaction on failure to ensure
                                  // state integrity
        throw tmpException;
      } finally {
        // Restore auto-commit configuration to default behavior
        tmpConnection.setAutoCommit(true);
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE,
          "Failed to update movie positions in watchlist: " + watchlistId,
          tmpException);
    }
  }

  /**
   * Retrieves all movies dynamically matching the JSON rule criteria for a
   * Smart Watchlist.
   *
   * @param tmpRulesJson the serialized JSON rules string
   * @return a list of dynamically matched {@link Movie} entities
   */
  public List<Movie> findMoviesForSmartWatchlist(String tmpRulesJson) {
    List<Movie> tmpMovies = new ArrayList<>();
    if (tmpRulesJson == null || tmpRulesJson.isBlank()) {
      return tmpMovies;
    }

    try {
      Gson tmpGson = new Gson();
      List<SmartRule> tmpRules = tmpGson.fromJson(tmpRulesJson,
          new TypeToken<List<SmartRule>>() {}.getType());

      StringBuilder tmpQueryBuilder =
          new StringBuilder("SELECT * FROM movies WHERE 1=1 ");
      List<Object> tmpParameters = new ArrayList<>();

      for (SmartRule tmpRule : tmpRules) {
        String tmpField = tmpRule.getField().toLowerCase();
        String tmpValue = tmpRule.getValue();

        String tmpDbField = switch (tmpField) {
          case "imdb rating" -> "raw_rating";
          case "rating" -> "rating";
          case "year" -> "year";
          case "genre" -> "genre";
          case "director" -> "director";
          case "actors" -> "actors";
          case "writer" -> "writer";
          case "producer" -> "producer";
          case "cinematographer" -> "cinematographer";
          case "composer" -> "composer";
          case "duration" -> "duration";
          default -> "title";
        };

        switch (tmpRule.getOperator()) {
          case "EQUALS" -> {
            tmpQueryBuilder.append(" AND ").append(tmpDbField).append(" = ?");
            tmpParameters.add(tmpValue);
          }
          case "GREATER_THAN" -> {
            tmpQueryBuilder.append(" AND ").append(tmpDbField).append(" > ?");
            try {
              if (tmpValue.contains(".") || tmpValue.contains(",")) {
                tmpParameters
                    .add(Double.parseDouble(tmpValue.replace(",", ".")));
              } else {
                tmpParameters.add(Integer.parseInt(tmpValue));
              }
            } catch (NumberFormatException tmpException) {
              tmpParameters.add(0);
            }
          }
          case "LESS_THAN" -> {
            tmpQueryBuilder.append(" AND ").append(tmpDbField).append(" < ?");
            try {
              if (tmpValue.contains(".") || tmpValue.contains(",")) {
                tmpParameters
                    .add(Double.parseDouble(tmpValue.replace(",", ".")));
              } else {
                tmpParameters.add(Integer.parseInt(tmpValue));
              }
            } catch (NumberFormatException tmpException) {
              tmpParameters.add(0);
            }
          }
          case "CONTAINS" -> {
            tmpQueryBuilder.append(" AND ").append(tmpDbField)
                .append(" LIKE ?");
            tmpParameters.add("%" + tmpValue + "%");
          }
        }
      }

      try (PreparedStatement tmpPreparedStatement = databaseService
          .getConnection().prepareStatement(tmpQueryBuilder.toString())) {
        for (int tmpIndex = 0; tmpIndex < tmpParameters.size(); tmpIndex++) {
          tmpPreparedStatement.setObject(tmpIndex + 1,
              tmpParameters.get(tmpIndex));
        }
        try (ResultSet tmpResultSet = tmpPreparedStatement.executeQuery()) {
          while (tmpResultSet.next()) {
            Movie tmpMovie = new Movie();
            tmpMovie.setId(tmpResultSet.getInt("id"));
            tmpMovie.setFilePath(tmpResultSet.getString("file_path"));
            tmpMovie.setTitle(tmpResultSet.getString("title"));

            int tmpYear = tmpResultSet.getInt("year");
            tmpMovie.setYear(tmpResultSet.wasNull() ? null : tmpYear);

            tmpMovie.setGenre(tmpResultSet.getString("genre"));
            tmpMovie.setDescription(tmpResultSet.getString("description"));

            int tmpRating = tmpResultSet.getInt("rating");
            tmpMovie.setRating(tmpResultSet.wasNull() ? null : tmpRating);

            tmpMovie
                .setCoverImagePath(tmpResultSet.getString("cover_image_path"));
            tmpMovie.setTrailerUrl(tmpResultSet.getString("trailer_url"));
            tmpMovie.setDateAdded(
                Instant.ofEpochSecond(tmpResultSet.getLong("date_added")));
            tmpMovies.add(tmpMovie);
          }
        }
      }
    } catch (Exception tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to evaluate smart watchlist rules",
          tmpException);
    }
    return tmpMovies;
  }
}
