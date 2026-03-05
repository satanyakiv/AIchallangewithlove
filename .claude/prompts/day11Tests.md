Read .claude/rules/testing.md
Read server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/
Read server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/memory/
Read server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/model/

## Context

Day 11 psy-agent has a persistence bug: user profile data (preferred name,
concerns, triggers) is not being saved when extracted from conversation.
We need comprehensive tests for the entire persistence layer, then fix
everything that fails.

## Step 1: Write ALL tests first (do NOT fix anything yet)

Create server/src/test/kotlin/com/portfolio/ai_challenge/Day11PersistenceTest.kt

Use InMemoryContextStore directly for unit tests. No HTTP, no Ktor test server.
For tests that need chat() responses, mock LlmClient to return a fixed string
so tests don't call DeepSeek API.
```kotlin
class Day11PersistenceTest {
    private val contextStore = InMemoryContextStore()
    // mock LlmClient, PsyPromptBuilder as needed
    // create PsyAgent with real contextStore + mocked llm
}
```

### Session lifecycle tests

- testCreateSession_returnsValidSessionId:
  startSession("user1"), assert sessionId is not blank and not null

- testAppendMessage_messagesInCorrectOrder:
  Create session, append 3 ConversationEntry messages manually via contextStore,
  loadSession, assert messages size == 3 and in chronological order with correct roles

- testInvalidSessionId_throwsException:
  Call agent.chat("fake-session-id", "hello"),
  assert throws IllegalArgumentException with message containing "Session not found"

- testTwoSessions_sameUser_isolatedMessages:
  Create session1 and session2 for same "user1".
  Append "hello" to session1, append "goodbye" to session2.
  Load session1: messages has only "hello".
  Load session2: messages has only "goodbye".

### Profile persistence tests

- testPreferredName_userSaysName_profileUpdated:
  Create session, call agent.chat(sessionId, "My name is Katya"),
  load profile for that userId, assert preferredName == "Katya"

- testPreferredName_callMe_profileUpdated:
  agent.chat(sessionId, "Please call me Dmytro"),
  load profile, assert preferredName == "Dmytro"

- testPreferredName_persistsAcrossSessions:
  Session 1: agent.chat("Call me Katya").
  Create session 2 for SAME userId.
  Load profile, assert preferredName still "Katya"

- testConcerns_anxietyKeyword_extracted:
  agent.chat(sessionId, "I have been feeling really anxious lately"),
  load profile, assert primaryConcerns contains "anxiety"

- testConcerns_sleepKeyword_extracted:
  agent.chat(sessionId, "I cant sleep at night"),
  load profile, assert primaryConcerns contains "sleep issues"

- testConcerns_accumulateAcrossMessages:
  agent.chat(sessionId, "I feel anxious"),
  agent.chat(sessionId, "I also cant sleep"),
  load profile, assert concerns contains BOTH "anxiety" AND "sleep issues"

- testConcerns_noDuplicates:
  agent.chat(sessionId, "I feel anxious"),
  agent.chat(sessionId, "My anxiety is getting worse"),
  load profile, assert "anxiety" appears exactly once in concerns

- testTriggers_workKeyword_extracted:
  agent.chat(sessionId, "Work deadlines are killing me"),
  load profile, assert knownTriggers contains "work stress"

- testTriggers_familyKeyword_extracted:
  agent.chat(sessionId, "Having issues with my partner"),
  load profile, assert knownTriggers contains "family dynamics"

- testTriggers_persistAcrossSessions:
  Session 1: agent.chat("Work deadlines stress me out").
  Create session 2 for SAME userId.
  Load profile, assert triggers still contains "work stress"

- testProfileNotOverwritten_onNewSession:
  Session 1: send name + anxiety messages. Verify profile has both.
  Create session 2 for same userId.
  Load profile, assert name AND concerns still present from session 1.

- testTwoUsers_separateProfiles:
  User "user-a": chat "My name is Katya".
  User "user-b": chat "My name is Dmytro".
  Load profile user-a: preferredName "Katya".
  Load profile user-b: preferredName "Dmytro". No cross-contamination.

### Context assembly tests

- testAssembleContext_returnsCurrentSessionMessages:
  Create session, append 3 messages, call assembleContext,
  assert conversationHistory has exactly those 3 messages

- testAssembleContext_includesUserProfile:
  Save profile with concerns ["anxiety"], call assembleContext,
  assert returned context.userProfile has concerns ["anxiety"]

- testAssembleContext_emptyProfile_doesNotCrash:
  New userId with no profile saved, call assembleContext,
  assert does not throw, returns context with empty/default profile

- testAssembleContext_includesRecentSessionHistory:
  Manually add 2 SessionSummary entries to profile.sessionHistory,
  save profile, call assembleContext,
  assert context.recentSessions has those 2 entries

- testAssembleContext_differentSessions_differentMessages:
  Session1 has messages A,B. Session2 has messages C,D.
  assembleContext(session1) returns A,B not C,D.
  assembleContext(session2) returns C,D not A,B.

### Memory debug API tests

- testMemoryDebug_sessionShowsMessageCount:
  Send 2 user messages via agent.chat(), check PsyChatResult,
  assert memoryLayers or session data reflects correct message count
  (should be 4: 2 user + 2 assistant)

- testMemoryDebug_profileShowsUpdatedName:
  agent.chat(sessionId, "Call me Katya"), check PsyChatResult,
  assert result contains profile data with "Katya"

## Step 2: Run tests, observe failures

Run: ./gradlew :server:test --tests "*.Day11PersistenceTest"

Print summary: which tests pass, which fail, and WHY each fails.
Do NOT fix anything yet. Just report.

## Step 3: Fix all failures

Based on test results, fix the persistence logic. Expected fixes:

a) Profile extraction: PsyAgent.chat() needs to extract profile updates
from user messages and call contextStore.updateUserProfile().

Extraction rules (keyword-based, case-insensitive, user messages only):
- "my name is X" / "call me X" / "i am X" -> preferredName = X
  (X = next word after keyword, capitalized)
- "anxious", "anxiety", "worried", "nervous" -> concern: "anxiety"
- "sleep", "insomnia", "cant sleep", "nightmares" -> concern: "sleep issues"
- "sad", "depressed", "hopeless", "empty" -> concern: "depression"
- "work", "deadline", "boss", "job" -> trigger: "work stress"
- "family", "parents", "partner", "relationship" -> trigger: "family dynamics"
- "lonely", "alone", "isolated" -> trigger: "loneliness"

b) Create a simple ProfileExtractor class if it doesn't exist:
server/.../agent/day_11_psy_agent/ProfileExtractor.kt
- fun extract(message: String): ProfileUpdate
- ProfileUpdate: preferredName: String?, newConcerns: List<String>, newTriggers: List<String>

c) In PsyAgent.chat(), after saving user message and before LLM call:
- Extract profile updates from userMessage
- If any updates found, call contextStore.updateUserProfile()
- Merge: add new concerns/triggers without duplicates, update name if found

d) Make sure ContextStore.updateUserProfile() actually persists changes.
If it doesn't exist, add it to the interface and InMemoryContextStore.

e) Make sure createSession does NOT overwrite existing profile for that userId.

## Step 4: Run ALL tests

./gradlew :server:test

ALL tests must pass — both new Day11PersistenceTest and all existing tests.
Print full results.

## Constraints
- Mock LlmClient in tests — never call real DeepSeek API from tests
- Do NOT change any API endpoints or response format
- Do NOT change existing test files
- Keep ProfileExtractor simple — regex/keyword only, no LLM calls
- ProfileExtractor is a stepping stone for Day 12 PersonalizedPromptBuilder