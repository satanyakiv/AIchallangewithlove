# Day 9: Context Management with History Compression

## Context

**What problem does this solve?**
When an AI chat grows long, every message is sent to the LLM on every request. This wastes tokens and eventually hits the model's context window limit. Day 9 implements "history compression": older messages are replaced with a short AI-generated summary, so only the summary + recent messages are sent, saving tokens while preserving context quality.

**User will see:**
- A chat screen with a compression toggle (ON/OFF)
- Live token usage stats showing how many tokens were used
- A visible "summary" of what was compressed
- Ability to compare answer quality with/without compression

---

## Mental Model (Explain Like You're New)

Imagine a conversation with 20 messages. Normally you send all 20 to DeepSeek on every request.

**With compression:**
1. You decide: keep only the last 5 messages "as-is"
2. Take the first 15 messages → ask DeepSeek: "Please summarize this conversation in 3 sentences"
3. DeepSeek gives you a short summary text
4. Now on the next request you send: `[summary] + [last 5 messages]` instead of all 20
5. DeepSeek still "knows" the context, but you used far fewer tokens

**Where each piece lives:**
- Summary text → stored in the database (role="context_summary")
- Recent N messages → stored normally (role="user"/"assistant")
- Compression logic → runs on the **server** (Day9Agent)
- Token stats → returned by server with each response

---

## Architecture

### Data Flow

```
User types message
       ↓
Day9ViewModel saves message to DB
       ↓
Load all messages from DB
       ↓
compressionEnabled ON + count > THRESHOLD?
  ├── NO  → send all messages to /api/agent/chat-v9 (compression=false)
  └── YES → split: oldMessages + recentMessages + existingSummary
              → send to /api/agent/chat-v9 (compression=true)
                          ↓
                    Day9Agent (server):
                      1. Summarize oldMessages → newSummary
                      2. Build DeepSeek request: [system] + [newSummary as context] + [recentMessages]
                      3. Call DeepSeek → get response + token usage
                      4. Return: { response, newSummary, tokenUsage }
                          ↓
Day9ViewModel saves:
  - assistant response to DB
  - newSummary to DB (role="context_summary")
  - tokenUsage to UI state
```

### Constants (configurable)
- `WINDOW_SIZE = 10` — total messages before compression triggers
- `RECENT_KEEP = 5` — how many recent messages to keep "as-is"

---

## Files to Create

### Server
1. **`server/src/main/kotlin/.../agent/Day9Agent.kt`**
   - `chat(request: Day9AgentRequest): Day9AgentResponse`
   - Two-step: summarize old → respond with compressed context
   - Returns `newSummary` + `tokenUsage`

### Client
2. **`composeApp/src/commonMain/kotlin/.../ui/screen/Day9Screen.kt`**
   - Chat UI + toggle switch for compression
   - Stats card: total messages, compression status, token usage

3. **`composeApp/src/commonMain/kotlin/.../ui/screen/Day9ViewModel.kt`**
   - `compressionEnabled: StateFlow<Boolean>` — toggle
   - `tokenStats: StateFlow<TokenStats?>` — last request stats
   - `currentSummary: StateFlow<String?>` — visible summary text
   - Uses existing `ChatRepository` + new summary repo

---

## Files to Modify

4. **`server/src/main/kotlin/.../routes/AgentRoutes.kt`**
   - Add `POST /api/agent/chat-v9` route

5. **`composeApp/src/commonMain/kotlin/.../data/AgentApi.kt`**
   - Add `chatV9(request: AgentChatV9Request): AgentChatV9Response`
   - New DTOs: `AgentChatV9Request`, `AgentChatV9Response`, `TokenUsageDto`

6. **`composeApp/src/commonMain/kotlin/.../App.kt`**
   - Add `Day9` to sealed class Screen + when-expression routing

7. **`composeApp/src/commonMain/kotlin/.../ui/screen/MainScreen.kt`**
   - Add Day 9 card to `challengeDays` list

8. **`composeApp/src/commonMain/kotlin/.../di/AppModule.kt`**
   - Register `Day9ViewModel`

9. **`database/src/commonMain/kotlin/.../ChatMessageDao.kt`**
   - Add `@Query("SELECT * FROM chat_messages WHERE role = 'context_summary' ORDER BY id DESC LIMIT 1")` → getLatestSummary
   - Add `@Query("DELETE FROM chat_messages WHERE role = 'context_summary'")` → clearSummaries

10. **`database/src/commonMain/kotlin/.../ChatRepository.kt`**
    - Add `getLatestSummary(): ChatMessageEntity?`
    - Add `saveSummary(content: String)`
    - Add `clearSummaries()`

    > **Note:** No DB migration needed — no schema changes, only new queries on existing table.

---

## New DTOs (Serializable)

```kotlin
// AgentApi.kt additions
@Serializable
data class AgentChatV9Request(
    val recentMessages: List<ApiMessage>,
    val oldMessages: List<ApiMessage>,        // empty if compression=false
    val existingSummary: String?,             // null if no summary yet
    val compressionEnabled: Boolean,
)

@Serializable
data class AgentChatV9Response(
    val response: String,
    val newSummary: String?,                  // null if compression=false
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
```

---

## Day9Agent Logic (Pseudocode)

```kotlin
// compression=false path
fun chatNormal(allMessages): Day9AgentResponse {
    val request = buildDeepSeekRequest(systemPrompt, allMessages)
    val deepSeekResponse = callDeepSeek(request)
    return Day9AgentResponse(
        response = deepSeekResponse.text,
        newSummary = null,
        tokenUsage = deepSeekResponse.usage
    )
}

// compression=true path (2 DeepSeek calls)
fun chatCompressed(recentMessages, oldMessages, existingSummary): Day9AgentResponse {
    // Call 1: Summarize old context
    val summaryPrompt = buildSummaryPrompt(existingSummary, oldMessages)
    val newSummary = callDeepSeek(summaryPrompt).text

    // Call 2: Answer with compressed context
    val contextMessage = "Previous conversation summary: $newSummary"
    val request = buildDeepSeekRequest(
        systemPrompt,
        contextMessage,
        recentMessages        // only last N messages
    )
    val deepSeekResponse = callDeepSeek(request)
    return Day9AgentResponse(
        response = deepSeekResponse.text,
        newSummary = newSummary,
        tokenUsage = deepSeekResponse.usage  // tokens from call 2 only
    )
}
```

---

## UI Layout (Day9Screen)

```
┌─────────────────────────────────────────┐
│  Day 9: Context Compression         ←   │
├─────────────────────────────────────────┤
│  ┌─────────────────────────────────┐    │
│  │ Compression   [ Toggle ON/OFF ] │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ Context Summary (if any)        │    │
│  │ "User discussed X, Y, Z..."     │    │
│  └─────────────────────────────────┘    │
│                                         │
│  [Chat messages scrollable area]        │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ Token Stats                     │    │
│  │ Prompt: 123 | Response: 45      │    │
│  │ Total: 168 tokens               │    │
│  └─────────────────────────────────┘    │
│                                         │
│  [Input field]              [Send]      │
└─────────────────────────────────────────┘
```

---

## Key Files Reference

| File | Path |
|------|------|
| Day7Agent (reference) | `server/src/main/kotlin/.../agent/Day7Agent.kt` |
| AgentRoutes | `server/src/main/kotlin/.../routes/AgentRoutes.kt` |
| AgentApi | `composeApp/src/commonMain/kotlin/.../data/AgentApi.kt` |
| App.kt (navigation) | `composeApp/src/commonMain/kotlin/.../App.kt` |
| MainScreen | `composeApp/src/commonMain/kotlin/.../ui/screen/MainScreen.kt` |
| AppModule | `composeApp/src/commonMain/kotlin/.../di/AppModule.kt` |
| ChatRepository | `database/src/commonMain/kotlin/.../ChatRepository.kt` |
| ChatMessageDao | `database/src/commonMain/kotlin/.../ChatMessageDao.kt` |
| Day7ViewModel (reference) | `composeApp/src/commonMain/kotlin/.../ui/screen/Day7ViewModel.kt` |

---

## Verification

1. Run server: `./gradlew :server:run`
2. Run app: `./gradlew :composeApp:run`
3. Navigate to Day 9 screen
4. **Test without compression:** Send 5+ messages, check token stats
5. **Test with compression:**
   - Send 12+ messages (crosses WINDOW_SIZE=10 threshold)
   - Toggle compression ON
   - Send another message
   - Verify: summary card appears, token count decreases vs without compression
6. Verify summary is persisted: restart app, check summary still shows
7. Verify answers are coherent even with compression (references earlier context)
