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

public class DatabaseService {
    private static final Logger LOGGER = Logger.getLogger(DatabaseService.class.getName());
    private static final String DB_FILENAME = "kinonode.db";
    
    private final Path dbPath;
    private Connection connection;

    public DatabaseService() {
        this(resolveDefaultDbPath());
    }

    public DatabaseService(Path dbPath) {
        this.dbPath = dbPath;
        initialize();
    }

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

    private void createTables() throws SQLException {
        String createMoviesTable = """
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

        String createWatchlistsTable = """
            CREATE TABLE IF NOT EXISTS watchlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                created_at INTEGER NOT NULL
            );
            """;

        String createWatchlistMoviesTable = """
            CREATE TABLE IF NOT EXISTS watchlist_movies (
                watchlist_id INTEGER NOT NULL,
                movie_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY (watchlist_id, movie_id),
                FOREIGN KEY (watchlist_id) REFERENCES watchlists(id) ON DELETE CASCADE,
                FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
            );
            """;

        String createSettingsTable = """
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
            stmt.execute("INSERT INTO settings (key, value) VALUES ('video_extensions', '3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv') ON CONFLICT(key) DO UPDATE SET value = excluded.value;");
            
            // Schema migration for new columns
            String[] newColumns = {"director", "writer", "producer", "cinematographer", "composer"};
            for (String col : newColumns) {
                try {
                    stmt.execute("ALTER TABLE movies ADD COLUMN " + col + " TEXT;");
                    LOGGER.info("Added new column to movies: " + col);
                } catch (SQLException e) {
                    // Ignore, column likely already exists
                }
            }
            
            LOGGER.info("Database tables verified/created successfully.");
        }
    }

    public Connection getConnection() {
        try {
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

    public Path getDbPath() {
        return dbPath;
    }

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

    private static Path resolveDefaultDbPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        Path appDataDir;

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                appData = System.getProperty("user.home");
            }
            appDataDir = Paths.get(appData, "KinoNode");
        } else if (osName.contains("mac")) {
            appDataDir = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "KinoNode");
        } else {
            // Linux/Unix
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome == null || xdgDataHome.isBlank()) {
                xdgDataHome = System.getProperty("user.home") + "/.local/share";
            }
            appDataDir = Paths.get(xdgDataHome, "KinoNode");
        }
        return appDataDir.resolve(DB_FILENAME);
    }
}
