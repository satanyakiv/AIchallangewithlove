# Plan: Infrastructure Upgrade — 7 Libraries Integration

## Context

The project (KMP: Compose Multiplatform + Ktor backend) lacks several infrastructure libraries that would improve code quality, error handling, testing, logging, and configuration. This plan integrates 7 libraries in dependency order — from zero-risk additive deps to deeper refactors.

**Current state**: Logback 1.5.32 (no kotlin-logging wrapper), raw `System.getenv()` for config, exception-based error flow, `assertEquals`-only tests, no static analysis, no kotlinx.datetime, 1 java.time usage.

**Decision**: `kotlinx.collections.immutable` **deferred** — ConcurrentHashMap in InMemoryContextStore is correct for server-side concurrent mutable access. PersistentMap + AtomicReference would add complexity without benefit.

---

## Phase 1: Zero-risk, additive only (no existing code changes)

### Step 1: Detekt — static analysis

**Files to modify:**
- `gradle/libs.versions.toml` — add version + plugin
- `build.gradle.kts` (root) — declare plugin `apply false`
- `server/build.gradle.kts` — apply plugin
- NEW: `config/detekt/detekt.yml` — generated then customized

**Actions:**
1. Add to `libs.versions.toml` `[versions]`: `detekt = "1.23.8"`, `[plugins]`: `detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }`
2. Root `build.gradle.kts`: add `alias(libs.plugins.detekt) apply false`
3. `server/build.gradle.kts`: add `alias(libs.plugins.detekt)` in plugins block
4. Run `./gradlew detektGenerateConfig` to create `config/detekt/detekt.yml`
5. Customize: `LargeClass` threshold = 150, `LongMethod` = 20, disable `FunctionNaming` for composeApp (PascalCase composables)

**Verify:** `./gradlew detekt` — generates report, no build failure

### Step 2: AssertK — fluent test assertions

**Files to modify:**
- `gradle/libs.versions.toml` — add version + library
- `server/build.gradle.kts` — add testImplementation

**Actions:**
1. `[versions]`: `assertk = "0.28.1"`, `[libraries]`: `assertk = { module = "com.willowtreeapps.assertk:assertk", version.ref = "assertk" }`
2. `server/build.gradle.kts`: `testImplementation(libs.assertk)`
3. Do NOT migrate existing tests — new tests use AssertK going forward

**Verify:** Write 1 test with `assertThat(x).isEqualTo(y)`, run `--tests "*NewTest"`

### Step 3: Kotest property-based testing

**Files to modify:**
- `gradle/libs.versions.toml` — add version + libraries
- `server/build.gradle.kts` — add testImplementation

**Actions:**
1. `[versions]`: `kotest = "5.9.1"`, `[libraries]`: `kotest-property = { module = "io.kotest:kotest-property", version.ref = "kotest" }`
2. `server/build.gradle.kts`: `testImplementation(libs.kotest.property)`
3. Do NOT add Kotest runner — coexists with JUnit. Use `checkAll(Arb.string()) { ... }` inside `@Test` methods
4. Good candidates for property tests: `ProfileExtractor`, `InvariantChecker`, `DetectCrisisUseCase`

**Verify:** Write 1 property test, run `--tests "*PropertyTest"`

---

## Phase 2: Low-risk, thin wrappers

### Step 4: kotlin-logging — lazy SLF4J wrapper

**Files to modify:**
- `gradle/libs.versions.toml` — add version + library
- `server/build.gradle.kts` — add implementation
- ~5 server .kt files — add logger

**Actions:**
1. `[versions]`: `kotlin-logging = "7.0.7"`, `[libraries]`: `kotlin-logging = { module = "io.github.oshai:kotlin-logging-jvm", version.ref = "kotlin-logging" }`
2. `server/build.gradle.kts`: `implementation(libs.kotlin.logging)`
3. Add `private val logger = KotlinLogging.logger {}` to key files:
   - `LlmClient.kt` — `logger.warn { "DeepSeek error ($status): $body" }` before throwing
   - `ValidateAndRetryUseCase.kt` — `logger.info { "Retry $attemptCount: ${hardBlock.invariantName}" }`
   - `Day15PsyAgent.kt` — `logger.debug { "Session $sessionId: state=${state}" }`
   - `InMemoryContextStore.kt` — `logger.debug { "Session created: $sessionId" }`
4. Compatible with existing Logback 1.5.32 (SLF4J 2.x) — zero config changes

**Verify:** `./gradlew :server:run`, send request, confirm structured logs in console

### Step 5: kotlinx.datetime — KMP date/time

**Files to modify:**
- `gradle/libs.versions.toml` — add version + library
- `server/build.gradle.kts` — add implementation
- `server/.../PersonalizeResponseUseCase.kt` lines 53-54 — replace java.time

**Actions:**
1. `[versions]`: `kotlinx-datetime = "0.6.2"`, `[libraries]`: `kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }`
2. `server/build.gradle.kts`: `implementation(libs.kotlinx.datetime)`
3. Replace in `PersonalizeResponseUseCase.kt:53-54`:
   ```kotlin
   // Before:
   val date = java.time.Instant.ofEpochMilli(s.timestampMs)
       .atZone(java.time.ZoneOffset.UTC).toLocalDate()
   // After:
   val date = Instant.fromEpochMilliseconds(s.timestampMs)
       .toLocalDateTime(TimeZone.UTC).date
   ```
4. Leave all 33 `System.currentTimeMillis()` calls as-is — Long timestamps work fine in Room entities and JSON serialization

**Verify:** `./gradlew :server:test --tests "*Day12PersonalizationTest"`

---

## Phase 3: Medium-risk, structural changes

### Step 6: Hoplite — type-safe configuration

**Files to modify:**
- `gradle/libs.versions.toml` — add version + 2 libraries
- `server/build.gradle.kts` — add 2 implementations
- NEW: `server/.../config/ServerConfig.kt` — config data classes
- NEW: `server/src/main/resources/application.yaml` — config file
- `server/.../di/ServerModule.kt` lines 46-55 — replace raw env vars with config injection

**Actions:**
1. `[versions]`: `hoplite = "2.9.0"`, `[libraries]`: `hoplite-core` + `hoplite-yaml`
2. Create `ServerConfig.kt`:
   ```kotlin
   data class ServerConfig(val deepseek: DeepSeekConfig, val server: HttpConfig = HttpConfig())
   data class DeepSeekConfig(val apiKey: String, val apiUrl: String = "https://api.deepseek.com/chat/completions", val model: String = "deepseek-chat", val requestTimeoutMs: Long = 120_000)
   data class HttpConfig(val port: Int = 8080, val host: String = "0.0.0.0")
   ```
3. Create `application.yaml` with `${DEEPSEEK_API_KEY}` env var substitution
4. Refactor `ServerModule.kt`:
   - Replace `single { System.getenv("DEEPSEEK_API_KEY") ?: error(...) }` with `single { loadServerConfig() }`
   - `LlmClient(get(), get<ServerConfig>().deepseek.apiKey, get<ServerConfig>().deepseek.apiUrl, get<ServerConfig>().deepseek.model)`
   - `HttpClient(CIO) { engine { requestTimeout = get<ServerConfig>().deepseek.requestTimeoutMs } }`

**Verify:** `./gradlew :server:run` — server starts on :8080, API key resolves. Then `./gradlew :server:test --tests "*Day7AgentRouteTest"`

### Step 7: kotlin-result — typed error handling

**Files to modify:**
- `gradle/libs.versions.toml` — add version + library
- `server/build.gradle.kts` — add implementation
- NEW: `server/.../models/LlmError.kt` — error hierarchy
- `server/.../models/LlmClient.kt` — return Result instead of throw
- `server/.../agent/psy_agent/ValidateAndRetryUseCase.kt` — handle Result
- Agent files (Day7Agent, Day12-15PsyAgent) — unwrap Result
- Route files — replace try-catch with fold

**Phased migration (do in order):**

**7a — Error hierarchy** (new file):
```kotlin
sealed interface LlmError {
    data class HttpError(val statusCode: Int, val body: String) : LlmError
    data class ParseError(val cause: Throwable) : LlmError
    data class Timeout(val message: String) : LlmError
}
```

**7b — LlmClient** (modify `LlmClient.kt`):
- `complete()` → returns `Result<String, LlmError>`
- `completeWithResponse()` → returns `Result<DeepSeekResponse, LlmError>`
- Replace `throw Exception(...)` with `Err(LlmError.HttpError(...))`
- Wrap JSON parsing: `runCatching { json.decodeFromString(...) }.mapError { LlmError.ParseError(it) }`

**7c — ValidateAndRetryUseCase**: handle `Result` from `llmClient.complete()`

**7d — Agents**: each agent unwraps Result, one at a time

**7e — Routes**: replace `try { } catch { }` with `result.fold(success = { ... }, failure = { ... })`

**Verify per phase:**
- 7b: `./gradlew :server:test --tests "*Day7AgentTest"`
- 7c: `./gradlew :server:test --tests "*Day14InvariantTest"`
- 7d: `./gradlew :server:test --tests "*Day15TaskLifecycleTest"`
- 7e: `./gradlew :server:test --tests "*Day7AgentRouteTest"`

---

## Risk Summary

| Step | Library | Risk | Blast radius |
|------|---------|------|-------------|
| 1 | Detekt | Negligible | 0 code files |
| 2 | AssertK | Negligible | 0 code files |
| 3 | Kotest | Negligible | 0 code files |
| 4 | kotlin-logging | Low | ~5 files gain logger |
| 5 | kotlinx.datetime | Low | 1 file (2 lines) |
| 6 | Hoplite | Medium | ServerModule.kt + new config |
| 7 | kotlin-result | High | LlmClient + all agents + all routes (~15 files) |

## Final Verification

After all steps: `./gradlew :server:test --tests "*Test"` (unit tests only, never integration). Then `./gradlew detekt` for static analysis baseline.
