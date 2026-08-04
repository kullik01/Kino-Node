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
 * Service class responsible for managing the SQLite database. It handles
 * connection creation, schema setup, migration, and closing resource links.
 * Provides the central SQL connection instance to repositories and other
 * services.
 */
public class DatabaseService {
  private static final Logger LOGGER =
      Logger.getLogger(DatabaseService.class.getName());
  private static final String DB_FILENAME = "kinonode.db";

  private final Path dbPath;
  private Connection connection;

  /**
   * Constructs a new DatabaseService using the default database path. The
   * default path is OS-specific and points to the user's application data
   * directory.
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
   * Initializes the database connection and creates tables if they do not
   * exist. Prepares directories, loads the database file, and configures
   * connections (e.g. enabling foreign keys).
   */
  private void initialize() {
    try {
      // Ensure parent directory exists
      Path tmpParentDir = dbPath.getParent();
      if (tmpParentDir != null && !Files.exists(tmpParentDir)) {
        Files.createDirectories(tmpParentDir);
        LOGGER.info(
            "Created app data directory: " + tmpParentDir.toAbsolutePath());
      }

      // Connect to database
      String tmpUrl = "jdbc:sqlite:"
          + dbPath.toAbsolutePath().toString().replace("\\", "/");
      connection = DriverManager.getConnection(tmpUrl);
      LOGGER
          .info("Connected to SQLite database at: " + dbPath.toAbsolutePath());

      // Enable Foreign Keys in SQLite
      try (Statement tmpStatement = connection.createStatement()) {
        tmpStatement.execute("PRAGMA foreign_keys = ON;");
      }

      createTables();
    } catch (IOException | SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to initialize database", tmpException);
      throw new RuntimeException("Database initialization failed",
          tmpException);
    }
  }

  /**
   * Verifies and creates necessary database tables if they do not exist.
   * Inserts default application settings and runs schema migrations if
   * necessary.
   *
   * @throws SQLException if a database access error occurs or SQL statement
   *         fails
   */
  private void createTables() throws SQLException {
    String tmpCreateMoviesTable = """
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
            actors TEXT,
            duration INTEGER,
            date_added INTEGER NOT NULL
        );
        """;

    String tmpCreateWatchlistsTable = """
        CREATE TABLE IF NOT EXISTS watchlists (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            created_at INTEGER NOT NULL
        );
        """;

    String tmpCreateWatchlistMoviesTable =
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

    String tmpCreateSettingsTable = """
        CREATE TABLE IF NOT EXISTS settings (
            key TEXT PRIMARY KEY,
            value TEXT
        );
        """;

    try (Statement tmpStatement = connection.createStatement()) {
      tmpStatement.execute(tmpCreateMoviesTable);
      tmpStatement.execute(tmpCreateWatchlistsTable);
      tmpStatement.execute(tmpCreateWatchlistMoviesTable);
      tmpStatement.execute(tmpCreateSettingsTable);

      // Populate/update default video extensions structured alphabetically
      tmpStatement.execute(
          "INSERT INTO settings (key, value) VALUES ('video_extensions', '3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv') ON CONFLICT(key) DO UPDATE SET value = excluded.value;");

      // Schema migration for new columns
      String[] tmpNewColumns =
          {"director", "writer", "producer", "cinematographer", "composer",
              "actors", "duration", "raw_rating", "match_status"};
      for (String tmpColumn : tmpNewColumns) {
        try {
          String columnType =
              tmpColumn.equals("duration") ? "INTEGER"
                  : (tmpColumn.equals("raw_rating") ? "REAL"
                      : (tmpColumn.equals("match_status")
                          ? "TEXT DEFAULT 'MATCHED'"
                          : "TEXT"));
          tmpStatement.execute("ALTER TABLE movies ADD COLUMN " + tmpColumn
              + " " + columnType + ";");
          LOGGER.info("Added new column to movies: " + tmpColumn);
        } catch (SQLException tmpException) {
          // Ignore, column likely already exists.
        }
      }

      // Schema migration for smart watchlists
      String[] tmpWatchlistColumns = {"is_smart", "smart_rules_json"};
      for (String tmpColumn : tmpWatchlistColumns) {
        try {
          String columnType =
              tmpColumn.equals("is_smart") ? "BOOLEAN DEFAULT 0" : "TEXT";
          tmpStatement.execute("ALTER TABLE watchlists ADD COLUMN " + tmpColumn
              + " " + columnType + ";");
          LOGGER.info("Added new column to watchlists: " + tmpColumn);
        } catch (SQLException tmpException) {
          // Ignore, column likely already exists.
        }
      }

      // Performance Optimization: Create Indexes for faster Smart Watchlist
      // querying
      tmpStatement.execute(
          "CREATE INDEX IF NOT EXISTS idx_movies_year ON movies(year);");
      tmpStatement.execute(
          "CREATE INDEX IF NOT EXISTS idx_movies_rating ON movies(rating);");
      tmpStatement.execute(
          "CREATE INDEX IF NOT EXISTS idx_movies_raw_rating ON movies(raw_rating);");
      tmpStatement.execute(
          "CREATE INDEX IF NOT EXISTS idx_movies_genre ON movies(genre);");

      LOGGER.info("Database tables verified/created successfully.");
    }
  }

  /**
   * Gets the active JDBC {@link Connection} to the database. Reopens the
   * connection if it was closed or not yet initialized.
   *
   * @return the SQL Connection object
   */
  public Connection getConnection() {
    try {
      // Check if connection is null or has been closed, and re-establish if
      // needed
      if (connection == null || connection.isClosed()) {
        String tmpUrl = "jdbc:sqlite:"
            + dbPath.toAbsolutePath().toString().replace("\\", "/");
        connection = DriverManager.getConnection(tmpUrl);
        try (Statement tmpStatement = connection.createStatement()) {
          tmpStatement.execute("PRAGMA foreign_keys = ON;");
        }
      }
    } catch (SQLException tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to reopen database connection",
          tmpException);
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
      } catch (SQLException tmpException) {
        LOGGER.log(Level.WARNING, "Failed to close database connection",
            tmpException);
      }
    }
  }

  /**
   * Resolves the default path for the database file based on the user's
   * Operating System. Supported operating systems include Windows, macOS, and
   * Linux/Unix.
   *
   * @return the resolved {@link Path} to the database file
   */
  private static Path resolveDefaultDbPath() {
    // 1. Check custom system property override
    String tmpCustomHome = System.getProperty("kinonode.home");
    if (tmpCustomHome != null && !tmpCustomHome.isBlank()) {
      return Paths.get(tmpCustomHome, DB_FILENAME);
    }

    // 2. Check if local kinonode.db already exists in current directory
    // (portable mode)
    Path tmpLocalDb = Paths.get(System.getProperty("user.dir"), DB_FILENAME);
    if (Files.exists(tmpLocalDb)) {
      return tmpLocalDb;
    }

    // 3. Check single directory mode: if running inside a directory named
    // KinoNode
    Path tmpCurrentDir = Paths.get(System.getProperty("user.dir"));
    if (tmpCurrentDir.getFileName() != null && tmpCurrentDir.getFileName()
        .toString().equalsIgnoreCase("KinoNode")) {
      return tmpCurrentDir.resolve(DB_FILENAME);
    }

    // 4. Fallback to OS-specific user application data directory
    String tmpOperatingSystemName = System.getProperty("os.name").toLowerCase();
    Path tmpAppDataDir;

    // Differentiate path generation strategy based on the platform OS
    if (tmpOperatingSystemName.contains("win")) {
      String tmpAppData = System.getenv("APPDATA");
      if (tmpAppData == null || tmpAppData.isBlank()) {
        tmpAppData = System.getProperty("user.home");
      }
      tmpAppDataDir = Paths.get(tmpAppData, "KinoNode");
    } else if (tmpOperatingSystemName.contains("mac")) {
      tmpAppDataDir = Paths.get(System.getProperty("user.home"), "Library",
          "Application Support", "KinoNode");
    } else {
      // Linux/Unix fallback path using XDG standard if available
      String tmpXdgDataHome = System.getenv("XDG_DATA_HOME");
      if (tmpXdgDataHome == null || tmpXdgDataHome.isBlank()) {
        tmpXdgDataHome = System.getProperty("user.home") + "/.local/share";
      }
      tmpAppDataDir = Paths.get(tmpXdgDataHome, "KinoNode");
    }
    return tmpAppDataDir.resolve(DB_FILENAME);
  }
}
