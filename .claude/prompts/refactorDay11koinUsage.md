Read .claude/rules/architecture.md (Dependency Injection section).
Read server/src/main/kotlin/com/portfolio/ai_challenge/Application.kt

## Problem

Application.kt manually creates all dependencies via constructors.
Koin is in the project (libs.versions.toml, used in composeApp) but
NOT used on the server side. This violates the DI rule.

## Task

Set up Koin on the server side. Move ALL dependency creation from
Application.module() into Koin modules.

### 1. Create server DI module

Create server/src/main/kotlin/com/portfolio/ai_challenge/di/ServerModule.kt:
```kotlin
val serverModule = module {
    // Infrastructure
    single {
        HttpClient(CIO) { engine { requestTimeout = 120_000 } }
    }
    single {
        System.getenv("DEEPSEEK_API_KEY")
            ?: error("DEEPSEEK_API_KEY environment variable is not set")
    }

    // Shared
    single { LlmClient(get(), get()) }

    // Legacy agents (Day 6-10)
    single { Day6Agent(get()) }
    single { Day7Agent(get(), get()) }
    single { Day9Agent(get(), get()) }
    single { Day10SlidingAgent(get(), get()) }
    single { Day10FactsAgent(get(), get()) }
    single { Day10BranchingAgent(get(), get()) }

    // Psy-Agent (Day 11+)
    single<ContextStore> { InMemoryContextStore() }
    single { ContextWindowManager() }
    single { PsyPromptBuilder(get()) }
    single { PsyResponseMapper() }
    single { PsyAgent(get(), get(), get()) }
}
```

### 2. Install Koin in Application.kt
```kotlin
fun Application.module() {
    install(Koin) { modules(serverModule) }

    install(ServerContentNegotiation) { ... }
    install(CORS) { ... }

    routing {
        get("/") { call.respondText("Ktor: ${Greeting().greet()}") }
        temperatureRoutes(get(), get())
        modelRoutes(get(), get())
        agentRoutes(get())
        agentV7Routes(get())
        agentV9Routes(get())
        agentV10Routes(get(), get(), get())
        psyAgentRoutes(get(), get())
    }
}
```

### 3. Add Koin Ktor dependency if missing

Check server/build.gradle.kts. If koin-ktor is not there, add:
- io.insert-koin:koin-ktor (same version as koin-core in libs.versions.toml)

Also check if the `install(Koin)` extension is available.
If the ktor-koin plugin is not available for this Koin version,
use the manual approach:
```kotlin
fun Application.module() {
    val koin = startKoin { modules(serverModule) }.koin

    routing {
        agentRoutes(koin.get())
        psyAgentRoutes(koin.get(), koin.get())
        // ...
    }
}
```

### 4. Update route functions

Route functions should accept typed parameters (they already do),
so no changes needed in route files.

### 5. Verification

- ./gradlew :server:test — all tests pass
- ./gradlew :server:run — server starts
- POST /api/agent/psy/start + /psy/chat — same behavior as before
- Application.kt has ZERO direct constructor calls for agents or components
- All dependency creation is in ServerModule.kt