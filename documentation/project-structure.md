# Project File Structure

> AI Challenge With Love — Kotlin Multiplatform project
> Last updated: 2026-03-05

```
AIchallangewithlove/
├── CLAUDE.md                          # Claude Code project instructions
├── README.md
├── build.gradle.kts                   # Root build file
├── settings.gradle.kts                # Module declarations
├── gradle.properties
├── gradlew / gradlew.bat
│
├── gradle/
│   ├── libs.versions.toml             # Version catalog (all deps centralized)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── androidApp/                        # Android Application module (AGP 9)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/portfolio/ai_challenge/
│       │   ├── MainActivity.kt
│       │   └── AiChallengeApplication.kt
│       └── res/
│           ├── drawable[-v24]/        # Launcher icons (vector)
│           ├── mipmap-*/              # Launcher icons (raster, all densities)
│           └── values/strings.xml
│
├── composeApp/                        # Compose Multiplatform UI (Android + Desktop JVM)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── composeResources/drawable/
│       │   │   └── compose-multiplatform.xml
│       │   └── kotlin/com/portfolio/ai_challenge/
│       │       ├── App.kt                          # Root composable + navigation
│       │       ├── navigation/
│       │       │   └── AppScreen.kt                # Sealed class screen definitions
│       │       ├── model/
│       │       │   └── ChallengeDay.kt
│       │       ├── data/
│       │       │   ├── AgentApi.kt
│       │       │   ├── HttpClientFactory.kt
│       │       │   ├── ModelApi.kt
│       │       │   ├── TemperatureApi.kt
│       │       │   └── PlatformConfig.kt           # expect declaration
│       │       ├── di/
│       │       │   ├── AppModule.kt                # Koin DI modules
│       │       │   └── NetworkModule.kt
│       │       └── ui/
│       │           ├── TextFormatter.kt
│       │           ├── theme/
│       │           │   └── Theme.kt
│       │           ├── components/
│       │           │   ├── ChallengeCard.kt
│       │           │   └── ChatComponents.kt       # Shared chat UI components
│       │           └── screen/
│       │               ├── ChatMessage.kt
│       │               ├── MainScreen.kt           # Home / day picker
│       │               ├── Day4Screen.kt + Day4ViewModel.kt
│       │               ├── Day5Screen.kt + Day5ViewModel.kt
│       │               ├── Day6Screen.kt + Day6ViewModel.kt
│       │               ├── Day7Screen.kt + Day7ViewModel.kt
│       │               ├── Day8Screen.kt
│       │               ├── Day9Screen.kt + Day9ViewModel.kt
│       │               ├── Day10HubScreen.kt       # Day 10 entry point
│       │               ├── Day10SlidingScreen.kt + Day10SlidingViewModel.kt
│       │               ├── Day10FactsScreen.kt + Day10FactsViewModel.kt
│       │               ├── Day10BranchingScreen.kt + Day10BranchingViewModel.kt
│       │               ├── Day10SharedComponents.kt
│       │               ├── Day10ComparisonScreen.kt
│       │               └── Day10ComparisonData.kt
│       ├── androidMain/kotlin/com/portfolio/ai_challenge/data/
│       │   └── PlatformConfig.kt                   # Android actual
│       ├── jvmMain/kotlin/com/portfolio/ai_challenge/
│       │   ├── main.kt                             # Desktop entry point
│       │   └── data/PlatformConfig.kt              # JVM actual
│       └── commonTest/kotlin/com/portfolio/ai_challenge/data/
│           └── AgentApiTest.kt
│
├── shared/                            # Common KMP library (platform detection, constants)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/portfolio/ai_challenge/
│       │   ├── Constants.kt           # SERVER_PORT and shared constants
│       │   ├── Greeting.kt
│       │   └── Platform.kt            # expect class
│       ├── androidMain/kotlin/com/portfolio/ai_challenge/
│       │   └── Platform.android.kt    # actual
│       └── jvmMain/kotlin/com/portfolio/ai_challenge/
│           └── Platform.jvm.kt        # actual
│
├── database/                          # Room + SQLite KMP library
│   ├── build.gradle.kts
│   ├── schemas/                       # Room auto-migration schemas (v1, v2)
│   └── src/
│       ├── commonMain/kotlin/com/portfolio/ai_challenge/database/
│       │   ├── ChatDatabase.kt        # @Database declaration (v2, AutoMigration 1->2)
│       │   ├── ChatMessageDao.kt
│       │   ├── ChatMessageEntity.kt
│       │   ├── ChatRepository.kt
│       │   ├── DatabaseFactory.kt     # expect
│       │   ├── Day10MessageEntity.kt
│       │   ├── Day10BranchEntity.kt
│       │   ├── Day10FactEntity.kt
│       │   ├── Day10Dao.kt
│       │   ├── Day10Repository.kt
│       │   └── di/DatabaseModule.kt
│       ├── androidMain/kotlin/com/portfolio/ai_challenge/database/
│       │   ├── DatabaseFactory.android.kt
│       │   └── di/DatabaseModule.android.kt
│       └── jvmMain/kotlin/com/portfolio/ai_challenge/database/
│           ├── DatabaseFactory.jvm.kt
│           └── di/DatabaseModule.jvm.kt
│
├── server/                            # Ktor backend (port 8080)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/portfolio/ai_challenge/
│       │   │   ├── Application.kt     # Ktor entry point, route registration
│       │   │   ├── agent/
│       │   │   │   ├── Day6Agent.kt
│       │   │   │   ├── Day7Agent.kt
│       │   │   │   ├── Day8Agent.kt
│       │   │   │   ├── Day9Agent.kt
│       │   │   │   ├── Day10SlidingAgent.kt
│       │   │   │   ├── Day10FactsAgent.kt
│       │   │   │   └── Day10BranchingAgent.kt
│       │   │   ├── routes/
│       │   │   │   ├── AgentRoutes.kt
│       │   │   │   ├── ModelRoutes.kt
│       │   │   │   └── TemperatureRoutes.kt
│       │   │   ├── models/
│       │   │   │   ├── DeepSeekModels.kt
│       │   │   │   └── TokenUsage.kt
│       │   │   └── experiment/
│       │   │       ├── Day8ExperimentRunner.kt
│       │   │       ├── ExperimentLogger.kt
│       │   │       ├── TestDataParser.kt
│       │   │       └── models/
│       │   │           ├── ExperimentResult.kt
│       │   │           ├── StepResult.kt
│       │   │           └── TestCase.kt
│       │   └── resources/logback.xml
│       └── test/
│           ├── kotlin/com/portfolio/ai_challenge/
│           │   ├── Day7AgentTest.kt
│           │   ├── Day7AgentRouteTest.kt
│           │   ├── Day8SmokeTest.kt
│           │   ├── Day8FullExperimentTest.kt
│           │   ├── Day8OverflowExperimentTest.kt
│           │   ├── Day10IntegrationTest.kt
│           │   └── TestDataParserTest.kt
│           └── resources/
│               ├── day-8-test-data/   # Input cases for Day 8 experiments
│               └── day-8-results/     # Saved experiment results + evaluations
│
└── documentation/
    ├── project-structure.md           # This file
    ├── challenge-tasks/               # Task descriptions (day-4 through day-15)
    │   └── day-{4..15}.txt
    └── day-11-to-15/                  # Architecture diagrams for Psy-Agent
        ├── psy-agent-class-diagram.md
        ├── psy-agent-sequence-diagram.md
        └── psy-agent-state-diagram.md
```

## Module Summary

| Module | Type | Description |
|--------|------|-------------|
| `:androidApp` | Android Application | Entry point, `MainActivity`, `Application` class, resources |
| `:composeApp` | KMP Library (Android + JVM) | All UI screens, ViewModels, navigation, DI |
| `:shared` | KMP Library (Android + JVM) | Platform detection, shared constants |
| `:database` | KMP Library (Android + JVM) | Room database, DAOs, repositories |
| `:server` | JVM (Ktor) | REST API, AI agents, experiment runner |

## API Routes (server, port 8080)

| Route | Agent | Day |
|-------|-------|-----|
| `POST /api/agent/chat` | Day6Agent | 6 |
| `POST /api/agent/chat-v7` | Day7Agent | 7 |
| `POST /api/agent/chat-v8` | Day8Agent | 8 |
| `POST /api/agent/chat-v9` | Day9Agent | 9 |
| `POST /api/agent/chat-v10/sliding` | Day10SlidingAgent | 10 |
| `POST /api/agent/chat-v10/facts` | Day10FactsAgent | 10 |
| `POST /api/agent/chat-v10/branching` | Day10BranchingAgent | 10 |
