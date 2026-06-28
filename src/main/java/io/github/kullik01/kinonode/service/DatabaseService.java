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
package io.github.kullik01.kinonode.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class responsible for managing the SQLite database. It handles connection creation,
 * schema setup, migration, and closing resource links. Provides the central SQL connection instance
 * to repositories and other services.
 */
public class DatabaseService {
  private static final Logger LOGGER = Logger.getLogger(DatabaseService.class.getName());
  private static final String DB_FILENAME = "kinonode.db";

  private final Path dbPath;
  private Connection connection;

  /**
   * Constructs a new DatabaseService using the default database path. The default path is
   * OS-specific and points to the user's application data directory.
   */
  public DatabaseService() {
    this(resolveDefaultDbPath());
  }

  /**
   * Constructs a new DatabaseService with a custom database file path.
   *
   * @param dbPath the {@link Path} to the SQLite database file
   */
  public DatabaseService(Path dbPath) {
    this.dbPath = dbPath;
    initialize();
  }

  /**
   * Initializes the database connection and creates tables if they do not exist. Prepares
   * directories, loads the database file, and configures connections (e.g. enabling foreign keys).
   */
  private void initialize() {
    try {
      // Ensure parent directory exists
      Path parentDir = dbPath.getParent();
      if (parentDir != null && !Files.exists(parentDir)) {
        Files.createDirectories(parentDir);
        LOGGER.info("Created app data directory: " + parentDir.toAbsolutePath());
      }

      // Connect to database
      String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString().replace("\\", "/");
      connection = DriverManager.getConnection(url);
      LOGGER.info("Connected to SQLite database at: " + dbPath.toAbsolutePath());

      // Enable Foreign Keys in SQLite
      try (Statement stmt = connection.createStatement()) {
        stmt.execute("PRAGMA foreign_keys = ON;");
      }

      createTables();
    } catch (IOException | SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
      throw new RuntimeException("Database initialization failed", e);
    }
  }

  /**
   * Verifies and creates necessary database tables if they do not exist. Inserts default
   * application settings and runs schema migrations if necessary.
   *
   * @throws SQLException if a database access error occurs or SQL statement fails
   */
  private void createTables() throws SQLException {
    String createMoviesTable =
        """
            CREATE TABLE IF NOT EXISTS movies (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_path TEXT UNIQUE NOT NULL,
                title TEXT NOT NULL,
                year INTEGER,
                genre TEXT,
                description TEXT,
                rating INTEGER,
                cover_image_path TEXT,
                trailer_url TEXT,
                director TEXT,
                writer TEXT,
                producer TEXT,
                cinematographer TEXT,
                composer TEXT,
                date_added INTEGER NOT NULL
            );
            """;

    String createWatchlistsTable =
        """
            CREATE TABLE IF NOT EXISTS watchlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                created_at INTEGER NOT NULL
            );
            """;

    String createWatchlistMoviesTable =
        """
            CREATE TABLE IF NOT EXISTS watchlist_movies (
                watchlist_id INTEGER NOT NULL,
                movie_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY (watchlist_id, movie_id),
                FOREIGN KEY (watchlist_id) REFERENCES watchlists(id) ON DELETE CASCADE,
                FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
            );
            """;

    String createSettingsTable =
        """
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT
            );
            """;

    try (Statement stmt = connection.createStatement()) {
      stmt.execute(createMoviesTable);
      stmt.execute(createWatchlistsTable);
      stmt.execute(createWatchlistMoviesTable);
      stmt.execute(createSettingsTable);

      // Populate/update default video extensions structured alphabetically
      stmt.execute(
          "INSERT INTO settings (key, value) VALUES ('video_extensions', '3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv') ON CONFLICT(key) DO UPDATE SET value = excluded.value;");

      // Schema migration for new columns
      String[] newColumns = {"director", "writer", "producer", "cinematographer", "composer"};
      for (String col : newColumns) {
        try {
          stmt.execute("ALTER TABLE movies ADD COLUMN " + col + " TEXT;");
          LOGGER.info("Added new column to movies: " + col);
        } catch (SQLException e) {
          // Ignore, column likely already exists. This avoids failure during runtime if schema is
          // already upgraded.
        }
      }

      LOGGER.info("Database tables verified/created successfully.");
    }
  }

  /**
   * Gets the active JDBC {@link Connection} to the database. Reopens the connection if it was
   * closed or not yet initialized.
   *
   * @return the SQL Connection object
   */
  public Connection getConnection() {
    try {
      // Check if connection is null or has been closed, and re-establish if needed
      if (connection == null || connection.isClosed()) {
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString().replace("\\", "/");
        connection = DriverManager.getConnection(url);
        try (Statement stmt = connection.createStatement()) {
          stmt.execute("PRAGMA foreign_keys = ON;");
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to reopen database connection", e);
    }
    return connection;
  }

  /**
   * Gets the file path of the database.
   *
   * @return the {@link Path} to the SQLite database file
   */
  public Path getDbPath() {
    return dbPath;
  }

  /** Closes the active database connection if it is open. */
  public void close() {
    if (connection != null) {
      try {
        connection.close();
        LOGGER.info("Database connection closed.");
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Failed to close database connection", e);
      }
    }
  }

  /**
   * Resolves the default path for the database file based on the user's Operating System. Supported
   * operating systems include Windows, macOS, and Linux/Unix.
   *
   * @return the resolved {@link Path} to the database file
   */
  private static Path resolveDefaultDbPath() {
    String osName = System.getProperty("os.name").toLowerCase();
    Path appDataDir;

    // Differentiate path generation strategy based on the platform OS
    if (osName.contains("win")) {
      String appData = System.getenv("APPDATA");
      if (appData == null || appData.isBlank()) {
        appData = System.getProperty("user.home");
      }
      appDataDir = Paths.get(appData, "KinoNode");
    } else if (osName.contains("mac")) {
      appDataDir =
          Paths.get(System.getProperty("user.home"), "Library", "Application Support", "KinoNode");
    } else {
      // Linux/Unix fallback path using XDG standard if available
      String xdgDataHome = System.getenv("XDG_DATA_HOME");
      if (xdgDataHome == null || xdgDataHome.isBlank()) {
        xdgDataHome = System.getProperty("user.home") + "/.local/share";
      }
      appDataDir = Paths.get(xdgDataHome, "KinoNode");
    }
    return appDataDir.resolve(DB_FILENAME);
  }
}
