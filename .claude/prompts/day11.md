Read the architecture diagrams:
- documentation/day-11-to-15/psy-agent-class-diagram.md
- documentation/day-11-to-15/psy-agent-sequence-diagram.md
- documentation/day-11-to-15/psy-agent-state-diagram.md

Read the project structure: documentation/project-structure.md

Study existing patterns before writing code:
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/ — how existing agents are structured
- server/src/main/kotlin/com/portfolio/ai_challenge/routes/AgentRoutes.kt — how routes are registered
- server/src/main/kotlin/com/portfolio/ai_challenge/models/DeepSeekModels.kt — existing API models
- database/src/commonMain/kotlin/com/portfolio/ai_challenge/database/ — how Room entities and DAOs are structured
- composeApp/src/commonMain/kotlin/com/portfolio/ai_challenge/ui/screen/ — how screens and ViewModels are structured
- composeApp/src/commonMain/kotlin/com/portfolio/ai_challenge/di/AppModule.kt — how Koin DI is set up

Follow the EXACT naming conventions, DI patterns, and code style from existing code.

## Task

Implement the memory model with 3 layers for a mental health AI assistant (MindGuard / Psy-Agent).

## Server side

### Models: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/model/

Create these data classes:

TurnContext.kt:
- currentMessage: String
- plan: String? = null
- attemptCount: Int = 0
- lastViolation: String? = null

ConversationEntry.kt:
- role: String ("user" / "assistant")
- content: String
- timestamp: Long
- metadata: Map<String, String> = emptyMap()

PsySessionContext.kt:
- sessionId: String
- userId: String
- startedAt: Long
- messages: MutableList<ConversationEntry>
- detectedEmotions: MutableList<String>
- appliedTechniques: MutableList<String>

PsyUserProfile.kt:
- userId: String
- createdAt: Long
- preferredName: String
- primaryConcerns: MutableList<String>
- knownTriggers: MutableList<String>
- preferredTechniques: MutableList<String>
- communicationStyle: String = "informal"
- sessionHistory: MutableList<PsySessionSummary>

PsySessionSummary.kt:
- sessionId: String
- date: Long
- mainTopics: List<String>
- techniquesUsed: List<String>
- keyInsights: List<String>
- homework: String? = null

PsyAgentContext.kt:
- sessionId: String
- userId: String
- currentState: String
- conversationHistory: List<ConversationEntry>
- userProfile: PsyUserProfile?
- recentSessions: List<PsySessionSummary>
- domainKnowledge: DomainKnowledge

DomainKnowledge.kt:
- techniques: List<Technique>
- Technique: name, description, stepsCount, suitableFor: List<String>
- Prepopulate with: Progressive Muscle Relaxation (3 steps, for: anxiety/stress), Box Breathing (4 steps, for: anxiety/panic), Cognitive Restructuring (5 steps, for: depression/negative-thinking)

### Memory store: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/memory/

ContextStore.kt — interface:
- createSession(userId: String): PsySessionContext
- loadSession(sessionId: String): PsySessionContext?
- saveSession(session: PsySessionContext)
- appendMessage(sessionId: String, message: ConversationEntry)
- loadUserProfile(userId: String): PsyUserProfile?
- saveUserProfile(profile: PsyUserProfile)
- updateUserProfile(userId: String, update: (PsyUserProfile) -> PsyUserProfile)
- assembleContext(sessionId: String, currentState: String): PsyAgentContext

InMemoryContextStore.kt — implementation using ConcurrentHashMap:
- sessions by sessionId
- profiles by userId
- assembleContext loads session + profile + last 3 summaries from profile + domain knowledge

### Context window: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/memory/

ContextWindowManager.kt:
- fun buildPrompt(context: PsyAgentContext, maxTokens: Int = 4000): String
- Estimate tokens as charCount / 4
- Priority: user profile summary > session bridge (last 3 sessions) > recent messages (newest first)
- If truncated, prepend "[Earlier conversation summarized]"

### Agent: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/

PsyAgent.kt:
- Follow the pattern of existing agents (study Day7Agent or Day9Agent)
- Constructor: contextStore, contextWindowManager, httpClient (for DeepSeek API)
- fun startSession(userId: String): PsySessionContext
- suspend fun chat(sessionId: String, userMessage: String): String
- chat() does: load context -> build prompt with ContextWindowManager -> call DeepSeek API -> save message -> return response
- Use the same DeepSeek API call pattern as existing agents

### Route: server/src/main/kotlin/com/portfolio/ai_challenge/routes/

Add to AgentRoutes.kt (or create PsyAgentRoutes.kt following existing pattern):
- POST /api/agent/psy/start — body: { "userId": "..." } -> returns { "sessionId": "..." }
- POST /api/agent/psy/chat — body: { "sessionId": "...", "message": "..." } -> returns { "response": "...", "state": "...", "memoryLayers": { "turn": {...}, "session": {...}, "profile": {...} } }

The memoryLayers field is for debugging — shows what data lives in each layer.

Register routes in Application.kt following existing pattern.

## Database side (skip for Day 11)

Day 11 uses InMemoryContextStore. We will add Room persistence later if needed.

## UI side

### API client: composeApp/src/commonMain/kotlin/com/portfolio/ai_challenge/data/

PsyAgentApi.kt (follow AgentApi.kt pattern):
- suspend fun startSession(userId: String): StartSessionResponse
- suspend fun chat(sessionId: String, message: String): PsyChatResponse

Data classes for responses:
- StartSessionResponse: sessionId: String
- PsyChatResponse: response: String, state: String, memoryLayers: MemoryLayersDebug
- MemoryLayersDebug: turn: String, session: String, profile: String

### DI

Add PsyAgentApi to NetworkModule.kt following existing pattern.

### Screen: composeApp/src/commonMain/kotlin/com/portfolio/ai_challenge/ui/screen/

Day11Screen.kt — chat screen with memory debug panel:
- Standard chat interface (follow existing Day7Screen or Day9Screen pattern)
- Below the chat: expandable "Memory Debug" section showing 3 tabs: Turn / Session / Profile
- Each tab shows the raw JSON of that memory layer from the API response
- "Start Session" button that calls /psy/start before first message

Day11ViewModel.kt:
- Follow existing ViewModel pattern (study Day7ViewModel or Day9ViewModel)
- sessionId state
- messages list state
- memoryDebug state (turn/session/profile strings)
- startSession() and sendMessage() functions

### Navigation

Add Day11 to:
- composeApp/.../navigation/AppScreen.kt — add Day11 screen definition
- composeApp/.../model/ChallengeDay.kt — add day 11 entry
- composeApp/.../ui/screen/MainScreen.kt — add card for day 11
- composeApp/.../App.kt — add navigation route

Follow EXACTLY the pattern of how Day10 screens are registered.

## Tests

### Server tests: server/src/test/kotlin/com/portfolio/ai_challenge/

Day11MemoryTest.kt:
- testTurnContextNotPersisted: TurnContext is a local variable, not saved to store
- testSessionLifecycle: createSession -> appendMessage x3 -> loadSession -> verify messages
- testUserProfilePersistence: saveUserProfile -> loadUserProfile -> verify fields
- testAssembleContext: session + profile + call assembleContext -> verify all layers present
- testContextWindowTruncation: 20 messages, maxTokens=500, verify output fits
- testMultipleSessions: 2 sessions same user, independent but share UserProfile

Day11IntegrationTest.kt (follow Day10IntegrationTest pattern):
- Start Ktor test server
- POST /psy/start -> get sessionId
- POST /psy/chat with message -> get response with memoryLayers
- Verify memoryLayers.session contains the message
- Verify memoryLayers.profile exists

## Constraints
- Follow ALL existing naming conventions, DI patterns, route patterns
- Use kotlinx.serialization with @Serializable
- Use existing DeepSeek API integration from models/DeepSeekModels.kt
- Use existing HttpClient setup
- Prefix psy-agent specific models with "Psy" to avoid conflicts with existing models