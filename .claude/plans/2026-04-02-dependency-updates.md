# Plan: Update All Dependencies

## Context
Оновлення всіх залежностей проекту до останніх стабільних версій (станом на квітень 2026). Проект використовує Gradle version catalog (`gradle/libs.versions.toml`) для централізованого управління версіями. Усього 16 залежностей потребують оновлення.

## Step 1: Update versions in `gradle/libs.versions.toml`

**File:** `gradle/libs.versions.toml`

| Version key | Current | New |
|---|---|---|
| `kotlin` | 2.3.10 | 2.3.20 |
| `composeMultiplatform` | 1.10.2 | 1.10.3 |
| `ktor` | 3.4.1 | 3.4.2 |
| `dokka` | 2.1.0 | 2.2.0 |
| `kotlin-logging` | 7.0.7 | 7.0.13 |
| `koin` | 4.1.1 | 4.2.0 |
| `androidx-activity` | 1.12.4 | 1.13.0 |
| `androidx-lifecycle` | 2.9.6 | 2.10.0 |
| `lifecycle-kmp` | 2.10.0-beta01 | 2.10.0 |
| `nav3-runtime` | 1.1.0-alpha05 | 1.1.0-rc01 |
| `nav3-kmp` | 1.0.0-alpha06 | 1.1.0-beta01 |
| `material3` | 1.10.0-alpha05 | 1.11.0-alpha05 |
| `kotlinx-datetime` | 0.6.2 | 0.7.1 |
| `koog` | 0.6.4 | 0.7.3 |
| `kotest` | 5.9.1 | 6.1.4 |

## Step 2: Fix breaking changes

### 2a. kotlinx-datetime 0.6.2 → 0.7.1
- Fix import in `PersonalizeResponseUseCase.kt`: `kotlin.time.Instant` → `kotlinx.datetime.Instant`

### 2b. Koog 0.6.4 → 0.7.3
- Compile and fix if API changed in `Day6Agent.kt` and `ServerModule.kt`

### 2c. Kotest 5.9.1 → 6.1.4
- No code changes needed (not used in code)

## Verification
- All modules compile
- Unit tests pass