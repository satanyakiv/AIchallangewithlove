# CLAUDE.md

## Project Overview

KMP project: Compose Multiplatform UI + Ktor backend. Android + Desktop (JVM), no iOS.
Package: `com.portfolio.ai_challenge`

## Build & Run

```bash
./gradlew :composeApp:hotRunJvm --mainClass=com.portfolio.ai_challenge.MainKt  # Desktop (Hot Reload)
./gradlew :composeApp:assembleDebug # Android APK
./gradlew :server:run              # Ktor server (port 8080)
./gradlew :server:test --tests "*.ClassName"  # Server tests (ALWAYS with --tests filter)
./gradlew :server:dokkaGeneratePublicationHtml  # Server API docs → server/build/dokka/html/index.html
```

## Modules

| Module        | Purpose                                         |
|---------------|-------------------------------------------------|
| `shared/`     | KMP library: Platform expect/actual, Constants  |
| `composeApp/` | Compose UI: screens, ViewModels, navigation, DI |
| `database/`   | Room + SQLite KMP library                       |
| `server/`     | Ktor 3.4.2 + Netty: routes, agents, API         |

## Tech Stack

Kotlin 2.3.20, Compose 1.10.3, Ktor 3.4.2, AGP 9.1.0, Room 2.8.4, Koin 4.2.0, kotlinx-serialization 1.10.0, Navigation3 1.1.0-beta01, Koog 0.7.3, MockK 1.14.9. Full versions: `gradle/libs.versions.toml`.

## Key Rules (details in `.claude/rules/`)

Read before writing code:
- `.claude/rules/architecture.md` — layered arch, DI, types, file organization
- `.claude/rules/mapping.md` — AtoBMapper pattern for type conversions
- `.claude/rules/prompts.md` — prompt text in resources, loader/template patterns
- `.claude/rules/testing.md` — unit + integration, naming, what to test per component

- **Layered arch**: Routes (HTTP) → Agent (orchestration) → Components (logic) → Store (data)
- **SRP**: every class/function does ONE thing
- **DI**: Koin only, constructor injection, never create deps inside a class
- **Types**: sealed interface over strings. Zero hardcoded strings — pass the type, not `.name`. If enum/sealed doesn't exist, create it first.
- **Mappers**: dedicated `{A}To{B}Mapper` for type conversions, never inline in Agent/UseCase/Route
- **Size limits**: files < 150 lines, functions < 20 lines
- **Use Cases**: extract logic from Agent into `{Verb}{Noun}UseCase.kt` beyond simple orchestration
- **Test every mutation**: every data change (profile, session) needs 3 tests: happy, no-op, persistence
- **Models**: one class per file, `@Serializable`, prefix domain models (`PsyUserProfile`)
- **Prompts**: all prompt text in `server/src/main/resources/prompts/`, never inline in .kt files
- **Testing**: unit + integration per feature. Naming: `testWhat_condition_expected()`
- **NEVER run all tests blindly**. Integration tests call real APIs. Always use `--tests` flag.
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
- **Desktop app: ALWAYS run via Hot Reload** — `./gradlew :composeApp:hotRunJvm --mainClass=com.portfolio.ai_challenge.MainKt`. Never use plain `:composeApp:run`
- Do NOT restart server/desktop app after every change. Only restart for user testing if the task took 10+ minutes of active work
- **MANDATORY: When exiting Plan Mode**, IMMEDIATELY save the plan to `.claude/plans/YYYY-MM-DD-<topic>.md` BEFORE writing any code. This is the FIRST action after plan mode exit — no exceptions.
- After adding/changing public server API (agents, routes, use cases, interfaces): run `./gradlew :server:dokkaGeneratePublicationHtml`