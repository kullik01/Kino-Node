# Skill: Building MVC Views & Controllers

This guide covers creating new view pages and their controllers in the
`kinonode-ui` module, following the project's programmatic JavaFX MVC pattern.

## Architecture Overview

KinoNode uses a strict **Controller + View** separation:

- **Controller** (`controller/`): Business logic, event wiring, data loading.
  Receives dependencies via `@Inject` constructor. Exposes `buildView()` and
  `initialize()`.
- **View** (`view/pages/`): Pure UI construction. Receives `ResourceBundle` in
  constructor. Exposes `public` fields for controller binding.

## Reference Implementation

**Controller:** `AboutController.java` — simplest complete example  
**View:** `AboutView.java` — demonstrates the View class pattern  
**Complex Controller:** `LibraryController.java` — shows caching, tasks, navigation

## Step 1 — Create the View Class

**Location:** `kinonode-ui/src/main/java/io/github/kullik01/kinonode/view/pages/`

```java
package io.github.kullik01.kinonode.view.pages;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class NewFeatureView {

  private final ScrollPane rootPane;

  // Public fields for controller access — the controller reads/writes these
  public final Label lblTitle = new Label("");
  public final Label lblData = new Label("Loading...");

  public NewFeatureView(ResourceBundle resources) {
    rootPane = new ScrollPane();
    rootPane.setFitToWidth(true);
    rootPane.setFitToHeight(true);
    rootPane.setStyle("-fx-background-color: transparent;");
    rootPane.getStylesheets().add(
        getClass().getResource("/io/github/kullik01/kinonode/css/theme.css")
            .toExternalForm());

    VBox mainContent = new VBox(25.0);
    mainContent.getStyleClass().add("detail-pane");
    mainContent.setPadding(new Insets(30.0, 30.0, 30.0, 30.0));

    // Build UI components using resources for i18n strings
    lblTitle.setText(resources.getString("newfeature.title"));
    lblTitle.getStyleClass().add("title-1");

    mainContent.getChildren().addAll(lblTitle, lblData);
    rootPane.setContent(mainContent);
  }

  public Parent getRoot() {
    return rootPane;
  }
}
```

### Key View Conventions

- Root container is typically `ScrollPane` wrapping a `VBox`
- Always load `theme.css` stylesheet
- Use `detail-pane` style class on the main content VBox
- UI labels/fields are `public final` for direct controller access
- Constructor takes `ResourceBundle` for i18n string loading
- Expose `getRoot()` returning `Parent`

## Step 2 — Create the Controller

**Location:** `kinonode-ui/src/main/java/io/github/kullik01/kinonode/controller/`

```java
package io.github.kullik01.kinonode.controller;

import com.google.inject.Inject;
import io.github.kullik01.kinonode.repository.SomeRepository;
import io.github.kullik01.kinonode.view.pages.NewFeatureView;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Parent;

/**
 * Controller for the New Feature view. Manages data loading and user
 * interactions for the feature page.
 */
public class NewFeatureController implements Refreshable {
  private static final Logger LOGGER =
      Logger.getLogger(NewFeatureController.class.getName());

  private NewFeatureView view;

  private final SomeRepository someRepository;
  private final ResourceBundle resources;

  @Inject
  public NewFeatureController(SomeRepository someRepository,
      ResourceBundle resources) {
    this.someRepository = someRepository;
    this.resources = resources;
  }

  /**
   * Builds the view for this controller. Creates the view instance, wires
   * event handlers, and returns the root node.
   *
   * @return the root Parent node of this view
   */
  public Parent buildView() {
    view = new NewFeatureView(resources);

    // Wire event handlers here
    // view.btnAction.setOnAction(e -> handleAction());

    return view.getRoot();
  }

  /**
   * Initializes this controller by loading initial data into the view.
   */
  public void initialize() {
    loadData();
  }

  @Override
  public void refresh() {
    loadData();
  }

  private void loadData() {
    if (view == null) return;

    try {
      // Load data from repository and update view
      String tmpValue = someRepository.get("some_key", "default");
      view.lblData.setText(tmpValue);
    } catch (Exception tmpException) {
      LOGGER.log(Level.SEVERE, "Failed to load data", tmpException);
      view.lblData.setText("Error loading data");
    }
  }
}
```

### Key Controller Conventions

- BSD 3-Clause license header (omitted above for brevity — always include it)
- Static `LOGGER` as first field
- View field (`private NewFeatureView view`)
- Dependencies as `private final` fields
- `@Inject` on constructor — all dependencies injected
- `buildView()` creates view, wires handlers, returns `view.getRoot()`
- `initialize()` triggers initial data load
- Implement `Refreshable` if data should reload on tab navigation
- Null-check `view` at start of data loading methods

## Step 3 — Wire into MainController

If this is a new top-level tab visible in the sidebar:

### 3.1 Add Provider to Constructor

```java
private final Provider<NewFeatureController> newFeatureProvider;

@Inject
public MainController(
    // ... existing providers ...
    Provider<NewFeatureController> newFeatureProvider,
    // ...
) {
  // ... existing assignments ...
  this.newFeatureProvider = newFeatureProvider;
}
```

### 3.2 Add Sidebar Button

In `buildView()`, add after existing buttons:

```java
btnNewFeature = createSidebarButton(
    resources.getString("main.newfeature"), "btnNewFeature",
    "M12 2C6.48 2 2 6.48 2 12s...",  // SVG path data
    this::showNewFeature, false);
```

Add to sidebar: `sidebar.getChildren().addAll(..., btnNewFeature, ...);`

### 3.3 Add Navigation Method

```java
private void showNewFeature() {
  navigate("NewFeature", btnNewFeature);
}
```

### 3.4 Add createView() Case

```java
} else if ("NewFeature".equals(name)) {
  NewFeatureController controller = newFeatureProvider.get();
  view = controller.buildView();
  controller.initialize();
  views.put(name, view);
  controllers.put(name, controller);
}
```

### 3.5 Apply Cursor

In `initialize()`:

```java
CursorUtils.applyCursor(btnNewFeature);
```

## Step 4 — Update module-info.java

If the new controller or view introduces packages not yet exported/opened:

```java
// In kinonode-ui module-info.java:
opens io.github.kullik01.kinonode.controller to com.google.guice, javafx.fxml;
// view/pages is already exported
```

## Checklist

- [ ] View class created in `view/pages/` with `ResourceBundle` constructor
- [ ] View loads `theme.css` and uses `detail-pane` style class
- [ ] Controller has `@Inject` constructor with all dependencies
- [ ] Controller has `buildView()`, `initialize()`, and `refresh()` methods
- [ ] `Refreshable` implemented if data reloads on navigation
- [ ] Event handlers wired in `buildView()`, not in constructor
- [ ] All local variables use `tmp` prefix
- [ ] i18n keys added to `messages.properties`
- [ ] Sidebar button added in `MainController.buildView()`
- [ ] Navigation case added in `MainController.createView()`
- [ ] `CursorUtils.applyCursor()` called on interactive elements
