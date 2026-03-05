# CLAUDE.md

## Project Overview

KMP project: Compose Multiplatform UI + Ktor backend. Android + Desktop (JVM), no iOS.
Package: `com.portfolio.ai_challenge`

## Build & Run

```bash
./gradlew :composeApp:run          # Desktop app
./gradlew :composeApp:assembleDebug # Android APK
./gradlew :server:run              # Ktor server (port 8080)
./gradlew test                     # All tests
./gradlew :server:test             # Server tests only
```

## Modules

| Module | Purpose |
|--------|---------|
| `shared/` | KMP library: Platform expect/actual, Constants |
| `composeApp/` | Compose UI: screens, ViewModels, navigation, DI |
| `database/` | Room + SQLite KMP library |
| `server/` | Ktor 3.4.1 + Netty: routes, agents, API |

## Tech Stack

Kotlin 2.3.10, Compose 1.10.2, Ktor 3.4.1, AGP 9.1.0, Room 2.8.4, Koin 4.1.1, kotlinx-serialization 1.10.0, Navigation3 1.0.0-alpha06, Koog 0.6.4, MockK 1.14.9. Full versions: `gradle/libs.versions.toml`.

## Key Rules

Before writing code, read `.claude/rules/architecture.md` — mandatory for ALL server features.

- **Layered arch**: Routes (HTTP) → Agent (orchestration) → Components (logic) → Store (data)
- **SRP**: every class/function does ONE thing
- **DI**: Koin only, constructor injection, never create deps inside a class
- **Types**: sealed interface over strings for states, events, results
- **Size limits**: files < 150 lines, functions < 20 lines
- **Models**: one class per file, `@Serializable`, prefix domain models (`PsyUserProfile`)
- **Testing**: unit + integration per feature. Naming: `testWhat_condition_expected()`
- **UI**: English text, follow Day7-Day10 patterns, register in AppScreen/ChallengeDay/MainScreen/App.kt

## API

`DEEPSEEK_API_KEY` from `~/.zshrc`. Models: `deepseek-chat`, `deepseek-reasoner`. URL: `https://api.deepseek.com`

## DI

Koin only. `koinInject()` / `koinViewModel()` in Compose.

## Koog

Invoke `/koog` skill first for AI agent features. Reference: `~/.claude/koog-reference.md`

## Psy-Agent (Days 11-15)

Read `documentation/day-11-to-15/` diagrams before any psy-agent work.

## Workflow

- Kill port 8080 before server restart
- After task: run server + desktop app for user testing
- Save plans to `.claude/plans/` with date prefix