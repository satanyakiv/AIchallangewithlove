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