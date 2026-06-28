# 🎬 KinoNode

[![Java Version](https://img.shields.io/badge/Java-25-orange.svg?style=flat-square)](https://jdk.java.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-blue.svg?style=flat-square)](https://openjfx.io/)
[![License](https://img.shields.io/badge/License-BSD_3--Clause-green.svg?style=flat-square)](LICENSE)

KinoNode is a modern, lightweight, personal movie library manager built on a solid Java 25 and JavaFX 25 baseline. Featuring a clean MVC (Model-View-Controller) architecture, an embedded SQLite database for persistence, and a premium dark mode design, KinoNode brings a media-center feel to your desktop movie collection.

---

## 🌟 Key Features

* **Workspace Synchronization:** Recursively scan local folders to catalog your movies. Title, year, and metadata are automatically extracted from filenames and synchronized.
* **Modern Media Hub:** A responsive, poster-centric library grid. Easily replace or custom-set movie cover art at any time.
* **Smart Metadata Enrichment:** Automatically queries online directories (IMDb Suggestion API & Wikidata SPARQL) to populate missing directors, writers, producers, cinematographers, and composers.
* **YouTube Trailer Integration:** Automatically scrapes and saves official YouTube trailer URLs, allowing you to watch them in your default web browser with a single click.
* **Clapperboard Rating System:** Rate your favorite films on a sliding 0–10 scale using custom film-reel clapperboard graphics.
* **Playlists & Watchlists:** Build, rename, and manage multiple watchlists. Drag, reorder, or organize movies exactly the way you want them.
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
* **Martin Urban** - *Visual Brand & Logo Design*