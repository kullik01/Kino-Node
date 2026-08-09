<h1 align="center">
  <sub><img src="kinonode-ui/src/main/resources/io/github/kullik01/kinonode/images/logos/logo.png" height="250" alt="KinoNode Logo" /></sub>
  <br>
  KinoNode
</h1>

[![Build Status](https://github.com/kullik01/Kino-Node/actions/workflows/build.yml/badge.svg)](https://github.com/kullik01/Kino-Node/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/kullik01/Kino-Node?style=flat-square&color=Release)](https://github.com/kullik01/Kino-Node/releases/tag/v1.0.1)
[![Java Version](https://img.shields.io/badge/Java-25-orange.svg?style=flat-square)](https://jdk.java.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-blue.svg?style=flat-square)](https://openjfx.io/)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-green.svg?style=flat-square)](LICENSE)

KinoNode is a modern, lightweight, personal movie library manager built on a solid Java 25 and JavaFX 25 baseline. Featuring a clean MVC (Model-View-Controller) architecture, an embedded SQLite database for persistence, and a premium dark mode design, KinoNode brings a media-center feel to your desktop movie collection.

---

## 📑 Table of Contents

- [📥 Download](#-download)
- [🌟 Key Features](#-key-features)
  - [🗃️ Library & Metadata](#️-library--metadata)
  - [🎨 User Interface & Experience](#-user-interface--experience)
  - [🎬 Organization & Playback](#-organization--playback)
  - [💾 Data Management & Configuration](#-data-management--configuration)
- [🛠️ Technology Stack](#️-technology-stack)
- [🚀 Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Clone and Run](#clone-and-run)
  - [Running Tests](#running-tests)
  - [Code Formatting](#code-formatting)
- [📂 Database & Data Storage](#-database--data-storage)
  - [Single Directory / Portable Mode (Recommended)](#single-directory--portable-mode-recommended)
  - [Standard OS Application Data Directory](#standard-os-application-data-directory)
- [📦 Native Distribution & Packaging](#-native-distribution--packaging)
  - [Modular Runtime Image](#modular-runtime-image)
  - [Native OS Installers](#native-os-installers)
  - [🐧 Linux Desktop Integration & Shortcuts](#-linux-desktop-integration--shortcuts)
  - [Automated CI/CD Pipelines](#automated-cicd-pipelines)
- [🤝 Contributing & Feedback](#-contributing--feedback)
- [📄 License](#-license)
- [👥 Contributors & Credits](#-contributors--credits)

---

## 📥 Download

The easiest way to use KinoNode is to download the latest pre-compiled binaries for your operating system. I provide standalone installers and portable packages for Windows, macOS, and Linux.

**[⬇️ Download the Latest Release](https://github.com/kullik01/Kino-Node/releases/latest)**

---

## 🌟 Key Features

### 🗃️ Library & Metadata
* **Workspace Synchronization:** Recursively scan local folders to catalog your movies. Title, year, and metadata are automatically extracted from filenames and synchronized.
* **Smart Metadata Enrichment:** Automatically queries online directories (IMDb Suggestion API & Wikidata SPARQL) to populate missing directors, writers, producers, cinematographers, composers, and all billing-ordered cast members.
* **Crew & Actor Lookups:** Interactive dropdown controls (ComboBoxes) for multi-person metadata fields (Directors, Writers, Producers, Cinematographers, Composers, and Actors). Select any specific crew member or actor and view their dedicated IMDb profile.
* **Wikidata-Powered Disambiguation:** Resolves the exact IMDb person ID (`nmXXXXXXX`) for crew and actors in the background via Wikidata SPARQL queries, linking directly to their official biography page instead of a generic name search.

### 🎨 User Interface & Experience
* **Modern Media Hub:** A responsive, poster-centric library grid. Easily replace or custom-set movie cover art at any time.
* **Dynamic Light/Dark Theme Switcher:** Instantly toggle between a crisp Light mode and a premium Dark mode design natively powered by the AtlantaFX (Primer) styling framework.
* **Clapperboard Rating System:** Rate your favorite films on a sliding 0–10 scale using custom film-reel clapperboard graphics.
* **Library Analytics Dashboard:** A dedicated interactive Analytics view displaying real-time library KPIs (Total Movies, Average Rating, Year Span, Total Watchlists, Top Genre, and Unique Directors count), dynamic decade distribution bar charts, and clapperboard rating frequency charts.
* **Embedded IMDb Web Browser:** A built-in, tab-integrated browser preloaded on startup, complete with custom back, forward, reload, and home buttons for seamless movie exploration.

### 🎬 Organization & Playback
* **Smart Watchlists & Dynamic Queries:** Build automated watchlists using an interactive rule builder. Define conditions (e.g., Year < 2000, IMDb Rating > 8.0) and watch the list instantly populate. The UI reacts in real-time, automatically updating smart watchlists whenever new movies match or metadata changes.
* **Playlists & Watchlists:** Build, rename, and manage multiple standard watchlists. Drag, reorder, or organize movies exactly the way you want them.
* **Top Cast & Crew Leaderboards:** Automatically calculates and displays ranked leaderboards of your collection's most featured Directors and Actors/Cast members.
* **External Player Launch:** Seamlessly watch your movies. KinoNode auto-detects system media players (like `mpv` and `VLC`) or launches any custom player executable path.
* **YouTube Trailer Integration:** Automatically scrapes and saves official YouTube trailer URLs, allowing you to watch them in your default web browser with a single click.

### 💾 Data Management & Configuration
* **100% Portable Mode Capability:** Run KinoNode entirely self-contained within a single directory without touching OS Application Data folders—perfect for USB drives and portable storage.
* **Catalog Export & HTML Reports:** One-click portability allowing you to export your entire movie catalog to structured **JSON**, spreadsheet-ready **CSV**, or generate a styled, standalone **HTML Collection Report** that can be opened in any web browser.
* **Automated SQLite Snapshot Backups:** Create instant, timestamped backup snapshots of your embedded SQLite database (`kinonode.db`) with a single click from the Analytics toolbar.
* **Configurable Video File Extensions:** Define exactly which custom video formats (e.g. `.mp4`, `.mkv`, `.avi`, `.webm`) the library scanner should target via the Settings dashboard.

---

## 🛠️ Technology Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 25 (LTS) | Modern Java baseline exploiting modern switch patterns, records, and pattern matching. |
| **UI Toolkit** | JavaFX 25 | Fully modular hardware-accelerated user interface framework. |
| **Theming** | AtlantaFX (Primer Dark) | Modern CSS system styled after Github's Primer Dark design language. |
| **Database** | SQLite via JDBC | Embedded, zero-configuration database stored in a single local file. |
| **Dependency Injection** | Google Guice | Lightweight DI framework wiring the UI and data layers together. |
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
Generate a native executable installer (Windows `.msi`, macOS `.dmg`, or Linux `.deb` / `.rpm`) configured for the current host system:
```bash
./gradlew jpackage
```
> [!NOTE]
> Cross-compilation is not supported by `jpackage`. You must run the build command on the target operating system.

### 🐧 Linux Desktop Integration & Shortcuts
KinoNode automatically extracts FreeDesktop icon assets (`~/.local/share/icons/hicolor/256x256/apps/kinonode.png`) and generates application menu entries (`~/.local/share/applications/KinoNode.desktop`) and user Desktop shortcuts (`~/Desktop/KinoNode.desktop`) upon launch or via the included installation script:
```bash
./install_shortcut.sh
```
* **Window Manager Binding:** Utilizes `StartupWMClass=io-github-kullik01-kinonode-Launcher` to bind window manager instances directly to custom branding in Linux taskbars and window switchers instead of generic Java runtime icons.

### Automated CI/CD Pipelines
KinoNode features a GitHub Actions workflow (`.github/workflows/build.yml`) triggered on pushes and pull requests to the main branch. The workflow runs automated tests and builds distribution assets across a multi-OS matrix:
* **Windows:** `.msi` installer, plus portable `.zip` and `.tar.gz` archives.
* **macOS:** Premium `.dmg` and `.pkg` installers, plus portable `.zip` and `.tar.gz` archives.
* **Linux:** Debian `.deb` and RedHat `.rpm` packages, plus portable `.zip` and `.tar.gz` archives.

---

## 🤝 Contributing & Feedback

Here's how you can help:
* **Found a bug?** Open an issue on our [GitHub Issues](https://github.com/kullik01/Kino-Node/issues) page.
* **Have a feature request?** We'd love to hear it. Start a discussion or open an issue.
* **Want to contribute code?** Fork the repository, create a new branch for your feature or bugfix, and submit a Pull Request.

---

## 📄 License
This project is licensed under the **BSD 3-Clause License** - see the [LICENSE](LICENSE) file for details.

## 👥 Contributors & Credits
* *Lead Developer* - Hannah Kullik
* *Logo Designer* - Martin Urban & Hannah Kullik
