package io.github.kullik01.kinonode.repository;

import io.github.kullik01.kinonode.model.Movie;
import io.github.kullik01.kinonode.service.DatabaseService;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MovieRepository {
    private static final Logger LOGGER = Logger.getLogger(MovieRepository.class.getName());
    private final DatabaseService databaseService;

    public MovieRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public List<Movie> findAll() {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT * FROM movies ORDER BY date_added DESC";
        try (Statement stmt = databaseService.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                movies.add(mapRowToMovie(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find all movies", e);
        }
        return movies;
    }

    public Movie findById(int id) {
        String sql = "SELECT * FROM movies WHERE id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMovie(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find movie by ID: " + id, e);
        }
        return null;
    }

    public Movie findByFilePath(String filePath) {
        String sql = "SELECT * FROM movies WHERE file_path = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setString(1, filePath);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMovie(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find movie by file path: " + filePath, e);
        }
        return null;
    }

    public boolean existsByFilePath(String filePath) {
        String sql = "SELECT 1 FROM movies WHERE file_path = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setString(1, filePath);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check movie existence by file path: " + filePath, e);
        }
        return false;
    }

    public Movie insert(Movie movie) {
        String sql = """
            INSERT INTO movies (file_path, title, year, genre, description, rating, cover_image_path, trailer_url, date_added)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, movie.getFilePath());
            stmt.setString(2, movie.getTitle());
            
            if (movie.getYear() != null) stmt.setInt(3, movie.getYear());
            else stmt.setNull(3, Types.INTEGER);
            
            stmt.setString(4, movie.getGenre());
            stmt.setString(5, movie.getDescription());
            
            if (movie.getRating() != null) stmt.setInt(6, movie.getRating());
            else stmt.setNull(6, Types.INTEGER);
            
            stmt.setString(7, movie.getCoverImagePath());
            stmt.setString(8, movie.getTrailerUrl());
            stmt.setLong(9, movie.getDateAdded().getEpochSecond());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        movie.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert movie: " + movie, e);
        }
        return movie;
    }

    public void update(Movie movie) {
        String sql = """
            UPDATE movies SET file_path = ?, title = ?, year = ?, genre = ?, description = ?, rating = ?, cover_image_path = ?, trailer_url = ?, date_added = ?
            WHERE id = ?;
            """;
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setString(1, movie.getFilePath());
            stmt.setString(2, movie.getTitle());
            
            if (movie.getYear() != null) stmt.setInt(3, movie.getYear());
            else stmt.setNull(3, Types.INTEGER);
            
            stmt.setString(4, movie.getGenre());
            stmt.setString(5, movie.getDescription());
            
            if (movie.getRating() != null) stmt.setInt(6, movie.getRating());
            else stmt.setNull(6, Types.INTEGER);
            
            stmt.setString(7, movie.getCoverImagePath());
            stmt.setString(8, movie.getTrailerUrl());
            stmt.setLong(9, movie.getDateAdded().getEpochSecond());
            stmt.setInt(10, movie.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update movie: " + movie, e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM movies WHERE id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete movie by id: " + id, e);
        }
    }

    public void deleteByFilePathPrefix(String prefix) {
        String sql = "DELETE FROM movies WHERE file_path LIKE ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete movies by file path prefix: " + prefix, e);
        }
    }

    private Movie mapRowToMovie(ResultSet rs) throws SQLException {
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
        return movie;
    }
}
