# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin Multiplatform (KMP) project with Compose Multiplatform UI and Ktor backend. Targets Android and Desktop (JVM) — no iOS. The project explores AI model parameters and integration (see `Challange-tasks/`).

## Build & Run Commands

```bash
# Desktop app
./gradlew :composeApp:run

# Android (debug APK)
./gradlew :composeApp:assembleDebug

# Ktor server (port 8080)
./gradlew :server:run

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :server:test
./gradlew :shared:testDebugUnitTest   # Android tests
./gradlew :shared:jvmTest             # JVM tests
./gradlew :composeApp:testDebugUnitTest
```

## Architecture

Three Gradle modules defined in `settings.gradle.kts`:

- **`shared/`** — Multiplatform library (Android + JVM). Contains `Platform` (expect/actual pattern), `Greeting`, `Constants` (SERVER_PORT). Package: `com.portfolio.ai_challange_with_love`
- **`composeApp/`** — Compose Multiplatform UI (Android + Desktop). Entry points: `MainActivity` (Android), `main.kt` (Desktop). Main composable: `App()`
- **`server/`** — Ktor 3.3.3 server with Netty. Entry point: `Application.kt`. Uses shared module for constants and greeting logic.

### Key Patterns

- **expect/actual**: Platform-specific implementations in `shared/` (`Platform.kt` → `Platform.android.kt`, `Platform.jvm.kt`)
- **Compose Resources**: Multiplatform resources in `commonMain/composeResources/`
- **Version catalog**: All dependency versions centralized in `gradle/libs.versions.toml`

## Tech Stack

- Kotlin 2.3.0, Compose Multiplatform 1.10.0, Ktor 3.3.3
- Android: compileSdk 36, minSdk 31, AGP 8.11.2
- Gradle 8.14.3 with configuration cache enabled
- Material3 for UI components

## API Keys

- Use `DEEPSEEK_API_KEY` (from `~/.zshrc`) for **all** AI API calls in this project
- Available models: `deepseek-chat` (V3.2), `deepseek-reasoner` (V3.2 thinking mode)
- API base URL: `https://api.deepseek.com` (OpenAI-compatible)

## DI Framework

- Use **Koin** for dependency injection, NOT Kodein
- All Compose feature generation must use Koin modules and `koinInject()` / `koinViewModel()`

## Koog (AI Agents Framework)

- When designing or implementing AI agent features, invoke `/koog` skill first (`.claude/skills/koog.md`)
  - Covers: architecture decisions, canonical patterns, anti-patterns, testing, MCP/A2A
- For quick API syntax/imports lookup, use `.claude/koog-reference.md`
- Use `ksrc` skill to inspect Koog source code for API details not covered in either reference

## Workflow

- After completing a task, always run `./gradlew :server:run` (background) and `./gradlew :composeApp:run` (background) so the user can test immediately
- Kill existing processes on port 8080 before restarting the server

## UI Language

- All UI text (labels, titles, descriptions) must be in **English**
- Challenge task content (from `Challange-tasks/`) stays in its original language (Ukrainian)
- Code comments and documentation in English

## Plans
After creating a plan, always copy it to .claude/plans/ in the project root with date prefix (e.g. 2026-02-27-day8-experiment.md). Never delete old plans.