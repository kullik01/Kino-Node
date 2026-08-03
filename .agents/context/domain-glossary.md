# Domain Glossary

Definitions of key domain terms used throughout the KinoNode codebase.

## Core Entities

| Term | Definition | Java Class | DB Table |
|---|---|---|---|
| **Movie** | A cataloged video file with associated metadata (title, year, genre, crew, rating, etc.). The central entity of the application. | `Movie.java` | `movies` |
| **Watchlist** | A user-curated, ordered collection of movies. Can be either standard (manual) or smart (rule-driven). | `Watchlist.java` | `watchlists` |
| **Smart Watchlist** | A special watchlist that auto-populates based on `SmartRule` conditions. Updates dynamically when movies are added or metadata changes. | `Watchlist.java` (with `isSmart=true`) | `watchlists` |
| **SmartRule** | A single filter condition (field + operator + value) used by Smart Watchlists. Example: "Year > 2000" or "Genre Equals Drama". | `SmartRule.java` | Stored as JSON in `watchlists.smart_rules_json` |

## Application Concepts

| Term | Definition |
|---|---|
| **Library** | The complete set of movies currently cataloged in the database. Populated by scanning workspace folders. |
| **Workspace** | One or more configured local filesystem folder paths that KinoNode scans to discover movie files. Stored as semicolon-separated paths in the `settings` table under key `workspace_folders`. |
| **Library Sync** | The process of scanning workspace folders, discovering new movie files, extracting metadata from filenames, and persisting them to the database. Handled by `LibraryScanService`. |
| **Portable Mode** | Running KinoNode with the database stored in the application directory itself (rather than OS-specific app data). Activated when running from a directory named `KinoNode` or when `kinonode.db` exists locally. |

## Metadata & Enrichment

| Term | Definition |
|---|---|
| **Rating** | The user's personal 0–10 integer rating displayed as clapperboard graphics. Stored in `movies.rating`. |
| **Raw Rating** | The external decimal rating fetched from IMDb (e.g., 8.7). Stored in `movies.raw_rating` as `REAL`. |
| **Metadata Enrichment** | Automatic querying of IMDb Suggestion API and Wikidata SPARQL to populate crew, actors, duration, genre, and other metadata for movies. Handled by `ImdbService`. |
| **Trailer URL** | A YouTube URL for the movie's official trailer, scraped automatically. Stored in `movies.trailer_url`. |

## UI Concepts

| Term | Definition |
|---|---|
| **Sidebar** | The left navigation panel in `MainController` containing buttons for Library, Watchlists, Analytics, IMDb, Settings, and About. |
| **Theme Toggle** | An `AtlantaFX ToggleSwitch` in the sidebar switching between Primer Light and Primer Dark themes. Persisted in `settings` as `theme_mode`. |
| **Movie Card** | A poster-centric thumbnail in the library grid, rendered by `MovieCardController`. Cached in `cardCache` for performance. |
| **Movie Detail** | The full metadata view for a single movie, rendered by `MovieDetailController`. Replaces the library grid in the same `StackPane`. |

## Services

| Service | Responsibility |
|---|---|
| `DatabaseService` | SQLite connection management, schema creation, migrations |
| `LibraryScanService` | Workspace folder scanning, file discovery, title extraction |
| `ImdbService` | IMDb API queries, metadata enrichment, Wikidata SPARQL |
| `AnalyticsService` | Library KPI calculations, chart data aggregation |
| `PlaybackService` | Media player detection and movie file launching |
| `YoutubeService` | YouTube trailer URL scraping |

## Settings Keys

Key-value pairs stored in the `settings` database table:

| Key | Purpose | Default |
|---|---|---|
| `workspace_folders` | Semicolon-separated folder paths | (empty) |
| `video_extensions` | Comma-separated file extensions | `3gp,avi,flv,...,wmv` |
| `theme_mode` | `light` or `dark` | `light` |
| `player_path` | Custom media player executable path | (empty) |
| `library_sort_filter` | Last-used sort option | (none) |
