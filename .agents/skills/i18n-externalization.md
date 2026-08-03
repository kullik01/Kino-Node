# Skill: i18n String Externalization

This guide covers externalizing UI strings into the ResourceBundle-based
internationalization system used by KinoNode.

## System Overview

KinoNode uses Java's `ResourceBundle` mechanism:

- **Properties file:** `kinonode-ui/src/main/resources/io/github/kullik01/kinonode/i18n/messages.properties`
- **Guice binding:** `ResourceBundle` is provided as a `@Singleton` via `KinoNodeModule`
- **Injection:** Controllers receive `ResourceBundle resources` via `@Inject` constructor
- **Views:** Receive `ResourceBundle resources` as a constructor parameter

## Step 1 — Add Keys to messages.properties

### Naming Convention

Use hierarchical dot-separated keys: `section.subsection.key`

```properties
# Section comment describing the group
section.label.name=Display Name
section.tooltip.hint=Tooltip text
section.btn.action=Button Label
section.empty.message=Empty state message text
```

### Existing Key Prefixes

| Prefix | Used By |
|---|---|
| `app.*` | Application-level metadata |
| `main.*` | `MainController` (sidebar labels, theme) |
| `about.*` | `AboutController` / `AboutView` |
| `library.*` | `LibraryController` (toolbar, sorting, empty states) |
| `menu.*` | Menu bar items |
| `button.*` | Shared button labels |

### Rules

- Group related keys together with a section comment
- Use descriptive suffixes: `.label.`, `.btn.`, `.tooltip.`, `.title`, `.text`
- Escape special characters: `©` → `\u00A9`, `—` → `\u2014`, `•` → `\u2022`
- Keep values on a single line (no line breaks in property values)

## Step 2 — Inject ResourceBundle in Controller

If the controller already has `ResourceBundle resources` injected, skip this step.

Otherwise, add it to the `@Inject` constructor:

```java
private final ResourceBundle resources;

@Inject
public NewController(SomeRepository repo, ResourceBundle resources) {
  this.someRepository = repo;
  this.resources = resources;
}
```

## Step 3 — Pass ResourceBundle to View

Views receive the bundle as a constructor parameter:

```java
// In controller's buildView():
public Parent buildView() {
  view = new NewView(resources);
  return view.getRoot();
}

// In view's constructor:
public NewView(ResourceBundle resources) {
  Label tmpLabel = new Label(resources.getString("section.label.name"));
  // ...
}
```

## Step 4 — Replace Hardcoded Strings

### Before

```java
Label title = new Label("Library");
Button btn = new Button("Sync Library");
Tooltip tip = new Tooltip("Light Mode");
```

### After

```java
Label title = new Label(resources.getString("library.title"));
Button btn = new Button(resources.getString("library.empty.sync_btn"));
Tooltip tip = new Tooltip(resources.getString("main.theme.light"));
```

## Common Patterns

### Labels with Dynamic Values

Do NOT concatenate translated strings. Use `String.format()`:

```java
// Good:
view.lblCount.setText(tmpCount + (tmpCount == 1 ? " movie" : " movies"));

// For translatable dynamic values in the future:
// String.format(resources.getString("library.count.format"), tmpCount);
```

### Tooltips

```java
Tooltip tmpTooltip = new Tooltip(resources.getString("section.tooltip.hint"));
tmpTooltip.setShowDelay(Duration.millis(250));
someControl.setTooltip(tmpTooltip);
```

### ComboBox Items

```java
cmbSort.getItems().addAll(
    resources.getString("library.sort.title"),
    resources.getString("library.sort.year"),
    resources.getString("library.sort.rating"));
```

## Checklist

- [ ] Keys added to `messages.properties` with hierarchical naming
- [ ] Section comment added above new key group
- [ ] `ResourceBundle` injected in controller constructor
- [ ] `ResourceBundle` passed to view constructor
- [ ] All user-visible strings use `resources.getString()`
- [ ] Special characters properly escaped
- [ ] No string concatenation of translated fragments
