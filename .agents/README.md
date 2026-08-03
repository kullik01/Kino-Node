# .agents/ — KinoNode AI Agent Knowledge Base

This directory contains structured knowledge files that help AI coding assistants
understand the KinoNode project's architecture, coding standards, and common workflows.

## Directory Structure

```
.agents/
├── README.md                      ← You are here
├── skills/                        ← Task-specific step-by-step playbooks
│   ├── add-feature.md             ← End-to-end feature implementation workflow
│   ├── add-domain-model.md        ← Creating new domain model entities
│   ├── add-view-controller.md     ← Building new MVC views & controllers
│   ├── database-migration.md      ← SQLite schema evolution patterns
│   └── i18n-externalization.md    ← Externalizing UI strings
├── standards/                     ← Coding conventions & architecture references
│   ├── java-conventions.md        ← Java coding standards reference
│   └── architecture.md            ← Module structure & design patterns
└── context/                       ← Domain-specific knowledge
    ├── domain-glossary.md         ← KinoNode domain terminology
    └── database-schema.md         ← Complete SQLite schema reference
```

## How to Use

These files are designed to be read by AI agents as context when working on
this repository. They complement the root `GEMINI.md` file with detailed,
task-specific guidance.

- **Starting a new feature?** → Read `skills/add-feature.md`
- **Adding a new entity?** → Read `skills/add-domain-model.md`
- **Creating a new page/tab?** → Read `skills/add-view-controller.md`
- **Changing the database?** → Read `skills/database-migration.md`
- **Adding UI text?** → Read `skills/i18n-externalization.md`
- **Unsure about a convention?** → Read `standards/java-conventions.md`
- **Understanding the architecture?** → Read `standards/architecture.md`
- **Unfamiliar with a domain term?** → Read `context/domain-glossary.md`
