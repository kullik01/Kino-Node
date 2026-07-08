# 🎬 KinoNode

[![Java Version](https://img.shields.io/badge/Java-25-orange.svg?style=flat-square)](https://jdk.java.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-blue.svg?style=flat-square)](https://openjfx.io/)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-green.svg?style=flat-square)](LICENSE)

KinoNode is a modern, lightweight, personal movie library manager built on a solid Java 25 and JavaFX 25 baseline. Featuring a clean MVC (Model-View-Controller) architecture, an embedded SQLite database for persistence, and a premium dark mode design, KinoNode brings a media-center feel to your desktop movie collection.

---

## 🌟 Key Features

* **Workspace Synchronization:** Recursively scan local folders to catalog your movies. Title, year, and metadata are automatically extracted from filenames and synchronized.
* **Modern Media Hub:** A responsive, poster-centric library grid. Easily replace or custom-set movie cover art at any time.
* **Smart Metadata Enrichment:** Automatically queries online directories (IMDb Suggestion API & Wikidata SPARQL) to populate missing directors, writers, producers, cinematographers, composers, and all billing-ordered cast members.
* **Crew & Actor Lookups:** Interactive dropdown controls (ComboBoxes) for multi-person metadata fields (Directors, Writers, Producers, Cinematographers, Composers, and Actors). Select any specific crew member or actor and view their dedicated IMDb profile.
* **Wikidata-Powered Disambiguation:** Resolves the exact IMDb person ID (`nmXXXXXXX`) for crew and actors in the background via Wikidata SPARQL queries, linking directly to their official biography page instead of a generic name search.
* **Embedded IMDb Web Browser:** A built-in, tab-integrated browser preloaded on startup, complete with custom back, forward, reload, and home buttons for seamless movie exploration.
* **YouTube Trailer Integration:** Automatically scrapes and saves official YouTube trailer URLs, allowing you to watch them in your default web browser with a single click.
* **Clapperboard Rating System:** Rate your favorite films on a sliding 0–10 scale using custom film-reel clapperboard graphics.
* **Playlists & Watchlists:** Build, rename, and manage multiple watchlists. Drag, reorder, or organize movies exactly the way you want them.
* **Library Analytics Dashboard:** A dedicated interactive Analytics view displaying real-time library KPIs (Total Movies, Average Rating, Year Span, Total Watchlists, Top Genre, and Unique Directors count), dynamic decade distribution bar charts, and clapperboard rating frequency charts.
* **Top Cast & Crew Leaderboards:** Automatically calculates and displays ranked leaderboards of your collection's most featured Directors and Actors/Cast members.
* **Catalog Export & HTML Reports:** One-click portability allowing you to export your entire movie catalog to structured **JSON**, spreadsheet-ready **CSV**, or generate a styled, standalone **HTML Collection Report** that can be opened in any web browser.
* **Automated SQLite Snapshot Backups:** Create instant, timestamped backup snapshots of your embedded SQLite database (`kinonode.db`) with a single click from the Analytics toolbar.
* **External Player Launch:** Seamlessly watch your movies. KinoNode auto-detects system media players (like `mpv` and `VLC`) or launches any custom player executable path.

---

## 🛠️ Technology Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 25 (LTS) | Modern Java baseline exploiting modern switch patterns, records, and pattern matching. |
| **UI Toolkit** | JavaFX 25 | Fully modular hardware-accelerated user interface framework. |
| **Theming** | AtlantaFX (Primer Dark) | Modern CSS system styled after Github's Primer Dark design language. |
| **Database** | SQLite via JDBC | Embedded, zero-configuration database stored in a single local file. |
| **Build Tool** | Gradle (Kotlin DSL) | Modern build automation utilizing modular compilation pipelines. |
| **Testing** | JUnit 5 | Comprehensive test suite targeting data layers and repositories. |
| **Code Formatting** | Spotless & Google Java Format | Enforces consistent and clean code layout automatically. |

---

## 🚀 Getting Started

### Prerequisites
* **Java Development Kit (JDK) 25** or newer.

### Clone and Run
```bash
# Clone the repository
git clone https://github.com/kullik01/Kino-Node.git
cd Kino-Node

# Build and execute the application
./gradlew run
```

### Running Tests
To run the automated JUnit 5 test suite:
```bash
./gradlew test
```

### Code Formatting
To check code formatting:
```bash
./gradlew spotlessCheck
```

To automatically apply Google Java Format to all source files:
```bash
./gradlew spotlessApply
```

---

## 📂 Database & Data Storage

KinoNode utilizes an embedded SQLite database (`kinonode.db`) for all library metadata, watchlists, and application settings.

### Single Directory / Portable Mode (Recommended)
KinoNode supports running as a self-contained application where all data and configuration are stored together inside a single directory named **`KinoNode`**. When running from a directory named `KinoNode` (or when a local `kinonode.db` exists in the application directory), the database is stored directly inside that directory:
* **Single Directory Path:** `<AppDirectory>/kinonode.db`
* **Custom Path Override:** You can also override the storage directory by starting Java with `-Dkinonode.home=/path/to/directory`.

### Standard OS Application Data Directory
If not running in single-directory mode, KinoNode automatically creates and uses a platform-specific `KinoNode` directory inside your operating system user profile:
* **Windows:** `%APPDATA%/KinoNode/kinonode.db`
* **macOS:** `~/Library/Application Support/KinoNode/kinonode.db`
* **Linux/Unix:** `~/.local/share/KinoNode/kinonode.db` (or `$XDG_DATA_HOME/KinoNode/kinonode.db`)

You can inspect your active storage mode (`Single Directory / Portable Mode` vs `OS Application Data Directory`), check database file size, and open the containing folder directly from the **About** page within the application interface.

---

## 📦 Native Distribution & Packaging

KinoNode is fully modularized and supports native deployment packages using the `org.beryx.jlink` Gradle plugin.

### Modular Runtime Image
Create a minimal, platform-specific custom Java Runtime Image containing only the required modules:
```bash
./gradlew jlink
```

### Native OS Installers
Generate a native executable installer (Windows `.exe`, macOS `.dmg`, or Linux `.deb` / `.rpm`) configured for the current host system:
```bash
./gradlew jpackage
```
> [!NOTE]
> Cross-compilation is not supported by `jpackage`. You must run the build command on the target operating system.

### Automated CI/CD Pipelines
KinoNode features a GitHub Actions workflow (`.github/workflows/package.yml`) triggered on new release tags (e.g. `v1.0.0`). The workflow builds distribution assets across a multi-OS matrix:
* **Windows:** Standard `.exe` installer and portable `.zip` archives.
* **macOS:** Premium `.dmg` and `.pkg` installers.
* **Linux:** Debian `.deb`, RedHat `.rpm`, and portable `.zip` packages.

Finished binaries are automatically bundled and uploaded directly as draft assets to the corresponding GitHub Release.

---

## 📄 License
This project is licensed under the **BSD 3-Clause License** - see the [LICENSE](LICENSE) file for details.

## 👥 Contributors & Credits
* **Hannah Kullik** - *Lead Developer*
* **Martin Urban** - * Logo Designer*