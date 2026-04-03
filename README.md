# AI Challenge

35-day AI agent course in Kotlin Multiplatform. 4 modules, 15 days implemented, 2 standalone agents. Android + Desktop clients talk to a Ktor backend that calls DeepSeek LLM.

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.10.3-4285F4?logo=jetpackcompose&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-3.4.2-087CFA)
![Platform](https://img.shields.io/badge/Platform-Android_%7C_Desktop_JVM-green)

## What this is and why

| Question | Answer |
| -------- | ------ |
| What gets built? | AI agents. From a raw API call on Day 4 to a system with dual state machines, 5 safety invariants, and a retry pipeline on Day 15 |
| Why not just a tutorial? | Most tutorials stop at "call the API, print the response." This course builds each day on top of the previous one. Complexity grows incrementally, not in one dump |
| What runs where? | Ktor server on JVM handles agents. Compose Multiplatform app (Android + Desktop) is the client. Not a CLI demo |
| What LLM? | DeepSeek API (`deepseek-chat`, `deepseek-reasoner`) |

## Day-by-day progress

<details>
<summary><b>Week 1: Foundations</b></summary>

| Day | Topic | What it does | Status |
| --- | ----- | ------------ | ------ |
| 1-3 | Setup | KMP project scaffolding, DeepSeek API integration, shared constants | ✅ |
| 4 | Temperature | Streams LLM responses at temperature 0.0, 0.7, 1.2 side by side | ✅ |
| 5 | Model comparison | Compares deepseek-chat vs deepseek-reasoner: latency, token count, cost | ✅ |

</details>

<details>
<summary><b>Week 2: First agent and context management</b></summary>

| Day | Topic | What it does | Status |
| --- | ----- | ------------ | ------ |
| 6 | First agent | Single-request agent with system prompt using Koog framework | ✅ |
| 7 | Chat memory | Saves conversation history to Room SQLite, loads it next session | ✅ |
| 8 | Token counting | Tracks prompt/completion tokens per turn, runs cost experiments | ✅ |
| 9 | Context compression | Replaces old messages with AI-generated summary. Cuts token usage ~50% | ✅ |
| 10 | Context strategies | Same scenario, 4 approaches: sliding window, sticky facts, branching dialogs, summary | ✅ |

</details>

<details>
<summary><b>Week 3: Memory, states, invariants (PsyAgent)</b></summary>

| Day | Topic | What it does | Status |
| --- | ----- | ------------ | ------ |
| 11 | Memory model | Three-layer memory: turn context, session history, user profile. Context window manager with 4000-token budget | ✅ |
| 12 | Personalization | Communication preferences injected into prompts: formality, response length | ✅ |
| 13 | State machine | Typed FSM with guarded transitions: Greeting → ActiveListening → Intervention → Closing → Finished | ✅ |
| 14 | Safety invariants | 5 validators check every LLM response before it reaches the user. Hard block → retry up to 3 times → safe fallback | ✅ |
| 15 | Task lifecycle | Second FSM layered on top: Assessment → Analysis → Planning → Implementation → Closure. Phase enforcement blocks skipping steps | ✅ |

</details>

<details>
<summary><b>FreudAgent (standalone)</b></summary>

Psychoanalytic chatbot. Separate from PsyAgent, different personality, own FSM.

| Aspect | Details |
| ------ | ------- |
| States | Begrüssung → FreeAssociation → Interpretation → DreamAnalysis → Transference → Abschluss |
| Profile extraction | Dream symbols, fixations, psychological markers |
| Context | Own `FreudContextStore`, separate from PsyAgent memory |

</details>

<details>
<summary><b>Week 4-7 (planned)</b></summary>

| Days | Topic | Status |
| ---- | ----- | ------ |
| 16-20 | Model Context Protocol (MCP) | 🔲 |
| 21-25 | Retrieval-Augmented Generation (RAG) | 🔲 |
| 26-30 | Local LLMs, on-device inference | 🔲 |
| 31-35 | Production AI assistant patterns | 🔲 |

</details>

## Architecture

Server code has 4 layers. Every feature follows this split in separate files:

```
Routes (HTTP parsing)
  → Agent (orchestration)
    → Components (PromptBuilder, ResponseMapper, Validator, UseCase)
      → Store (data access behind interfaces)
```

| Pattern | How it works |
| ------- | ------------ |
| Sealed types over strings | If an enum exists for a concept, code passes the type. Never `.name` |
| `AtoBMapper` | Dedicated mapper class for every type conversion. Own file, one `map()` method |
| `UseCase` | Extracted when Agent logic grows beyond "call A, pass to B." Single `execute()` method |
| Prompts as config | All prompt text in `.txt` resource files. `PromptBuilder` composes them with runtime data |
| Size limits | Files < 150 lines, functions < 20, Agent class < 80 |

## Tech Stack

| Category | Technology | Version |
| -------- | ---------- | ------- |
| Language | Kotlin | 2.3.20 |
| UI | Compose Multiplatform | 1.10.3 |
| Server | Ktor + Netty | 3.4.2 |
| Database | Room KMP + SQLite Bundled | 2.8.4 |
| AI/LLM | DeepSeek API, Koog agents | 0.7.3 |
| DI | Koin | 4.2.0 |
| Serialization | kotlinx-serialization | 1.10.0 |
| Config | Hoplite (YAML + env vars) | 2.9.0 |
| Testing | MockK, Kotest, AssertK | 1.14.9 / 6.1.4 / 0.28.1 |
| Quality | Detekt, Dokka | 1.23.8 / 2.2.0 |

<details>
<summary><b>Day 14 invariants (what gets checked)</b></summary>

| Invariant | Severity | What it catches |
| --------- | -------- | --------------- |
| NoDiagnosis | HARD_BLOCK | Agent can't diagnose mental conditions |
| NoMedication | HARD_BLOCK | No medication recommendations |
| NoProfanity | HARD_BLOCK | Clean language only |
| NoPromptLeak | HARD_BLOCK | System prompt stays hidden from user |
| ResponseLength | SOFT_FIX | Max 300 tokens per response |

HARD_BLOCK triggers retry (up to 3 attempts). All retries fail → safe fallback response.

</details>

## How to run

Requires `DEEPSEEK_API_KEY` environment variable (export in `~/.zshrc` or equivalent).

| What | Command |
| ---- | ------- |
| Server (port 8080) | `./gradlew :server:run` |
| Desktop with Hot Reload | `./gradlew :composeApp:hotRunJvm --mainClass=com.portfolio.ai_challenge.MainKt` |
| Android APK | `./gradlew :composeApp:assembleDebug` |

## Project structure

| Module | What it does |
| ------ | ------------ |
| `server/` | Ktor REST API: agents, routes, prompt resources, experiment runners |
| `composeApp/` | Compose Multiplatform UI: screens, ViewModels, navigation (Android + Desktop JVM) |
| `database/` | Room + SQLite KMP library for client-side chat persistence |
| `shared/` | Platform expect/actual, shared constants (`SERVER_PORT`, `BASE_URL`) |
