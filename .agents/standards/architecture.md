# Architecture Reference

This document describes KinoNode's module structure, dependency graph,
and design patterns used throughout the codebase.

## Module Dependency Graph

```
┌─────────────────────────────────────────────────────────┐
│                     kinonode-ui                          │
│  JavaFX Controllers, Views, DI (Guice), UI Utilities    │
│  Packages: controller/, view/pages/, di/, uiutil/       │
├────────────────────────┬────────────────────────────────┤
                         │ depends on
├────────────────────────▼────────────────────────────────┤
│                    kinonode-data                         │
│  Repositories (JDBC), Services, External API Clients    │
│  Packages: repository/, service/                        │
├────────────────────────┬────────────────────────────────┤
                         │ depends on (transitive)
├────────────────────────▼────────────────────────────────┤
│                   kinonode-domain                        │
│  Domain Models, Utility Classes                         │
│  Packages: model/, util/                                │
└─────────────────────────────────────────────────────────┘
```

## Java Module System (JPMS)

Each Gradle module has a `module-info.java`:

### kinonode-domain
```java
module kinonode.domain {
    requires com.google.gson;
    exports io.github.kullik01.kinonode.model;
    exports io.github.kullik01.kinonode.util;
    opens io.github.kullik01.kinonode.model to com.google.gson;
}
```

### kinonode-data
```java
module kinonode.data {
    requires transitive kinonode.domain;
    requires java.sql;
    requires java.logging;
    requires com.google.guice;
    requires jakarta.inject;
    requires com.google.gson;
    exports io.github.kullik01.kinonode.repository;
    exports io.github.kullik01.kinonode.service;
    opens io.github.kullik01.kinonode.repository to com.google.guice;
    opens io.github.kullik01.kinonode.service to com.google.guice;
}
```

### kinonode-ui
```java
module kinonode.ui {
    requires kinonode.domain;
    requires kinonode.data;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.web;
    requires atlantafx.base;
    requires com.google.guice;
    requires jakarta.inject;
    requires com.google.gson;
    exports io.github.kullik01.kinonode;
    exports io.github.kullik01.kinonode.controller;
    exports io.github.kullik01.kinonode.view.pages;
    exports io.github.kullik01.kinonode.di;
    opens io.github.kullik01.kinonode to javafx.graphics;
    opens io.github.kullik01.kinonode.controller to com.google.guice, javafx.fxml;
    opens io.github.kullik01.kinonode.di to com.google.guice;
}
```

## Design Patterns

### 1. Singleton Service Pattern

Services and repositories are bound as singletons in Guice:

```java
// KinoNodeModule.configure()
bind(DatabaseService.class).in(Singleton.class);
bind(MovieRepository.class).in(Singleton.class);
bind(SettingsRepository.class).in(Singleton.class);
```

### 2. Repository Pattern

Data access is abstracted behind repository classes that encapsulate all SQL:

- `MovieRepository` — CRUD for movies table
- `WatchlistRepository` — CRUD for watchlists + join table
- `SettingsRepository` — Key-value config store

### 3. Provider-Based Lazy Loading

`MainController` uses `Provider<T>` to defer controller instantiation until
the user navigates to a tab:

```java
private final Provider<LibraryController> libraryProvider;
// Controller created only on first navigation:
LibraryController controller = libraryProvider.get();
```

### 4. View Caching

`MainController` caches loaded views in `Map<String, Parent> views` to avoid
rebuilding the entire scene graph on tab switches.

### 5. Refreshable Interface

Controllers that need to reload data on navigation implement `Refreshable`:

```java
public interface Refreshable {
  void refresh();
}
```

`MainController.loadView()` calls `refresh()` on cached views after displaying them.

### 6. Observer Pattern (Library Change Listeners)

`AppContext` maintains a list of `Runnable` listeners notified when the
library changes (e.g., after a sync):

```java
appContext.addLibraryChangeListener(() -> { /* react to changes */ });
appContext.notifyLibraryChanged();  // Called after sync
```

### 7. Controller-View Separation

- **Controller:** Receives `@Inject` dependencies, orchestrates business logic
- **View:** Pure UI construction, public fields for controller binding
- Controller creates View in `buildView()`, wires events, then returns `view.getRoot()`

### 8. Background Task Pattern

Long-running operations use `javafx.concurrent.Task<T>` on daemon threads:

```java
Task<ResultType> tmpTask = new Task<>() {
  @Override
  protected ResultType call() throws Exception {
    // Background work
  }
};
tmpTask.setOnSucceeded(event -> { /* UI update */ });
tmpTask.setOnFailed(event -> { /* Error handling */ });
Thread tmpThread = new Thread(tmpTask);
tmpThread.setDaemon(true);
tmpThread.start();
```

## Application Startup Flow

```
Launcher.main()
  → KinoNodeApp.start(Stage)
    → AppContext.getInstance()
      → Guice.createInjector(KinoNodeModule)
        → DatabaseService (singleton, creates tables)
        → All repositories and services bound
    → MainController (injected via Guice)
      → buildView() — creates sidebar + content area
      → initialize() — loads theme, shows Library, preloads IMDb
```

## Key File Locations

| Purpose | Path |
|---|---|
| Root build config | `build.gradle.kts` |
| UI module build | `kinonode-ui/build.gradle.kts` |
| Guice DI module | `kinonode-ui/.../di/KinoNodeModule.java` |
| App singleton context | `kinonode-ui/.../di/AppContext.java` |
| Main shell controller | `kinonode-ui/.../controller/MainController.java` |
| CSS theme | `kinonode-ui/.../resources/.../css/theme.css` |
| i18n messages | `kinonode-ui/.../resources/.../i18n/messages.properties` |
| DB schema + migrations | `kinonode-data/.../service/DatabaseService.java` |
| CI/CD workflow | `.github/workflows/build.yml` |
| Code formatter config | `eclipse-java-google-style.xml` |
