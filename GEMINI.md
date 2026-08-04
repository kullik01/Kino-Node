# KinoNode — AI Agent Instructions

> This file is the system-level instruction overlay for AI coding assistants
> (Gemini, Claude, Copilot, etc.) working on this repository. It encodes the
> project's architecture, coding standards, and conventions so that AI-generated
> code is indistinguishable from human-authored contributions.

## Project Identity

| Field | Value |
|---|---|
| **Name** | KinoNode |
| **Description** | Personal Movie Library Manager |
| **Repository** | `kullik01/Kino-Node` |
| **License** | BSD 3-Clause (Hannah Kullik, 2026) |
| **Version** | 1.0.0 |

## Technology Stack

| Component | Technology |
|---|---|
| **Language** | Java 25 (modern switch, records, text blocks, pattern matching) |
| **UI Toolkit** | JavaFX 25 — **programmatic only, NO FXML** |
| **Theme** | AtlantaFX 2.0.1 (Primer Dark / Primer Light) |
| **Database** | Embedded SQLite via raw JDBC (`kinonode.db`) |
| **DI Framework** | Google Guice 6.x (`@Inject`, `@Singleton`, `Provider<T>`) |
| **Build** | Gradle 8.x (Kotlin DSL), multi-module |
| **Formatter** | Google Java Format via Spotless (`./gradlew spotlessApply`) |
| **i18n** | `java.util.ResourceBundle` → `messages.properties` |
| **Testing** | JUnit 5 |
| **JSON** | Google Gson 2.10.1 |

## Module Architecture

```
kino-node (root)
├── kinonode-domain    → Domain models & utilities
├── kinonode-data      → Persistence (repositories, services, JDBC)
└── kinonode-ui        → JavaFX UI layer (controllers, views, DI)
```

**Dependency flow:** `kinonode-ui` → `kinonode-data` → `kinonode-domain`

| Module | Key Packages | Key Classes |
|---|---|---|
| `kinonode-domain` | `model/`, `util/` | `Movie`, `Watchlist`, `SmartRule`, `PathUtils` |
| `kinonode-data` | `repository/`, `service/` | `MovieRepository`, `WatchlistRepository`, `SettingsRepository`, `DatabaseService`, `ImdbService`, `LibraryScanService`, `AnalyticsService`, `PlaybackService`, `YoutubeService` |
| `kinonode-ui` | `controller/`, `view/pages/`, `di/`, `uiutil/` | `MainController`, `LibraryController`, `WatchlistsController`, `SettingsController`, `AnalyticsController`, `ImdbController`, `AboutController`, `KinoNodeModule`, `AppContext`, `AboutView` |

## Mandatory Coding Standards

### License Header
Every `.java` file MUST begin with the BSD 3-Clause license header block. Copy
the exact format (including `<p>` paragraph tags) from any existing source file.

### Naming Conventions

| Scope | Convention | Example |
|---|---|---|
| Local variables | `tmp` prefix, camelCase | `tmpQuery`, `tmpScene`, `tmpResultSet`, `tmpException` |
| Instance fields | No prefix, camelCase | `settingsRepository`, `contentArea` |
| Constants | `UPPER_SNAKE_CASE` | `DB_FILENAME`, `LOGGER` |
| Logger | Static final, class-scoped | `private static final Logger LOGGER = Logger.getLogger(ClassName.class.getName());` |
| Exception variables | `tmpException` | `catch (SQLException tmpException)` |

### Imports
- **NEVER** use wildcard imports (`import java.util.*` is **FORBIDDEN**)
- Always use explicit, single-class imports
- Order: `java.*`, then third-party, then project packages
- Enforced automatically by Spotless

### Dependency Injection (Guice)
- **Constructor injection** with `@Inject` — never field injection
- Use `Provider<T>` for lazy/deferred controller instantiation in `MainController`
- All singletons registered in `KinoNodeModule.configure()`
- `ResourceBundle` provided via `@Provides @Singleton` in `KinoNodeModule`

### UI Construction Pattern
- Views are built **programmatically in Java** — FXML is not used anywhere
- Controllers expose two lifecycle methods:
  - `buildView()` → returns `Parent` (the JavaFX scene graph)
  - `initialize()` → wires event handlers and loads initial data
- Controllers implementing `Refreshable` get auto-refreshed on navigation
- UI strings are externalized via `ResourceBundle resources` (injected via Guice)
  - Access via: `resources.getString("section.key.name")`
- View classes live in `view/pages/` and are instantiated by their controller
- The `MainController` caches loaded views in a `Map<String, Parent>`

### Database Access Pattern
- Raw JDBC with `PreparedStatement` — no ORM
- Use **text blocks** (`"""..."""`) for multi-line SQL
- Upsert via `ON CONFLICT(key) DO UPDATE SET`
- Schema migrations: `ALTER TABLE ... ADD COLUMN` wrapped in `try/catch` (ignore if column exists)
- All SQL parameters bound via `?` placeholders — never string concatenation

### Javadoc
- Required on all `public` classes, constructors, and methods
- Use `@param`, `@return`, `@throws` tags
- First sentence: imperative verb ("Gets the…", "Sets the…", "Constructs a new…")
- See `skills/commenting.md` for full templates, field annotation rules,
  inline comment guidelines, and a documentation completeness checklist

### Formatting
- Google Java Format — 2-space indent
- Enforced by: `./gradlew spotlessApply`
- Max line length ~100 characters (Google style)

### Logging
- Use `java.util.logging.Logger` — never `System.out.println`
- Log exceptions at `Level.SEVERE` with the exception object attached
- Example: `LOGGER.log(Level.SEVERE, "Failed to load view " + name, exception);`

### Threading
- Use `Platform.runLater()` for UI updates from background threads
- Background tasks use `javafx.concurrent.Task<T>` with daemon threads
- Example: `Thread tmpThread = new Thread(task); tmpThread.setDaemon(true); tmpThread.start();`

## Feature Implementation Workflow

When implementing a new feature, follow this module-by-module order:

1. **Domain model** → `kinonode-domain/model/` (if new entity needed)
2. **DB migration** → `kinonode-data/service/DatabaseService.createTables()` (add column/table)
3. **Repository** → `kinonode-data/repository/` (CRUD operations)
4. **i18n keys** → `kinonode-ui/resources/.../i18n/messages.properties`
5. **View class** → `kinonode-ui/view/pages/` (if separate view needed)
6. **Controller** → `kinonode-ui/controller/` (with `@Inject`, `buildView()`, `initialize()`)
7. **Guice registration** → `KinoNodeModule` (if new singleton)
8. **Navigation** → `MainController` (if new top-level tab)
9. **Build** → `./gradlew spotlessApply && ./gradlew build`

## Agent Skill Files

Detailed step-by-step guides for common tasks are in `.agents/skills/`:

| Skill | File |
|---|---|
| End-to-end feature workflow | `.agents/skills/add-feature.md` |
| Creating domain model entities | `.agents/skills/add-domain-model.md` |
| Building MVC views & controllers | `.agents/skills/add-view-controller.md` |
| SQLite schema evolution | `.agents/skills/database-migration.md` |
| Externalizing UI strings | `.agents/skills/i18n-externalization.md` |
| Commenting & documentation | `.agents/skills/commenting.md` |

Additional reference material in `.agents/standards/` and `.agents/context/`.

## Absolute Prohibitions

- ❌ Do NOT introduce FXML files — all UI is programmatic Java
- ❌ Do NOT use field injection (`@Inject` on fields)
- ❌ Do NOT use wildcard imports
- ❌ Do NOT add new dependencies without explicit approval
- ❌ Do NOT use `System.out.println` — use `java.util.logging.Logger`
- ❌ Do NOT modify the license header format
- ❌ Do NOT skip the `tmp` prefix on local variables
