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

public class WatchlistRepository {
    private static final Logger LOGGER = Logger.getLogger(WatchlistRepository.class.getName());
    private final DatabaseService databaseService;
    private final MovieRepository movieRepository;

    public WatchlistRepository(DatabaseService databaseService, MovieRepository movieRepository) {
        this.databaseService = databaseService;
        this.movieRepository = movieRepository;
    }

    public List<Watchlist> findAll() {
        List<Watchlist> watchlists = new ArrayList<>();
        String sql = "SELECT * FROM watchlists ORDER BY name ASC";
        try (Statement stmt = databaseService.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Watchlist w = new Watchlist();
                w.setId(rs.getInt("id"));
                w.setName(rs.getString("name"));
                w.setCreatedAt(Instant.ofEpochSecond(rs.getLong("created_at")));
                w.setMovies(findMoviesInWatchlist(w.getId()));
                watchlists.add(w);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find all watchlists", e);
        }
        return watchlists;
    }

    public Watchlist findById(int id) {
        String sql = "SELECT * FROM watchlists WHERE id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Watchlist w = new Watchlist();
                    w.setId(rs.getInt("id"));
                    w.setName(rs.getString("name"));
                    w.setCreatedAt(Instant.ofEpochSecond(rs.getLong("created_at")));
                    w.setMovies(findMoviesInWatchlist(id));
                    return w;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find watchlist by ID: " + id, e);
        }
        return null;
    }

    public Watchlist insert(Watchlist watchlist) {
        String sql = "INSERT INTO watchlists (name, created_at) VALUES (?, ?)";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, watchlist.getName());
            stmt.setLong(2, watchlist.getCreatedAt().getEpochSecond());
            int affected = stmt.executeUpdate();
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

    public void delete(int id) {
        String sql = "DELETE FROM watchlists WHERE id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete watchlist: " + id, e);
        }
    }

    public List<Movie> findMoviesInWatchlist(int watchlistId) {
        List<Movie> movies = new ArrayList<>();
        String sql = """
            SELECT m.* FROM movies m
            JOIN watchlist_movies wm ON m.id = wm.movie_id
            WHERE wm.watchlist_id = ?
            ORDER BY wm.position ASC
            """;
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, watchlistId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Reuse mapping from MovieRepository if possible, or do it locally
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

    public void addMovieToWatchlist(int watchlistId, int movieId) {
        // Check if relationship already exists
        String checkSql = "SELECT 1 FROM watchlist_movies WHERE watchlist_id = ? AND movie_id = ?";
        try (PreparedStatement checkStmt = databaseService.getConnection().prepareStatement(checkSql)) {
            checkStmt.setInt(1, watchlistId);
            checkStmt.setInt(2, movieId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return; // Already exists
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check watchlist relation", e);
        }

        // Get max position
        int nextPosition = 0;
        String maxSql = "SELECT IFNULL(MAX(position), -1) + 1 FROM watchlist_movies WHERE watchlist_id = ?";
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

    public void removeMovieFromWatchlist(int watchlistId, int movieId) {
        String sql = "DELETE FROM watchlist_movies WHERE watchlist_id = ? AND movie_id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, watchlistId);
            stmt.setInt(2, movieId);
            stmt.executeUpdate();
            
            // Re-order remaining movies or delete if empty
            List<Movie> remaining = findMoviesInWatchlist(watchlistId);
            if (remaining.isEmpty()) {
                delete(watchlistId);
            } else {
                List<Integer> ids = remaining.stream().map(Movie::getId).toList();
                updateMoviePositionsInWatchlist(watchlistId, ids);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to remove movie from watchlist", e);
        }
    }

    public void updateMoviePositionsInWatchlist(int watchlistId, List<Integer> movieIdsInOrder) {
        String sql = "UPDATE watchlist_movies SET position = ? WHERE watchlist_id = ? AND movie_id = ?";
        Connection conn = databaseService.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < movieIdsInOrder.size(); i++) {
                    stmt.setInt(1, i);
                    stmt.setInt(2, watchlistId);
                    stmt.setInt(3, movieIdsInOrder.get(i));
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update movie positions in watchlist: " + watchlistId, e);
        }
    }
}
