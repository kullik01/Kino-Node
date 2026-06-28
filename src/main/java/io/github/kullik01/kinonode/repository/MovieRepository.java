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
import io.github.kullik01.kinonode.service.DatabaseService;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access repository for managing {@link Movie} entities in the database. Handles database
 * operations such as retrieving, inserting, updating, and deleting movie records using standard
 * JDBC.
 */
public class MovieRepository {
  private static final Logger LOGGER = Logger.getLogger(MovieRepository.class.getName());
  private final DatabaseService databaseService;

  /**
   * Constructs a MovieRepository with the specified database service.
   *
   * @param databaseService the service providing the active database connection
   */
  public MovieRepository(DatabaseService databaseService) {
    this.databaseService = databaseService;
  }

  /**
   * Retrieves all movies stored in the database, ordered by the date they were added descending.
   *
   * @return a list of all {@link Movie} objects found, or an empty list if none exist or on error
   */
  public List<Movie> findAll() {
    List<Movie> tmpMovies = new ArrayList<>();
    String tmpSql = "SELECT * FROM movies ORDER BY date_added DESC";
    // Query the database using a simple statement since no parameters are needed
    try (Statement tmpStmt = databaseService.getConnection().createStatement();
        ResultSet tmpRs = tmpStmt.executeQuery(tmpSql)) {
      // Loop through each row of the result set and map it to a Movie object
      while (tmpRs.next()) {
        tmpMovies.add(mapRowToMovie(tmpRs));
      }
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to find all movies", tmpE);
    }
    return tmpMovies;
  }

  /**
   * Finds a single movie by its database identifier.
   *
   * @param id the database ID of the movie to find
   * @return the {@link Movie} with the specified ID, or null if not found or on error
   */
  public Movie findById(int id) {
    String tmpSql = "SELECT * FROM movies WHERE id = ?";
    // Use a prepared statement to prevent SQL injection and specify the ID parameter
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpStmt.setInt(1, id);
      try (ResultSet tmpRs = tmpStmt.executeQuery()) {
        // If a matching record is found, map it to a Movie object and return it
        if (tmpRs.next()) {
          return mapRowToMovie(tmpRs);
        }
      }
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to find movie by ID: " + id, tmpE);
    }
    return null;
  }

  /**
   * Finds a movie by its file system path.
   *
   * @param filePath the file system path of the movie to search for
   * @return the {@link Movie} with the specified file path, or null if not found or on error
   */
  public Movie findByFilePath(String filePath) {
    String tmpSql = "SELECT * FROM movies WHERE file_path = ?";
    // Locate movie based on the unique filePath parameter
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpStmt.setString(1, filePath);
      try (ResultSet tmpRs = tmpStmt.executeQuery()) {
        // Return the mapped Movie object if found
        if (tmpRs.next()) {
          return mapRowToMovie(tmpRs);
        }
      }
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to find movie by file path: " + filePath, tmpE);
    }
    return null;
  }

  /**
   * Checks if a movie with the specified file system path exists in the database.
   *
   * @param filePath the file system path to check
   * @return true if a movie with the given file path exists; false otherwise
   */
  public boolean existsByFilePath(String filePath) {
    String tmpSql = "SELECT 1 FROM movies WHERE file_path = ?";
    // Execute a lightweight query to check for existence of the file path
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpStmt.setString(1, filePath);
      try (ResultSet tmpRs = tmpStmt.executeQuery()) {
        // rs.next() will return true if at least one row matches
        return tmpRs.next();
      }
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to check movie existence by file path: " + filePath, tmpE);
    }
    return false;
  }

  /**
   * Inserts a new movie record into the database and updates the entity's ID field with the
   * database-generated key.
   *
   * @param movie the {@link Movie} entity to insert
   * @return the inserted {@link Movie} entity, potentially containing the generated ID
   */
  public Movie insert(Movie movie) {
    String tmpSql =
        """
            INSERT INTO movies (file_path, title, year, genre, description, rating, cover_image_path, trailer_url, director, writer, producer, cinematographer, composer, date_added)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;
    // Prepare statement requesting generated keys to retrieve the auto-incremented ID
    try (PreparedStatement tmpStmt =
        databaseService.getConnection().prepareStatement(tmpSql, Statement.RETURN_GENERATED_KEYS)) {
      tmpStmt.setString(1, movie.getFilePath());
      tmpStmt.setString(2, movie.getTitle());

      // Year can be null, so check and handle using Types.INTEGER if null
      if (movie.getYear() != null) tmpStmt.setInt(3, movie.getYear());
      else tmpStmt.setNull(3, Types.INTEGER);

      tmpStmt.setString(4, movie.getGenre());
      tmpStmt.setString(5, movie.getDescription());

      // Rating can be null, so check and handle using Types.INTEGER if null
      if (movie.getRating() != null) tmpStmt.setInt(6, movie.getRating());
      else tmpStmt.setNull(6, Types.INTEGER);

      tmpStmt.setString(7, movie.getCoverImagePath());
      tmpStmt.setString(8, movie.getTrailerUrl());
      tmpStmt.setString(9, movie.getDirector());
      tmpStmt.setString(10, movie.getWriter());
      tmpStmt.setString(11, movie.getProducer());
      tmpStmt.setString(12, movie.getCinematographer());
      tmpStmt.setString(13, movie.getComposer());
      // Store Instant timestamp as epoch seconds in the database
      tmpStmt.setLong(14, movie.getDateAdded().getEpochSecond());

      int tmpAffectedRows = tmpStmt.executeUpdate();
      // If the row was successfully inserted, fetch the auto-generated ID
      if (tmpAffectedRows > 0) {
        try (ResultSet tmpGeneratedKeys = tmpStmt.getGeneratedKeys()) {
          if (tmpGeneratedKeys.next()) {
            movie.setId(tmpGeneratedKeys.getInt(1));
          }
        }
      }
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to insert movie: " + movie, tmpE);
    }
    return movie;
  }

  /**
   * Updates an existing movie record in the database.
   *
   * @param movie the {@link Movie} entity with updated values to save
   */
  public void update(Movie movie) {
    String tmpSql =
        """
            UPDATE movies SET file_path = ?, title = ?, year = ?, genre = ?, description = ?, rating = ?, cover_image_path = ?, trailer_url = ?, director = ?, writer = ?, producer = ?, cinematographer = ?, composer = ?, date_added = ?
            WHERE id = ?;
            """;
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpStmt.setString(1, movie.getFilePath());
      tmpStmt.setString(2, movie.getTitle());

      // Year can be null, check and handle using Types.INTEGER if null
      if (movie.getYear() != null) tmpStmt.setInt(3, movie.getYear());
      else tmpStmt.setNull(3, Types.INTEGER);

      tmpStmt.setString(4, movie.getGenre());
      tmpStmt.setString(5, movie.getDescription());

      // Rating can be null, check and handle using Types.INTEGER if null
      if (movie.getRating() != null) tmpStmt.setInt(6, movie.getRating());
      else tmpStmt.setNull(6, Types.INTEGER);

      tmpStmt.setString(7, movie.getCoverImagePath());
      tmpStmt.setString(8, movie.getTrailerUrl());
      tmpStmt.setString(9, movie.getDirector());
      tmpStmt.setString(10, movie.getWriter());
      tmpStmt.setString(11, movie.getProducer());
      tmpStmt.setString(12, movie.getCinematographer());
      tmpStmt.setString(13, movie.getComposer());
      // Store Instant timestamp as epoch seconds in the database
      tmpStmt.setLong(14, movie.getDateAdded().getEpochSecond());
      tmpStmt.setInt(15, movie.getId());

      tmpStmt.executeUpdate();
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to update movie: " + movie, tmpE);
    }
  }

  /**
   * Deletes a movie record from the database by its identifier.
   *
   * @param id the database ID of the movie to delete
   */
  public void delete(int id) {
    String tmpSql = "DELETE FROM movies WHERE id = ?";
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpSql)) {
      tmpStmt.setInt(1, id);
      tmpStmt.executeUpdate();
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to delete movie by id: " + id, tmpE);
    }
  }

  /**
   * Deletes all movie records whose file paths start with a specified prefix. This is useful for
   * clearing movies belonging to a deleted directory.
   *
   * @param prefix the file path prefix to match
   */
  public void deleteByFilePathPrefix(String prefix) {
    String tmpSql = "DELETE FROM movies WHERE file_path LIKE ?";
    try (PreparedStatement tmpStmt = databaseService.getConnection().prepareStatement(tmpSql)) {
      // Append wildcard % to check for any files starting with the prefix
      tmpStmt.setString(1, prefix + "%");
      tmpStmt.executeUpdate();
    } catch (SQLException tmpE) {
      LOGGER.log(Level.SEVERE, "Failed to delete movies by file path prefix: " + prefix, tmpE);
    }
  }

  /**
   * Maps a single row from a database ResultSet to a Movie object. Handles retrieving nullable
   * integer values (year, rating) by checking wasNull().
   *
   * @param rs the ResultSet containing the database row
   * @return a mapped {@link Movie} object
   * @throws SQLException if a database access error occurs
   */
  private Movie mapRowToMovie(ResultSet rs) throws SQLException {
    Movie tmpMovie = new Movie();
    tmpMovie.setId(rs.getInt("id"));
    tmpMovie.setFilePath(rs.getString("file_path"));
    tmpMovie.setTitle(rs.getString("title"));

    // Year is optional and can be null; rs.getInt returning 0 requires rs.wasNull check
    int tmpYear = rs.getInt("year");
    tmpMovie.setYear(rs.wasNull() ? null : tmpYear);

    tmpMovie.setGenre(rs.getString("genre"));
    tmpMovie.setDescription(rs.getString("description"));

    // Rating is optional and can be null; rs.getInt returning 0 requires rs.wasNull check
    int tmpRating = rs.getInt("rating");
    tmpMovie.setRating(rs.wasNull() ? null : tmpRating);

    tmpMovie.setCoverImagePath(rs.getString("cover_image_path"));
    tmpMovie.setTrailerUrl(rs.getString("trailer_url"));
    tmpMovie.setDirector(rs.getString("director"));
    tmpMovie.setWriter(rs.getString("writer"));
    tmpMovie.setProducer(rs.getString("producer"));
    tmpMovie.setCinematographer(rs.getString("cinematographer"));
    tmpMovie.setComposer(rs.getString("composer"));
    // Re-construct the Instant object using the epoch second retrieved from the database
    tmpMovie.setDateAdded(Instant.ofEpochSecond(rs.getLong("date_added")));
    return tmpMovie;
  }
}
