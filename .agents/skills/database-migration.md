# Skill: SQLite Database Schema Migration

This guide covers evolving the SQLite schema in KinoNode. There is no migration
framework — all schema changes are managed manually in `DatabaseService.createTables()`.

## Target File

`kinonode-data/src/main/java/io/github/kullik01/kinonode/service/DatabaseService.java`

All schema definitions and migrations live in the `createTables()` method.

## Core Principle

KinoNode uses **additive-only migrations** with idempotent `ALTER TABLE` statements
wrapped in `try/catch`. This ensures the application can start from any schema
version and safely converge to the latest.

## Adding a New Column to an Existing Table

### Pattern

```java
// Inside createTables(), after existing migration blocks:
try {
  tmpStatement.execute("ALTER TABLE movies ADD COLUMN new_field TEXT;");
  LOGGER.info("Added new column to movies: new_field");
} catch (SQLException tmpException) {
  // Column already exists — safe to ignore
}
```

### Column Type Reference

| Java Type | SQLite Type | Example |
|---|---|---|
| `String` | `TEXT` | `title TEXT NOT NULL` |
| `Integer` | `INTEGER` | `year INTEGER` |
| `Double` | `REAL` | `raw_rating REAL` |
| `boolean` | `BOOLEAN DEFAULT 0` | `is_smart BOOLEAN DEFAULT 0` |
| `Instant` (epoch millis) | `INTEGER NOT NULL` | `date_added INTEGER NOT NULL` |

### Batch Migration Example (from codebase)

```java
String[] tmpNewColumns = {"director", "writer", "producer",
    "cinematographer", "composer", "actors", "duration", "raw_rating"};
for (String tmpColumn : tmpNewColumns) {
  try {
    String columnType = tmpColumn.equals("duration") ? "INTEGER"
        : (tmpColumn.equals("raw_rating") ? "REAL" : "TEXT");
    tmpStatement.execute("ALTER TABLE movies ADD COLUMN " + tmpColumn
        + " " + columnType + ";");
    LOGGER.info("Added new column to movies: " + tmpColumn);
  } catch (SQLException tmpException) {
    // Ignore, column likely already exists.
  }
}
```

## Adding a New Table

### Pattern

Use `CREATE TABLE IF NOT EXISTS` with a text block:

```java
String tmpCreateNewTable = """
    CREATE TABLE IF NOT EXISTS new_entities (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        parent_id INTEGER,
        created_at INTEGER NOT NULL,
        FOREIGN KEY (parent_id) REFERENCES movies(id) ON DELETE CASCADE
    );
    """;
tmpStatement.execute(tmpCreateNewTable);
```

### Conventions

- Primary keys: `id INTEGER PRIMARY KEY AUTOINCREMENT`
- Foreign keys: Always specify `ON DELETE CASCADE`
- Timestamps: Store as epoch milliseconds (`INTEGER NOT NULL`)
- Unique constraints: `name TEXT UNIQUE NOT NULL`
- Use text blocks (`"""..."""`) for multi-line SQL

## Adding Indexes

```java
tmpStatement.execute(
    "CREATE INDEX IF NOT EXISTS idx_movies_genre ON movies(genre);");
```

- Always use `IF NOT EXISTS`
- Name format: `idx_{table}_{column}`
- Add indexes for columns used in Smart Watchlist filtering or frequent queries

## Inserting Default Settings

```java
tmpStatement.execute(
    "INSERT INTO settings (key, value) VALUES ('new_setting', 'default_value') "
    + "ON CONFLICT(key) DO UPDATE SET value = excluded.value;");
```

## Updating Repository After Schema Change

After adding columns, update the corresponding repository:

### MovieRepository — Update INSERT

```java
String tmpInsert = """
    INSERT INTO movies (file_path, title, year, genre, ..., new_field, date_added)
    VALUES (?, ?, ?, ?, ..., ?, ?)
    """;
// Add: tmpPreparedStatement.setString(N, tmpMovie.getNewField());
```

### MovieRepository — Update SELECT Mapping

```java
// In the ResultSet mapping loop:
String tmpNewField = tmpResultSet.getString("new_field");
tmpMovie.setNewField(tmpNewField);
```

## Rules

- ❌ Never `DROP` columns or tables in migrations
- ❌ Never rename columns (SQLite doesn't support `ALTER TABLE RENAME COLUMN` reliably)
- ✅ Always wrap `ALTER TABLE` in `try/catch` for idempotency
- ✅ Always log successful column additions
- ✅ Use `IF NOT EXISTS` for tables and indexes
- ✅ Enable foreign keys: `PRAGMA foreign_keys = ON;`

## Existing Schema Summary

| Table | Purpose | Key Columns |
|---|---|---|
| `movies` | Movie catalog | `id`, `file_path` (UNIQUE), `title`, `year`, `genre`, `description`, `rating`, `raw_rating`, `cover_image_path`, `trailer_url`, `director`, `writer`, `producer`, `cinematographer`, `composer`, `actors`, `duration`, `date_added` |
| `watchlists` | Watchlist definitions | `id`, `name` (UNIQUE), `created_at`, `is_smart`, `smart_rules_json` |
| `watchlist_movies` | Many-to-many join | `watchlist_id`, `movie_id`, `position` |
| `settings` | Key-value config store | `key` (PK), `value` |
