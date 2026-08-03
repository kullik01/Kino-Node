# Database Schema Reference

Complete SQLite schema for KinoNode, derived from `DatabaseService.createTables()`.

## Database File

- **Filename:** `kinonode.db`
- **Engine:** SQLite via JDBC (`org.xerial:sqlite-jdbc:3.45.1.0`)
- **Foreign Keys:** Enabled via `PRAGMA foreign_keys = ON;`

## Tables

### movies

The primary table storing all movie metadata.

```sql
CREATE TABLE IF NOT EXISTS movies (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path         TEXT UNIQUE NOT NULL,
    title             TEXT NOT NULL,
    year              INTEGER,
    genre             TEXT,
    description       TEXT,
    rating            INTEGER,           -- User's 0-10 clapperboard rating
    raw_rating        REAL,              -- External decimal rating (e.g., 8.7)
    cover_image_path  TEXT,
    trailer_url       TEXT,
    director          TEXT,
    writer            TEXT,
    producer          TEXT,
    cinematographer   TEXT,
    composer          TEXT,
    actors            TEXT,              -- Comma-separated names
    duration          INTEGER,           -- Duration in minutes
    date_added        INTEGER NOT NULL   -- Epoch milliseconds (Instant)
);
```

**Indexes:**
```sql
CREATE INDEX IF NOT EXISTS idx_movies_year       ON movies(year);
CREATE INDEX IF NOT EXISTS idx_movies_rating     ON movies(rating);
CREATE INDEX IF NOT EXISTS idx_movies_raw_rating ON movies(raw_rating);
CREATE INDEX IF NOT EXISTS idx_movies_genre      ON movies(genre);
```

### watchlists

Watchlist definitions (both standard and smart).

```sql
CREATE TABLE IF NOT EXISTS watchlists (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT UNIQUE NOT NULL,
    created_at       INTEGER NOT NULL,      -- Epoch milliseconds
    is_smart         BOOLEAN DEFAULT 0,
    smart_rules_json TEXT                    -- JSON array of SmartRule objects
);
```

### watchlist_movies

Many-to-many join table with ordering support.

```sql
CREATE TABLE IF NOT EXISTS watchlist_movies (
    watchlist_id  INTEGER NOT NULL,
    movie_id      INTEGER NOT NULL,
    position      INTEGER NOT NULL,
    PRIMARY KEY (watchlist_id, movie_id),
    FOREIGN KEY (watchlist_id) REFERENCES watchlists(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);
```

### settings

Application configuration key-value store.

```sql
CREATE TABLE IF NOT EXISTS settings (
    key    TEXT PRIMARY KEY,
    value  TEXT
);
```

**Default values inserted on startup:**
```sql
INSERT INTO settings (key, value)
VALUES ('video_extensions', '3gp,avi,flv,m4v,mkv,mov,mp4,mpeg,mpg,mts,ogg,ogv,ts,webm,wmv')
ON CONFLICT(key) DO UPDATE SET value = excluded.value;
```

## Smart Rules JSON Schema

The `smart_rules_json` column stores a JSON array of `SmartRule` objects:

```json
[
  {
    "field": "Year",
    "operator": "Greater Than",
    "value": "2000"
  },
  {
    "field": "Genre",
    "operator": "Equals",
    "value": "Drama"
  }
]
```

### Valid Fields
`Title`, `Year`, `Genre`, `Director`, `Writer`, `Producer`, `Cinematographer`,
`Composer`, `Rating`, `IMDb Rating`, `Duration`

### Valid Operators
`Equals`, `Not Equals`, `Greater Than`, `Less Than`, `Contains`

## Java ↔ SQLite Type Mapping

| Java Type | SQLite Column Type | Notes |
|---|---|---|
| `Integer id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Auto-generated |
| `String` | `TEXT` | May be `UNIQUE`, `NOT NULL` |
| `Integer` | `INTEGER` | Nullable wrapper |
| `Double` | `REAL` | Nullable wrapper |
| `boolean` | `BOOLEAN DEFAULT 0` | SQLite stores as 0/1 |
| `Instant` | `INTEGER NOT NULL` | Stored as epoch milliseconds |
| `List<SmartRule>` | `TEXT` | Serialized as JSON via Gson |

## Migration Pattern

All migrations are in `DatabaseService.createTables()` using `ALTER TABLE`
wrapped in `try/catch`:

```java
try {
  tmpStatement.execute("ALTER TABLE table ADD COLUMN col TYPE;");
  LOGGER.info("Added new column to table: col");
} catch (SQLException tmpException) {
  // Column already exists — safe to ignore
}
```
