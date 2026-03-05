# Day 12 — Personalization

```
Read .claude/rules/architecture.md
Read .claude/rules/testing.md
Read .claude/rules/prompts.md

Read ALL existing psy-agent code:
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/model/PsyUserProfile.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/ProfileExtractor.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/UpdateProfileUseCase.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/PsyPromptBuilder.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/PsyResponseMapper.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/Day11PsyAgent.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/memory/ContextStore.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/memory/InMemoryContextStore.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/Prompts.kt

Read existing tests:
- server/src/test/kotlin/com/portfolio/ai_challenge/Day11PersistenceTest.kt
- server/src/test/kotlin/com/portfolio/ai_challenge/Day11MemoryTest.kt

Read UI patterns:
- composeApp/src/commonMain/kotlin/com/portfolio/ai_challenge/ui/screen/Day11Screen.kt
- composeApp/src/commonMain/kotlin/com/portfolio/ai_challenge/ui/screen/Day11ViewModel.kt

## Task

Add personalization layer on top of the existing memory model.
The agent should adapt its behavior based on user profile preferences.

## Server side

### 1. Add CommunicationPreferences to PsyUserProfile

Modify existing PsyUserProfile.kt — do NOT create a new file:

```kotlin
data class PsyUserProfile(
    val userId: String,
    val preferredName: String? = null,
    val primaryConcerns: List<String> = emptyList(),
    val knownTriggers: List<String> = emptyList(),
    val preferredTechniques: List<String> = emptyList(),
    val sessionHistory: List<PsySessionSummary> = emptyList(),
    val preferences: CommunicationPreferences = CommunicationPreferences(),
)

data class CommunicationPreferences(
    val language: String = "en",
    val formality: Formality = Formality.INFORMAL,
    val responseLength: ResponseLength = ResponseLength.MEDIUM,
    val avoidTopics: List<String> = emptyList(),
)

enum class Formality { FORMAL, INFORMAL, MIXED }
enum class ResponseLength { SHORT, MEDIUM, DETAILED }
```

Keep CommunicationPreferences, Formality, ResponseLength in the same
PsyUserProfile.kt file since they are tightly coupled.

### 2. Create PersonalizeResponseUseCase

Create server/.../agent/day_11_psy_agent/PersonalizeResponseUseCase.kt:

```kotlin
class PersonalizeResponseUseCase {
    fun buildPersonalizedSystemPrompt(context: PsyAgentContext): String
    fun buildUserSummary(profile: PsyUserProfile): String
    fun buildSessionBridge(recentSessions: List<PsySessionSummary>): String
}
```

buildPersonalizedSystemPrompt assembles a full system prompt:
1. Load base system prompt from Prompts.Psy.SYSTEM
2. Add communication style rules:
    - FORMAL: "Use formal, respectful language. Address the client with respect."
    - INFORMAL: "Use warm, casual, supportive tone. Be friendly and approachable."
    - MIXED: "Balance professionalism with warmth."
3. Add response length rules:
    - SHORT: "Keep responses to 2-3 sentences maximum."
    - MEDIUM: "Use 1-2 short paragraphs."
    - DETAILED: "Provide thorough, detailed responses with examples."
4. Add user summary from buildUserSummary
5. Add session bridge from buildSessionBridge
6. Add avoid topics: "Never discuss or bring up these topics: {avoidTopics}"
7. Add language: "Respond in {language}."

buildUserSummary:
If profile has data: "Client: {preferredName}. Known concerns: {concerns}. Known triggers: {triggers}. Preferred techniques: {techniques}. Sessions completed: {sessionHistory.size}."
If profile is empty/new: "New client. No prior information available. Start by getting to know them."

buildSessionBridge:
For each of the last 3 sessions: "Previous session ({date}): discussed {topics}, used {techniques}. Homework: {homework}."
If no sessions: empty string.

### 3. Create UpdatePreferencesUseCase

Create server/.../agent/day_11_psy_agent/UpdatePreferencesUseCase.kt:

```kotlin
class UpdatePreferencesUseCase(private val contextStore: ContextStore) {
    fun execute(userId: String, preferences: CommunicationPreferences)
}
```

Updates only the preferences field in user profile without touching
other fields (name, concerns, triggers).

### 4. Update PsyPromptBuilder

Update PsyPromptBuilder to use PersonalizeResponseUseCase:
- Inject PersonalizeResponseUseCase
- In buildMessages(): use personalizeUseCase.buildPersonalizedSystemPrompt(context)
  instead of loading raw Prompts.Psy.SYSTEM
- The personalized prompt replaces the static system prompt

### 5. Update PsyAgent (Day11PsyAgent.kt)

Rename file to PsyAgent.kt (drop Day11 prefix — this agent evolves across days).
Add PersonalizeResponseUseCase as dependency if PsyPromptBuilder doesn't
already handle it internally.

chat() flow stays the same — personalization happens inside PsyPromptBuilder transparently.

### 6. Add API endpoints

Add to routes/PsyAgentRoutes.kt:

GET /api/agent/psy/profile?userId={userId}
-> Returns current PsyUserProfile as JSON

POST /api/agent/psy/profile/preferences
-> Body: { "userId": "...", "language": "uk", "formality": "FORMAL", "responseLength": "SHORT", "avoidTopics": ["religion", "politics"] }
-> Updates preferences only, returns updated profile

Add to PsyChatResponse (or PsyChatResult):
-> profileUpdates: List<String> — what was auto-extracted this turn
(e.g. ["concern: anxiety", "trigger: work stress", "name: Katya"])

### 7. Update prompts

Create prompt resource files:

server/src/main/resources/prompts/psy/personalization-formal.txt:
"Use formal, respectful language. Address the client with respect. Avoid slang and casual expressions."

server/src/main/resources/prompts/psy/personalization-informal.txt:
"Use warm, casual, supportive tone. Be friendly and approachable. It is okay to use gentle humor."

server/src/main/resources/prompts/psy/personalization-short.txt:
"Keep responses to 2-3 sentences maximum. Be concise and direct."

server/src/main/resources/prompts/psy/personalization-medium.txt:
"Use 1-2 short paragraphs. Balance brevity with depth."

server/src/main/resources/prompts/psy/personalization-detailed.txt:
"Provide thorough, detailed responses with examples and explanations."

Add these to Prompts.Psy object.

PersonalizeResponseUseCase loads them based on profile preferences
instead of hardcoding style instructions.

### 8. Update DI

Register new classes in ServerModule.kt / Application.kt:
- PersonalizeResponseUseCase
- UpdatePreferencesUseCase
  Update PsyPromptBuilder constructor if it now depends on PersonalizeResponseUseCase.

## UI side

### Day12Screen.kt + Day12ViewModel.kt

Create new screen that REUSES Day11 chat logic but adds personalization UI:

Chat area: same as Day11 — messages, quick-reply chips, memory debug.

Add new UI elements:

1. "Profile" IconButton in top bar -> opens BottomSheet with:
    - Preferred name: text field (read-only, auto-populated from chat)
    - Language: dropdown (en, uk, es, de)
    - Style: 3 toggle buttons (Formal / Informal / Mixed)
    - Response length: 3 toggle buttons (Short / Medium / Detailed)
    - Concerns: chip list (read-only, auto-populated)
    - Triggers: chip list (read-only, auto-populated)
    - Avoid topics: editable chip list (add/remove topics)
    - "Save Preferences" button -> calls POST /psy/profile/preferences

2. After each assistant message, show extracted profile updates as
   small info chips: "Detected: anxiety, work stress"

3. Quick-reply chips — add personalization testing phrases:
   Group: Profile style test
    - "Respond formally please"
    - "Be casual with me"
    - "Give me detailed explanations"
    - "Keep it short"

   (These don't auto-switch style — user changes style in Profile sheet.
   These phrases test if the agent FOLLOWS the current style setting.)

### Navigation

Add Day12 to AppScreen.kt, ChallengeDay.kt, MainScreen.kt, App.kt.

### API client

Add to PsyAgentApi.kt (composeApp/.../data/):
- suspend fun getProfile(userId: String): PsyUserProfile
- suspend fun updatePreferences(userId: String, preferences: CommunicationPreferences)

## Tests

### IMPORTANT: Use --tests flag. Never run all tests.

Create server/src/test/kotlin/com/portfolio/ai_challenge/Day12PersonalizationTest.kt

Mock LlmClient in all tests. Never call real DeepSeek API.

### PersonalizeResponseUseCase tests

- testBuildPrompt_formalProfile_containsFormalInstruction:
  Create profile with formality=FORMAL, build prompt,
  assert contains "formal" or "respectful" (from personalization-formal.txt)

- testBuildPrompt_informalProfile_containsInformalInstruction:
  formality=INFORMAL, assert contains "warm" or "casual"

- testBuildPrompt_shortLength_containsShortInstruction:
  responseLength=SHORT, assert contains "2-3 sentences"

- testBuildPrompt_detailedLength_containsDetailedInstruction:
  responseLength=DETAILED, assert contains "thorough" or "detailed"

- testBuildPrompt_avoidTopics_listedInPrompt:
  avoidTopics=["religion", "politics"],
  assert prompt contains "Never discuss" and "religion" and "politics"

- testBuildPrompt_languageUk_promptContainsLanguage:
  language="uk", assert prompt contains "Respond in uk"

- testBuildPrompt_withConcerns_promptContainsConcerns:
  profile with concerns=["anxiety", "sleep issues"],
  assert prompt contains "anxiety" and "sleep issues"

- testBuildPrompt_withTriggers_promptContainsTriggers:
  profile with triggers=["work stress"],
  assert prompt contains "work stress"

- testBuildPrompt_newUser_promptSaysNewClient:
  empty profile, assert prompt contains "New client"

- testBuildPrompt_withPreferredName_promptContainsName:
  preferredName="Katya", assert prompt contains "Katya"

### Session bridge tests

- testSessionBridge_noHistory_returnsEmpty:
  empty sessionHistory, buildSessionBridge returns empty string

- testSessionBridge_oneSession_containsTopics:
  sessionHistory with 1 entry (topics=["anxiety"], techniques=["PMR"]),
  assert bridge contains "anxiety" and "PMR"

- testSessionBridge_threeSessionsMax_onlyLastThree:
  sessionHistory with 5 entries,
  assert bridge references only last 3

### UpdatePreferencesUseCase tests

- testUpdatePreferences_changesFormality:
  Save profile with INFORMAL, update to FORMAL,
  load profile, assert formality == FORMAL

- testUpdatePreferences_doesNotOverwriteName:
  Profile has name="Katya" and INFORMAL,
  update preferences to FORMAL,
  load profile, assert name still "Katya" AND formality == FORMAL

- testUpdatePreferences_doesNotOverwriteConcerns:
  Profile has concerns=["anxiety"],
  update preferences (add avoidTopics),
  load profile, assert concerns still ["anxiety"]

- testUpdatePreferences_avoidTopicsUpdated:
  Update with avoidTopics=["religion"],
  load profile, assert avoidTopics == ["religion"]

- testUpdatePreferences_persistsAcrossSessions:
  Session 1: update preferences to FORMAL.
  Session 2 (same userId): load profile,
  assert formality still FORMAL

### Two profiles comparison test

- testTwoProfiles_differentStyles_differentPrompts:
  Profile A: "Olena", FORMAL, DETAILED, concerns=["anxiety"], triggers=["work stress"]
  Profile B: "Dmytro", INFORMAL, SHORT, concerns=["grief"], no history

  Build prompts for both.

  Assert prompt A contains: "formal", "detailed", "Olena", "anxiety", "work stress"
  Assert prompt B contains: "casual" or "warm", "2-3 sentences", "Dmytro", "grief"
  Assert prompt A does NOT contain "casual"
  Assert prompt B does NOT contain "formal"

  Print both prompts side by side in test output.

### End-to-end agent personalization tests (mock LLM)

- testAgent_formalProfile_promptSentToLlmContainsFormal:
  Set profile to FORMAL, call agent.chat(),
  capture the messages sent to mockLlm,
  assert system message contains "formal"

- testAgent_informalProfile_promptSentToLlmContainsInformal:
  Set profile to INFORMAL, call agent.chat(),
  capture messages sent to mockLlm,
  assert system message contains "warm" or "casual"

- testAgent_avoidTopics_promptSentToLlmContainsAvoidance:
  Set avoidTopics=["religion"], call agent.chat(),
  capture messages, assert system contains "Never discuss" and "religion"

- testAgent_profileUpdatesReturned_inChatResult:
  Call agent.chat(sessionId, "I feel anxious about work"),
  assert result.profileUpdates or equivalent contains "anxiety" and "work stress"

- testAgent_preferenceChangeAffectsNextMessage:
  Send message with INFORMAL -> capture prompt, assert "casual".
  Update preferences to FORMAL.
  Send another message -> capture prompt, assert "formal".
  Shows that preference change takes effect immediately.

### Profile API route tests

- testGetProfile_existingUser_returnsProfile:
  Create session (creates profile), GET /psy/profile?userId=...,
  assert 200 and JSON contains userId

- testGetProfile_unknownUser_returns404orEmpty:
  GET /psy/profile?userId=nonexistent,
  assert appropriate error or empty profile

- testUpdatePreferences_validRequest_returns200:
  POST /psy/profile/preferences with valid body,
  assert 200 and response contains updated preferences

- testUpdatePreferences_thenChat_agentUsesNewStyle:
  POST preferences with FORMAL,
  POST chat message,
  (with mocked LLM) verify prompt contains formal instruction

Run all new tests:
./gradlew :server:test --tests "*.Day12PersonalizationTest"

## Manual testing scenarios (run in UI after build)

### Scenario A — Positive (profile builds up and affects responses)

1. Start session. Open Profile sheet -> verify all defaults (INFORMAL, MEDIUM, en)
2. Send: "Hi, my name is Katya, I have been feeling anxious about work"
   -> Check: info chip shows "Detected: name: Katya, concern: anxiety, trigger: work stress"
   -> Open Profile: name=Katya, concerns=[anxiety], triggers=[work stress]
3. Change style to FORMAL and length to SHORT in Profile sheet, save
4. Send: "Tell me more about breathing techniques"
   -> Response should be formal and short (2-3 sentences)
5. Change style to INFORMAL and length to DETAILED
6. Send same thing: "Tell me more about breathing techniques"
   -> Response should be casual and long. Compare with step 4.

### Scenario B — Negative (edge cases)

1. Start new session. Do NOT set any preferences.
2. Send: "The weather is nice today"
   -> No profile updates extracted. Info chip empty or not shown.
3. Open Profile: concerns and triggers should be empty.
4. Set avoidTopics: ["family"] in Profile sheet, save.
5. Send: "I want to talk about my family problems"
   -> Agent should respect avoidTopics and steer away from family topic.

### Scenario C — Neutral (two users comparison)

1. User A session: set FORMAL + DETAILED. Send: "I feel anxious"
2. User B session (different userId): set INFORMAL + SHORT. Send: "I feel anxious"
3. Compare responses side by side. Same input, different personalization = different output.

## Constraints
- Import ALL models from agent/day_11_psy_agent/model/ — do NOT duplicate
- Modify PsyUserProfile.kt to add CommunicationPreferences — do NOT create separate file for profile
- Follow UseCase pattern from architecture.md for new logic
- All prompt text in resources/prompts/psy/ — not inline in .kt files
- Register new classes in DI (ServerModule.kt or Application.kt)
- Do NOT break any existing Day11 tests
```