# Skill: Commenting & Documentation

This guide defines the commenting and documentation standards for the
KinoNode application. Follow these rules to ensure all code documentation
is consistent, complete, and matches the existing codebase conventions.

## Reference Standards

- `GEMINI.md` — Mandatory Javadoc section
- `standards/java-conventions.md` — Javadoc Style section

## Comment Categories

KinoNode uses four categories of comments, each with strict conventions:

1. **License Header** — BSD 3-Clause block at file top
2. **Javadoc Comments** (`/** ... */`) — on classes, constructors, methods,
   getters/setters
3. **Field Annotations** (`// ...`) — end-of-line on field declarations
   (units, ranges, nullability only)

> ✅ **Inline comments (`//` inside method bodies) are ALLOWED** when they
> provide a better understanding of complex logic, business rules, or non-obvious
> algorithms. Otherwise, prefer self-documenting code and method-level Javadoc.

---

## 1. License Header

Every `.java` file **MUST** begin with the full BSD 3-Clause license block.
This is the very first content in every file — before the `package` declaration.

Copy the header verbatim from any existing file. Note the `<p>` HTML tags
between paragraphs — these are **required** by Google Java Format / Spotless.

```java
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
package io.github.kullik01.kinonode.<subpackage>;
```

---

## 2. Class-Level Javadoc

**Required on:** every `public` class (and package-private classes that serve
as primary implementations).

### Rules

- First sentence: descriptive present-tense verb or noun phrase
- Describe the class's role and responsibilities
- Use `{@link ClassName}` to reference related types
- Mention the interface it implements (if applicable)
- Keep to 2–4 lines; longer descriptions for complex classes

### Templates by Class Type

#### Domain Model

```java
/**
 * Represents a [Entity] entity in the Kino-Node application. [Description
 * of what this entity models and its primary purpose.]
 */
public class EntityName {
```

**Real example — `Movie.java`:**
```java
/**
 * Represents a Movie entity in the Kino-Node application. This class stores
 * metadata about a movie, including its file path, title, production details
 * (director, writer, producer, etc.), release year, genre, description, rating,
 * and resources like cover image path and trailer URL.
 */
public class Movie {
```

#### Repository

```java
/**
 * Data access repository for managing {@link Entity} entities in the database.
 * Handles database operations such as retrieving, inserting, updating, and
 * deleting [entity] records using standard JDBC.
 */
public class EntityRepository {
```

#### Service

```java
/**
 * Service class responsible for [primary responsibility]. It handles
 * [specific operations] and provides [what it exposes to consumers].
 */
public class SomeService {
```

**Real example — `DatabaseService.java`:**
```java
/**
 * Service class responsible for managing the SQLite database. It handles
 * connection creation, schema setup, migration, and closing resource links.
 * Provides the central SQL connection instance to repositories and other
 * services.
 */
public class DatabaseService {
```

#### Controller

```java
/**
 * Controller for the "[ViewName]" view of the application. [Description of
 * what this controller manages.] Implements {@link Refreshable} to allow
 * [refresh behavior].
 */
public class SomeController implements Refreshable {
```

**Real example — `LibraryController.java`:**
```java
/**
 * Controller for the library view of the application. Manages displaying,
 * filtering, searching, and sorting the movies in the user's library.
 * Implements {@link Refreshable} to allow refreshing the movie catalog display.
 * Caches view cards and controllers to optimize rendering performance when
 * filtering/sorting.
 */
public class LibraryController implements Refreshable {
```

#### Utility Class

```java
/**
 * Utility class containing static helper methods for [domain description],
 * such as [key operations].
 */
public final class SomeUtils {
```

#### View Class

```java
/**
 * View component for the [feature/page] page. Builds the JavaFX scene graph
 * programmatically and exposes UI controls as public fields for controller
 * access.
 */
public class SomeView {
```

---

## 3. Constructor Javadoc

**Required on:** all `public` constructors.

### Rules

- First sentence: always begins with **"Constructs a new …"** or
  **"Constructs a [ClassName] with …"**
- Describe what gets initialized and any default values
- Document every parameter with `@param`
- For Guice `@Inject` constructors, the Javadoc block goes **above**
  the `@Inject` annotation

### Templates

#### Default / No-Arg Constructor (single-line Javadoc)

```java
/** Constructs a new [Entity] with the current timestamp as the [field]. */
public EntityName() {
  this.[timestampField] = Instant.now();
}
```

#### Parameterized Constructor (multi-line Javadoc)

```java
/**
 * Constructs a new [Entity] with a specified [param description] and sets
 * the [field] to the current timestamp.
 *
 * @param paramOne the [description of first parameter]
 * @param paramTwo the [description of second parameter]
 */
public EntityName(String paramOne, String paramTwo) {
```

#### Guice `@Inject` Constructor

```java
/**
 * Constructs a [ClassName] with the specified dependencies.
 *
 * @param someRepository the repository for [purpose]
 * @param resources the resource bundle for internationalized strings
 */
@Inject
public SomeController(SomeRepository someRepository,
    ResourceBundle resources) {
  this.someRepository = someRepository;
  this.resources = resources;
}
```

**Real example — `WatchlistRepository.java`:**
```java
/**
 * Constructs a WatchlistRepository with dependencies on the database and
 * movie repository.
 *
 * @param databaseService the service for active database connection
 * @param movieRepository the repository for resolving movie dependencies
 *        within watchlists
 */
@Inject
public WatchlistRepository(DatabaseService databaseService,
    MovieRepository movieRepository) {
```

---

## 4. Method Javadoc

**Required on:** all `public` methods (and `protected` methods that form part
of an API contract).

### Rules

- First sentence: **imperative verb** ("Gets the…", "Sets the…",
  "Retrieves all…", "Maps a…", "Exports the…", "Extracts the…")
- Document behavior, including edge cases and error handling
- Use all applicable Javadoc tags:
  - `@param` — every parameter, lowercase description starting with "the"
  - `@return` — what is returned and any special return values
    (null, empty list, default)
  - `@throws` — every checked exception and notable unchecked exceptions
- Use `{@link ClassName}` for cross-references to types
- Line-wrap `@param` / `@return` descriptions at ~72 characters, indenting
  continuation lines to align with the description start

### Templates

#### Query / Retrieval Method

```java
/**
 * Retrieves all [entities] stored in the database, ordered by [criteria].
 *
 * @return a list of all {@link Entity} objects found, or an empty list if
 *         none exist or on error
 */
public List<Entity> findAll() {
```

#### Lookup by ID

```java
/**
 * Finds a single [entity] by its database identifier.
 *
 * @param id the database ID of the [entity] to find
 * @return the {@link Entity} with the specified ID, or null if not found
 *         or on error
 */
public Entity findById(int id) {
```

#### Mapping / Transformation Method

```java
/**
 * Maps a single row from a database ResultSet to a [Entity] object.
 * Handles retrieving nullable integer values by checking wasNull().
 *
 * @param resultSet the ResultSet containing the database row
 * @return a mapped {@link Entity} object
 * @throws SQLException if a database access error occurs
 */
public Entity mapRowToEntity(ResultSet resultSet) throws SQLException {
```

#### Action / Side-Effect Method

```java
/**
 * Exports the [data description] to a formatted JSON file.
 *
 * @param targetFile the destination path for the JSON file
 * @param items the list of items to export
 * @throws IOException if an I/O error occurs while writing the file
 */
public void exportToJson(Path targetFile, List<Entity> items)
    throws IOException {
```

---

## 5. Getter & Setter Javadoc

**Required on:** every getter and setter in domain model classes.

### Rules

- Getters: "Gets the [field description]."
- Setters: "Sets the [field description]."
- Always include `@return` on getters, `@param` on setters
- Mention nullability when applicable (", or null if not persisted")

### Template

```java
/**
 * Gets the unique identifier of the [entity].
 *
 * @return the unique database ID, or null if not persisted
 */
public Integer getId() {
  return id;
}

/**
 * Sets the unique identifier of the [entity].
 *
 * @param id the unique database ID to set
 */
public void setId(Integer id) {
  this.id = id;
}
```

---

## 6. Inline Comments — ALLOWED FOR CLARITY

**You MAY use `//` comments inside method bodies** to explain complex logic, business rules, or non-obvious algorithms. 
However, strive to make the code as self-documenting as possible through:

- **Descriptive variable names** — e.g., `tmpParentDir` not `tmpDir`
- **Method extraction** — extract complex logic into a well-named private
  method with Javadoc
- **Method-level Javadoc** — document behavior, edge cases, and non-obvious
  logic in the method's Javadoc block

### ✅ Acceptable — Inline Comments for Better Understanding

```java
public void initialize() {
  // Ensure parent directory exists before attempting to connect
  Path tmpParentDir = dbPath.getParent();
  
  // The SQLite JDBC driver requires the file path to be absolute
  String tmpUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
}
```

> **Note:** End-of-line `//` annotations on **field declarations** (e.g.,
> `// 0-10 or null`) are also **permitted** — see Section 7.6.

---

## 7. Class Attributes & Instance Fields

Class-level fields (attributes/variables) follow strict grouping, ordering,
and commenting conventions depending on their category.

### Field Categories

KinoNode uses five categories of class fields. Each has specific commenting
rules:

| Category | Access | Comment Style | Comment Required? |
|---|---|---|---|
| Constants (`LOGGER`, config) | `private static final` | Self-documenting name | Only if purpose is non-obvious |
| Injected dependencies | `private final` | No comment needed | No — the type is self-documenting |
| UI controls | `private` (controllers) / `public` (views) | Named with type prefix | No — the prefix documents purpose |
| Mutable state | `private` | End-of-line `//` if needed | Only for non-obvious fields |
| Domain model fields | `private` | End-of-line `//` for units/ranges | When value range, units, or nullability need clarification |

### 7.1 Declaration Order

Fields **MUST** be declared in the following order within a class body:

1. **Constants** — `private static final` (always first)
2. **Injected dependencies** — `private final` (repositories, services,
   resource bundles)
3. **UI controls** — view references and interactive widgets
4. **Mutable state** — caches, flags, offsets

Separate each group with a blank line.

### 7.2 Constants

Constants are always the first fields in a class. The `LOGGER` constant is
always the very first declaration.

```java
private static final Logger LOGGER =
    Logger.getLogger(DatabaseService.class.getName());
private static final String DB_FILENAME = "kinonode.db";
```

**Rules:**
- Use `UPPER_SNAKE_CASE` naming
- No comment needed if the name is descriptive
- Add an end-of-line `//` comment only for non-obvious values

### 7.3 Injected Dependencies

Dependencies injected via the Guice `@Inject` constructor. Declared as
`private final` fields — their types are self-documenting.

```java
private final MovieRepository movieRepository;
private final LibraryScanService libraryScanService;
private final SettingsRepository settingsRepository;
private final AppContext appContext;
private final Provider<MovieDetailController> movieDetailControllerProvider;
private final ResourceBundle resources;
```

**Rules:**
- Always `private final` — never mutable
- No field comment needed — the type name documents the purpose
- Group all injected dependencies together
- `Provider<T>` fields for lazy-loaded controllers go in this group

### 7.4 UI Controls

UI widget fields in controllers and views. Controllers declare them as
`private`; View classes declare them as `public` (for controller access).

#### Controller fields (private)

```java
private VBox libraryRoot;
private TextField txtSearch;
private ComboBox<String> cmbSort;
private FlowPane gridPane;
private Button btnSyncLibrary;
private HBox progressArea;
private ProgressBar progressBar;
```

#### View fields (public, for controller access)

```java
public final Label lblVersion = new Label("1.0.0");
public final TextField lblDbPath = new TextField();
public final Label lblMovieCount = new Label("0 movies");
public final Button btnGitHub;
public final Button btnBug;
```

**Rules:**
- Use the standard UI prefix convention (`btn`, `lbl`, `txt`, `cmb`)
- No field comment needed — the prefix + name documents purpose
- Initialize with defaults inline when the value is known at declaration
- Group related controls together (e.g., all buttons, all labels)

### 7.5 Mutable State Fields

Internal state such as caches, flags, and coordinate offsets.

```java
private final Map<String, Parent> views = new HashMap<>();
private final Map<String, Object> controllers = new HashMap<>();
private Button activeButton;
private double xOffset = 0;
private double yOffset = 0;
```

**Rules:**
- Add an end-of-line `//` comment if the purpose is not immediately clear
  from the name
- Initialize with default values inline when applicable

### 7.6 Domain Model Fields

Fields in entity/model classes (`Movie`, `Watchlist`, `SmartRule`).

```java
private Integer id;
private String filePath;
private String title;
private Integer year;
private String genre;
private String description;
private Integer rating;      // 0-10 or null
private Double rawRating;    // e.g., 4.9, 8.8 or null
private String coverImagePath;
private String trailerUrl;
private Integer duration;    // Duration in minutes
private Instant dateAdded;
```

**Rules:**
- All fields are `private` (never `public` or `protected`)
- Use wrapper types (`Integer`, `Double`) for nullable fields
- Use `Instant` for timestamps
- Add end-of-line `//` comment to clarify:
  - **Value ranges** (e.g., `// 0-10 or null`)
  - **Units** (e.g., `// Duration in minutes`)
  - **Format/examples** (e.g., `// e.g., 4.9, 8.8 or null`)
  - **Nullability** when the name doesn't imply it
- Self-documenting names (`title`, `genre`, `director`) do **not** need a
  comment

### 7.7 Complete Field Section Example

```java
public class LibraryController implements Refreshable {
  // --- Constants ---
  private static final Logger LOGGER =
      Logger.getLogger(LibraryController.class.getName());

  // --- UI Controls ---
  private VBox libraryRoot;
  private TextField txtSearch;
  private ComboBox<String> cmbSort;
  private FlowPane gridPane;
  private Button btnSyncLibrary;

  // --- Injected Dependencies ---
  private final MovieRepository movieRepository;
  private final LibraryScanService libraryScanService;
  private final SettingsRepository settingsRepository;
  private final AppContext appContext;
  private final Provider<MovieDetailController> movieDetailControllerProvider;
  private final ResourceBundle resources;
```

> **Note:** The `// --- Section ---` headers above are for illustration only.
> The actual codebase does **not** use section separators in field
> declarations — grouping with blank lines is sufficient.

---

## 8. Comment-Free Zones

The following do NOT require or allow comments:

- **All method bodies** — unless needed for clarity (see Section 6)
- `package` declarations
- `import` statements
- Standard getter/setter **implementations**
- `module-info.java` exports/opens (unless non-obvious)
- Injected dependency field declarations (the type is the documentation)
- UI control fields with standard prefixes (`btn`, `lbl`, `txt`, `cmb`)

---

## 9. Package-Level Documentation

KinoNode does **not** use `package-info.java` files. Package-level
documentation is not required.

---

## Checklist — Reviewing Documentation Completeness

Use this checklist when creating or reviewing a `.java` file:

- [ ] BSD 3-Clause license header present (lines 1–37)
- [ ] Class-level Javadoc with descriptive first sentence
- [ ] All `public` constructors have Javadoc with `@param` tags
- [ ] Guice `@Inject` constructor: Javadoc is above `@Inject`
- [ ] All `public` methods have Javadoc with applicable tags
- [ ] All getters/setters have Javadoc (domain models)
- [ ] `@param` descriptions start lowercase with "the"
- [ ] `@return` describes the return value and edge cases
- [ ] `@throws` documents checked exceptions
- [ ] Fields grouped in order: constants → dependencies → UI → state → model
- [ ] Domain model fields annotate units, ranges, or nullability
- [ ] Inline comments inside method bodies are only used for complex logic clarification
- [ ] No commented-out code blocks
- [ ] No wildcard imports
- [ ] Spotless passes: `./gradlew spotlessApply`
