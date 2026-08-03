# Java Coding Conventions Reference

Comprehensive coding standards for the KinoNode project, extracted from the
existing codebase. Every convention here is enforced by the actual source files.

## Variable Naming

### Local Variables — `tmp` Prefix

**All** local variables use the `tmp` prefix. This is the project's most
distinctive convention.

```java
// ✅ Correct
String tmpQuery = "SELECT value FROM settings WHERE key = ?";
Path tmpDatabasePath = databaseService.getDbPath();
int tmpMovieCount = movieRepository.findAll().size();
double tmpSizeMegabytes = tmpSizeBytes / (1024.0 * 1024.0);
String tmpOperatingSystemName = System.getProperty("os.name");
PreparedStatement tmpPreparedStatement = connection.prepareStatement(tmpQuery);
ResultSet tmpResultSet = tmpPreparedStatement.executeQuery();

// ❌ Wrong — missing tmp prefix
String query = "SELECT ...";
Path dbPath = databaseService.getDbPath();
int count = movieRepository.findAll().size();
```

### Exception Variables

```java
catch (SQLException tmpException) {
  LOGGER.log(Level.SEVERE, "Failed to save setting: " + key, tmpException);
}
```

### Instance Fields — No Prefix

```java
private final MovieRepository movieRepository;
private final SettingsRepository settingsRepository;
private StackPane contentArea;
private Button btnLibrary;
```

### Constants

```java
private static final Logger LOGGER = Logger.getLogger(ClassName.class.getName());
private static final String DB_FILENAME = "kinonode.db";
```

### UI Element Naming

| Type | Prefix | Example |
|---|---|---|
| `Button` | `btn` | `btnSyncLibrary`, `btnGitHub`, `btnThemeToggle` |
| `Label` | `lbl` | `lblVersion`, `lblDbSize`, `lblMovieCount` |
| `TextField` | `txt` | `txtSearch` |
| `ComboBox` | `cmb` | `cmbSort` |
| `ToggleSwitch` | `btn` | `btnThemeToggle` |

## Import Rules

```java
// ✅ Correct — explicit imports only
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

// ❌ FORBIDDEN — wildcard imports
import java.sql.*;
import java.util.*;
```

## Logger Pattern

```java
// Declaration — first line inside class body
private static final Logger LOGGER =
    Logger.getLogger(ClassName.class.getName());

// Usage — info level
LOGGER.info("Database tables verified/created successfully.");
LOGGER.info("Added new column to movies: " + tmpColumn);

// Usage — error level (always attach the exception)
LOGGER.log(Level.SEVERE, "Failed to load view " + name, exception);
LOGGER.log(Level.SEVERE, "Failed to save setting: " + key, tmpException);

// Usage — warning level
LOGGER.log(Level.WARNING, "Failed to close database connection", tmpException);
```

## SQL Patterns

### Text Blocks for Multi-line SQL

```java
String tmpCreateTable = """
    CREATE TABLE IF NOT EXISTS movies (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        file_path TEXT UNIQUE NOT NULL,
        title TEXT NOT NULL,
        year INTEGER,
        date_added INTEGER NOT NULL
    );
    """;
```

### Prepared Statements

```java
String tmpQuery = "SELECT value FROM settings WHERE key = ?";
try (PreparedStatement tmpPreparedStatement =
    databaseService.getConnection().prepareStatement(tmpQuery)) {
  tmpPreparedStatement.setString(1, key);
  try (ResultSet tmpResultSet = tmpPreparedStatement.executeQuery()) {
    if (tmpResultSet.next()) {
      String tmpValue = tmpResultSet.getString("value");
      return tmpValue != null ? tmpValue : defaultValue;
    }
  }
} catch (SQLException tmpException) {
  LOGGER.log(Level.SEVERE, "Failed to load setting: " + key, tmpException);
}
```

### Upsert Pattern

```java
String tmpUpsert = """
    INSERT INTO settings (key, value) VALUES (?, ?)
    ON CONFLICT(key) DO UPDATE SET value = excluded.value;
    """;
```

## Dependency Injection

### Constructor Injection (the only allowed form)

```java
private final MovieRepository movieRepository;
private final ResourceBundle resources;

@Inject
public SomeController(MovieRepository movieRepository,
    ResourceBundle resources) {
  this.movieRepository = movieRepository;
  this.resources = resources;
}
```

### Provider Pattern for Lazy Loading

```java
private final Provider<LibraryController> libraryProvider;

@Inject
public MainController(Provider<LibraryController> libraryProvider, ...) {
  this.libraryProvider = libraryProvider;
}

// Usage — instantiated only when needed
LibraryController controller = libraryProvider.get();
```

## JavaFX UI Construction

### Programmatic View Building

```java
VBox mainContent = new VBox(25.0);
mainContent.getStyleClass().add("detail-pane");
mainContent.setPadding(new Insets(30.0, 30.0, 30.0, 30.0));

Label title = new Label(resources.getString("about.title"));
title.getStyleClass().add("title-1");
```

### Event Handlers

```java
// Lambda for simple actions
view.btnGitHub.setOnAction(e -> openRepository());

// Property change listeners
txtSearch.textProperty().addListener(
    (observable, oldValue, newValue) -> filterAndDisplayMovies());

// Toggle listeners
btnThemeToggle.selectedProperty()
    .addListener((observable, oldValue, newValue) -> {
      toggleTheme(newValue);
    });
```

### Tooltips

```java
Tooltip tmpTooltip = new Tooltip(resources.getString("tooltip.key"));
tmpTooltip.setShowDelay(Duration.millis(250));
someControl.setTooltip(tmpTooltip);
```

## Utility Classes

```java
public final class PathUtils {
  private PathUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String getFileExtension(File file) {
    // ...
  }
}
```

## Javadoc Style

```java
/**
 * Retrieves the configuration value associated with the specified key. If the
 * key does not exist or its value is null, the default value is returned.
 *
 * @param key the unique configuration key
 * @param defaultValue the default value to return if the key is not found or
 *        has a null value
 * @return the configuration value, or defaultValue if key is missing or
 *         database error occurs
 */
```

## Formatting

- **Indent:** 2 spaces (Google Java Format)
- **Line length:** ~100 characters
- **Enforced by:** `./gradlew spotlessApply`
- **Config file:** `eclipse-java-google-style.xml` at project root
