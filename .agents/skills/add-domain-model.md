# Skill: Creating Domain Model Entities

This guide covers creating new domain model classes in the `kinonode-domain` module.

## Target Location

`kinonode-domain/src/main/java/io/github/kullik01/kinonode/model/`

## Reference Template

Use `Movie.java` as the canonical reference. Every domain model must follow
this exact structure.

## Required Structure

### 1. License Header

Every file begins with the BSD 3-Clause license block. Copy verbatim from
any existing source file — it includes `<p>` HTML tags between paragraphs.

### 2. Package Declaration

```java
package io.github.kullik01.kinonode.model;
```

### 3. Class Javadoc

```java
/**
 * Represents a [Entity] entity in the Kino-Node application. [Description
 * of what this entity models and its primary purpose.]
 */
```

### 4. Fields

- Use `private` access for all fields
- Use wrapper types (`Integer`, `Double`) for nullable fields
- Use `Instant` for timestamps
- Use end-of-line `//` annotation for units, ranges, or nullability

```java
private Integer id;
private String name;
private Integer year;          // Release year, nullable
private Double score;          // 0.0-10.0 or null
private Instant createdAt;
```

### 5. Constructors

```java
/** Constructs a new [Entity] with the current timestamp as the creation time. */
public EntityName() {
  this.createdAt = Instant.now();
}

/**
 * Constructs a new [Entity] with a specified name and sets the creation
 * time to the current timestamp.
 *
 * @param name the name of the entity
 */
public EntityName(String name) {
  this.name = name;
  this.createdAt = Instant.now();
}
```

### 6. Getters & Setters

Every getter and setter must have Javadoc:

```java
/**
 * Gets the unique identifier of the entity.
 *
 * @return the unique database ID, or null if not persisted
 */
public Integer getId() {
  return id;
}

/**
 * Sets the unique identifier of the entity.
 *
 * @param id the unique database ID to set
 */
public void setId(Integer id) {
  this.id = id;
}
```

### 7. toString() Override

```java
/**
 * Returns a string representation of the [Entity], including its ID and name.
 *
 * @return a string representing this [Entity]
 */
@Override
public String toString() {
  return "EntityName{" + "id=" + id + ", name='" + name + '\'' + '}';
}
```

## Module Configuration

If adding a new package (not just a new class in `model/`), update
`kinonode-domain/src/main/java/module-info.java`:

```java
module kinonode.domain {
    requires com.google.gson;

    exports io.github.kullik01.kinonode.model;
    exports io.github.kullik01.kinonode.util;
    // Add: exports io.github.kullik01.kinonode.newpackage;

    opens io.github.kullik01.kinonode.model to com.google.gson;
    // Add if JSON-serialized: opens io.github.kullik01.kinonode.newpackage to com.google.gson;
}
```

## Existing Domain Models

| Class | Purpose | Key Fields |
|---|---|---|
| `Movie` | A cataloged video file | `id`, `filePath`, `title`, `year`, `genre`, `rating`, `rawRating`, `director`, `writer`, `producer`, `cinematographer`, `composer`, `actors`, `duration`, `coverImagePath`, `trailerUrl`, `dateAdded` |
| `Watchlist` | User-curated ordered collection | `id`, `name`, `createdAt`, `movies` (List), `isSmart`, `smartRulesJson` |
| `SmartRule` | A filter condition for Smart Watchlists | `field`, `operator`, `value` |

## Checklist

- [ ] BSD 3-Clause license header present
- [ ] Class-level Javadoc present
- [ ] Private fields with appropriate types
- [ ] Default constructor initializing `Instant.now()` timestamps
- [ ] Parameterized constructor (if applicable)
- [ ] Getters and setters with Javadoc on each
- [ ] `toString()` override
- [ ] `module-info.java` updated if new package
