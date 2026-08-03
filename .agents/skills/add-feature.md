# Skill: End-to-End Feature Implementation

This guide walks through the complete workflow for adding a new feature to
KinoNode, from domain modeling to UI integration.

## Prerequisites

- Read `GEMINI.md` for mandatory coding standards
- Read `standards/architecture.md` for module boundaries

## Step-by-Step Workflow

### Step 1 — Define or Extend the Domain Model

**Module:** `kinonode-domain` → `src/main/java/.../model/`

If your feature requires a new entity or new fields on an existing entity:

- New entity: Create a class following `Movie.java` as the reference template
  (see `skills/add-domain-model.md` for full details)
- New field on existing entity: Add private field, getter, setter with Javadoc
- Update `module-info.java` if introducing a new package

### Step 2 — Add Database Schema Migration

**Module:** `kinonode-data` → `DatabaseService.createTables()`

Add new columns or tables using the safe migration pattern:

```java
// Inside createTables(), after existing migrations:
try {
  tmpStatement.execute("ALTER TABLE movies ADD COLUMN new_field TEXT;");
  LOGGER.info("Added new column to movies: new_field");
} catch (SQLException tmpException) {
  // Column already exists — safe to ignore
}
```

See `skills/database-migration.md` for complete patterns.

### Step 3 — Update Repository CRUD Operations

**Module:** `kinonode-data` → `src/main/java/.../repository/`

Update the relevant repository to read/write the new data:

- Add the new column to `INSERT` and `SELECT` statements
- Use `PreparedStatement` with `?` placeholders — never concatenate
- Prefix all local variables with `tmp`

```java
// Example: Adding to a SELECT mapping
String tmpNewField = tmpResultSet.getString("new_field");
tmpMovie.setNewField(tmpNewField);
```

### Step 4 — Add i18n Keys

**Module:** `kinonode-ui` → `src/main/resources/.../i18n/messages.properties`

Add key-value pairs using hierarchical `section.subsection.key` naming:

```properties
# New Feature Section
feature.label.name=Display Name
feature.tooltip.hint=Helpful tooltip text
```

See `skills/i18n-externalization.md` for conventions.

### Step 5 — Build the View (if needed)

**Module:** `kinonode-ui` → `src/main/java/.../view/pages/`

If the feature needs its own page/panel, create a View class:

```java
public class NewFeatureView {
  private final ScrollPane rootPane;
  public final Label lblSomeData = new Label("Default");
  // ... public fields for controller access

  public NewFeatureView(ResourceBundle resources) {
    rootPane = new ScrollPane();
    // Build the UI programmatically
  }

  public Parent getRoot() {
    return rootPane;
  }
}
```

See `skills/add-view-controller.md` for the full pattern.

### Step 6 — Build the Controller

**Module:** `kinonode-ui` → `src/main/java/.../controller/`

Create a controller with `@Inject` constructor injection:

```java
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

  public Parent buildView() {
    view = new NewFeatureView(resources);
    // Wire event handlers here
    return view.getRoot();
  }

  public void initialize() {
    loadData();
  }

  @Override
  public void refresh() {
    loadData();
  }

  private void loadData() {
    // Fetch data and update view
  }
}
```

### Step 7 — Register in Guice (if new singleton)

**Module:** `kinonode-ui` → `di/KinoNodeModule.java`

```java
@Override
protected void configure() {
  // ... existing bindings
  bind(NewService.class).in(Singleton.class);
}
```

### Step 8 — Wire into MainController Navigation (if top-level tab)

**Module:** `kinonode-ui` → `controller/MainController.java`

1. Add `Provider<NewFeatureController>` to the constructor
2. Add sidebar button in `buildView()` using `createSidebarButton()`
3. Add navigation method: `private void showNewFeature() { navigate("NewFeature", btnNewFeature); }`
4. Add case in `createView()`:

```java
} else if ("NewFeature".equals(name)) {
  NewFeatureController controller = newFeatureProvider.get();
  view = controller.buildView();
  controller.initialize();
  views.put(name, view);
  controllers.put(name, controller);
}
```

### Step 9 — Build & Verify

```bash
./gradlew spotlessApply   # Auto-format code
./gradlew build           # Compile and run tests
./gradlew run             # Manual verification
```

## Checklist

- [ ] Domain model created/updated with Javadoc
- [ ] Database migration added (if schema changed)
- [ ] Repository CRUD updated
- [ ] i18n keys added to `messages.properties`
- [ ] View class created (if new page)
- [ ] Controller created with `@Inject` constructor
- [ ] `Refreshable` implemented (if data reloads on tab switch)
- [ ] Guice binding added (if new singleton)
- [ ] `MainController` updated (if new top-level tab)
- [ ] `module-info.java` updated (if new packages)
- [ ] `./gradlew spotlessApply` passes
- [ ] `./gradlew build` succeeds
