# Plan: Rewrite README.md

## Context

The current README.md is a boilerplate KMP template with generic build instructions. It says nothing about what the project actually is, what was built, or why. For a portfolio project targeting recruiters, this is a missed opportunity. The goal is to replace it with a human-like, fact-driven README that shows the scope and depth of the work.

## Writing Style

Adopt Nutrisport's anti-AI documentation rules:
- **Facts first**: open with concrete numbers, not value judgments
- **Direct voice**: "X does Y", not "X is designed to do Y"
- **Banned words**: ensures, leveraging, showcases, facilitates, encompasses, utilizing → use: keeps, using, shows, helps, covers, using
- **No em dashes in prose** (only in file trees / table cells)
- **No "Moreover/Furthermore/Additionally"**
- **Some sentences start with "And" or "But"**
- **Numbers over adjectives**: "5 invariants" not "robust safety system"
- **Contractions OK**: doesn't, can't, won't
- **Varied sentence length**: mix 3-word with 25-word sentences
- **No rule-of-three groupings** (use 2, 4, or 5 items)
- **English language**

## Target File

`/Users/taxistsamael/AndroidStudioProjects/AI-challenge/README.md`

## README Structure

### 1. Title + One-Line Intro
- Project name + factual one-liner
- Example: "35-day AI agent course built in Kotlin Multiplatform. 4 modules, 15 days implemented, 2 standalone agents."

### 2. Why This Project
- 2-3 short paragraphs explaining the motivation
- What problem it solves (learning AI agent patterns hands-on)
- What makes it different from typical tutorials (progressive complexity, real architecture, not just API wrappers)

### 3. Collapsible Day-by-Day Table
- GitHub `<details><summary>` for collapsibility
- All 35 days in one table
- Columns: Day | Topic | What It Does | Status
- Days 4-15: status = ✅ Done
- Days 16-35: status = 🔲 Planned
- Days 1-3: skip or mark as "Setup / Theory"
- Each "What It Does" cell is 1 concise sentence with the key technical concept

### 4. Architecture Overview
- Short text description of the layered architecture
- Simple ASCII diagram: `Routes → Agent → Components → Store`
- Mention key patterns: sealed types, mappers, use cases, prompt-as-config
- No mermaid (keep it simple for recruiters scanning GitHub)

### 5. Tech Stack
- Compact table: Category | Technology | Version
- Categories: Language, UI, Server, Database, AI/LLM, DI, Testing, Build
- Versions from `gradle/libs.versions.toml`

### 6. How to Run
- 3 commands: server, desktop (hot reload), Android APK
- Mention DEEPSEEK_API_KEY env var requirement
- Keep it minimal

### 7. Project Structure
- Short module table (4 modules: shared, composeApp, database, server)
- One line per module describing its purpose

## Day-by-Day Table Content

| Day | Topic | What It Does | Status |
|-----|-------|--------------|--------|
| 1-3 | Setup | Project scaffolding, KMP configuration, DeepSeek API setup | ✅ |
| 4 | Temperature | Compare LLM outputs at temperature 0.0, 0.7, 1.2 with streaming | ✅ |
| 5 | Model Comparison | Side-by-side model outputs with latency, tokens, cost metrics | ✅ |
| 6 | First Agent | Single-request agent with system prompt via Koog framework | ✅ |
| 7 | Chat Memory | Persistent conversation history with Room SQLite | ✅ |
| 8 | Token Counting | Track token usage and costs per turn, experiment runner | ✅ |
| 9 | Context Compression | AI-generated summaries replace old messages to save tokens | ✅ |
| 10 | Context Strategies | Three approaches: sliding window, sticky facts, branching dialogs | ✅ |
| 11 | Memory Model | Three-layer memory: turn, session, profile. PsyAgent begins | ✅ |
| 12 | Personalization | Communication preferences: formality, length, language style | ✅ |
| 13 | State Machine | Typed FSM: Greeting → ActiveListening → Intervention → Closing | ✅ |
| 14 | Safety Invariants | 5 validators (no diagnosis, no medication, no profanity, etc.) with retry pipeline | ✅ |
| 15 | Task Lifecycle | Dual state machines: session FSM + task phase FSM with enforcement | ✅ |
| 16-20 | MCP | Model Context Protocol integration | 🔲 |
| 21-25 | RAG | Retrieval-Augmented Generation | 🔲 |
| 26-30 | Local LLMs | On-device model inference | 🔲 |
| 31-35 | Applied Agents | Production AI assistant patterns | 🔲 |

Plus: **FreudAgent** (standalone psychoanalytic chatbot with its own FSM and dream analysis states)

## Verification

After writing:
1. Open README.md on GitHub (or preview locally) — check that `<details>` table collapses correctly
2. Check no Nutrisport-banned words slipped in (grep for "ensures", "leveraging", "showcases", etc.)
3. Verify all versions match `gradle/libs.versions.toml`
4. Confirm the tone reads as a human dev, not AI-generated marketing
