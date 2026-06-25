# KinoNode

KinoNode is a modern, lightweight, open-source personal movie library manager built with Java and JavaFX. It follows a clean MVC (Model-View-Controller) architecture, uses an embedded SQLite database for persistence, and offers a premium, media-center-style dark user interface.

## Features

- **Workspace Syncing**: Configure root movie directories and scan them recursively. KinoNode auto-registers movies, parses titles and years from filenames, and skips duplicate paths.
- **Visual Library Grid**: A responsive grid focusing on cover art posters first. Set custom cover files at any time.
- **Editable Metadata**: Edit Title, Release Year, Genre, Trailer URL, and Description (optional field) for each movie.
- **0–10 Rating System**: Rate movies from 0 to 10 (11 states, including unrated) using custom clapperboard/film-reel icons.
- **Multiple Watchlists**: Create, rename, delete, and manage named playlists for movies. Add a movie to multiple watchlists, and reorder titles manually.
- **External Player Integration**: Launches videos directly in your preferred media player (auto-detects `mpv` and `VLC`, or accepts custom executable paths).
- **YouTube Trailers**: Save YouTube trailer URLs and open them in the system web browser with a single click.

---

## Technology Stack

- **JDK Baseline**: Java 25 (LTS)
- **UI Toolkit**: JavaFX (OpenJFX) 25
- **Theming**: AtlantaFX (Primer Dark) + Custom media-center CSS
- **Persistence**: Embedded SQLite via `sqlite-jdbc` (zero-setup single-file database)
- **Build Tool**: Gradle (with Kotlin DSL)
- **Unit Testing**: JUnit 5 for repositories

---

## Local Development & Running

KinoNode requires Java 25 or later. You can run the application locally using the Gradle wrapper:

```bash
# Clone the repository
git clone https://github.com/kullik01/Kino-Node.git
cd Kino-Node

# Build and run the app
./gradlew run
```

### Running Tests

To run the repository and service unit tests:

```bash
./gradlew test
```

---

## Native Packaging & Release

KinoNode uses the `org.beryx.jlink` Gradle plugin to create lightweight custom runtime images and native OS installers via `jpackage`.

### Local Packaging

To compile a modular runtime image locally:

```bash
./gradlew jlink
```

To create a native installer (Windows `.exe`, macOS `.dmg`, or Linux `.deb`/`.rpm`) on your machine:

```bash
./gradlew jpackage
```

*Note: `jpackage` cannot cross-compile. Native installers must be built on their respective operating systems.*

### CI Matrix (GitHub Actions)

KinoNode includes a automated CI/CD workflow (`.github/workflows/package.yml`) that triggers on tag pushes (e.g. `v1.0.0`). It runs a multi-OS build matrix to generate native installers on Windows, Linux, and macOS:

- **Windows**: Produces a native `.exe` installer and a portable app-image `.zip`.
- **Linux**: Produces `.deb` and `.rpm` packages, plus a portable `.zip`.
- **macOS**: Produces `.dmg` and `.pkg` installers.

The workflow bundles these files and attaches them directly to a draft GitHub Release.

---

## License

This project is licensed under the BSD 3-Clause License - see the [LICENSE](LICENSE) file for details.

## Credits

- **Hannah Kullik** (Author)
- **Martin Urban** (Logo Designer)