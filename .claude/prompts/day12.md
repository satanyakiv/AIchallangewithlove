Read the architecture diagrams in documentation/day-11-to-15/.
Read documentation/project-structure.md.
Read ALL code created in Day 11 under server/.../agent/psy/ to understand the memory model.

## Task

Add personalization on top of the memory model. The agent should adapt its behavior based on user profile.

## Server side

### New models: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/model/

CommunicationPreferences.kt:
- language: String = "uk"
- formality: Formality (enum: FORMAL, INFORMAL, MIXED)
- responseLength: ResponseLength (enum: SHORT, MEDIUM, DETAILED)
- avoidTopics: List<String> = emptyList()
  Make it @Serializable.

ProfileUpdate.kt:
- newConcerns: List<String>? = null
- newTriggers: List<String>? = null
- preferredNameUpdate: String? = null
- styleUpdate: String? = null

### Update PsyUserProfile

Add field: preferences: CommunicationPreferences = CommunicationPreferences()
Do NOT create a new file — modify the existing PsyUserProfile.kt.

### New classes: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/personalization/

ProfileBuilder.kt:
- fun extractUpdates(messages: List<ConversationEntry>, currentProfile: PsyUserProfile?): ProfileUpdate
- fun applyUpdate(profile: PsyUserProfile, update: ProfileUpdate): PsyUserProfile

Extraction rules (keyword-based, case-insensitive, scan only role="user" messages):
- "anxious", "anxiety", "worried", "nervous" -> concern: "anxiety"
- "sleep", "insomnia", "cant sleep", "nightmares" -> concern: "sleep issues"
- "sad", "depressed", "hopeless", "empty" -> concern: "depression"
- "work", "deadline", "boss", "job" -> trigger: "work stress"
- "family", "parents", "partner", "relationship" -> trigger: "family dynamics"
- "lonely", "alone", "isolated" -> trigger: "loneliness"

applyUpdate: merge new concerns/triggers (no duplicates), update name if provided.

PersonalizedPromptBuilder.kt:
- fun buildSystemPrompt(context: PsyAgentContext): String
- fun buildUserSummary(profile: PsyUserProfile): String
- fun buildSessionBridge(recentSessions: List<PsySessionSummary>): String

buildSystemPrompt assembles:
1. Base: "You are MindGuard, a supportive mental health assistant."
2. Style: if FORMAL -> "Use formal, respectful language." if INFORMAL -> "Use warm, casual tone."
3. Length: SHORT -> "Keep responses to 2-3 sentences." MEDIUM -> "Use 1-2 short paragraphs." DETAILED -> "Provide thorough, detailed responses."
4. User summary from buildUserSummary
5. Session bridge from buildSessionBridge
6. Avoid: "Never discuss these topics: {avoidTopics}"
7. Language: "Respond in {language}."

buildUserSummary: "Client: {preferredName}. Known concerns: {concerns}. Known triggers: {triggers}. Preferred techniques: {techniques}. Sessions completed: {count}."

buildSessionBridge: for each recent session: "Previous session ({date}): discussed {topics}, used {techniques}. Homework: {homework}."

### Update PsyAgent.kt

Add ProfileBuilder and PersonalizedPromptBuilder as dependencies.
Update chat() flow:
1. Load context
2. Extract profile updates from new message using ProfileBuilder
3. Apply updates to user profile, save
4. Build personalized prompt using PersonalizedPromptBuilder
5. Call DeepSeek API
6. Save message
7. Return response

### Update API response

Add to PsyChatResponse: profileUpdates: List<String> (what was extracted and updated this turn).

## UI side

### Update Day11Screen -> rename to PsyAgentScreen

Or create Day12Screen.kt that reuses the same chat but adds:
- "Profile" button in top bar that opens a bottom sheet showing:
    - Preferred name (editable)
    - Communication style selector (Formal / Informal / Mixed)
    - Response length selector (Short / Medium / Detailed)
    - Concerns list (read-only, auto-populated)
    - Triggers list (read-only, auto-populated)
    - Avoid topics (editable)
- When profile is edited, send update to a new endpoint: POST /api/agent/psy/profile
- Show profileUpdates from chat response as a small chip below each assistant message: "Detected: anxiety, work stress"

Follow existing component patterns from ChatComponents.kt and Day10SharedComponents.kt.

### Navigation

Add Day12 to AppScreen.kt, ChallengeDay.kt, MainScreen.kt, App.kt — same pattern as Day11.

## Tests

### Server: server/src/test/kotlin/com/portfolio/ai_challenge/

Day12PersonalizationTest.kt:
- testExtractAnxiety: message "I feel so anxious lately" -> concern "anxiety"
- testExtractMultiple: message "Work deadlines make me anxious and I cant sleep" -> concern "anxiety" + "sleep issues", trigger "work stress"
- testNoExtractionFromAssistant: assistant message with keywords -> no extraction
- testMergeNoDuplicates: profile has "anxiety", update has "anxiety" + "sleep" -> result has both, no duplicate anxiety
- testFormalPrompt: formal profile -> prompt contains "formal, respectful"
- testInformalPrompt: informal profile -> prompt contains "warm, casual"
- testAvoidTopics: profile avoids "religion" -> prompt contains "Never discuss: religion"
- testSessionBridge: profile with 2 past sessions -> bridge mentions both
- testTwoProfilesComparison: build prompts for "Olena" (formal, anxiety) and "Dmytro" (informal, grief), print both side by side

## Constraints
- Import ALL models from agent/psy/model/ — do NOT create duplicate classes
- Modify existing files when adding fields (PsyUserProfile, PsyAgent)
- Follow existing Koin DI patterns for new classes
- @Serializable on all new data classes