# Prompt Management Rules

> Read before creating or editing any LLM prompts.

## Core Principle

Prompts are configuration, not code. All prompt text lives in resource files, never inline in .kt.

## Rules

1. All prompt text in `server/src/main/resources/prompts/{feature}/`
2. One `.txt` file per prompt. File name = purpose.
3. One loader `object` per feature loads .txt files as properties.
4. `Prompts` object loads raw text. `PromptBuilder` composes final prompt from pieces + runtime data.
5. Variables use `{{placeholder}}` syntax. PromptBuilder replaces them.
6. Never hardcode prompt text in Agent, PromptBuilder, Routes, Invariants, or any .kt file.
7. All prompt loaders MUST live in a single `Prompts` object (agent/Prompts.kt) with nested objects per feature. Never create separate prompt loader files per agent.

## Loader Pattern

```kotlin
// agent/psy/prompts/PsyPrompts.kt
object PsyPrompts {
    val SYSTEM by lazy { load("prompts/psy/system.txt") }
    val STATE_GREETING by lazy { load("prompts/psy/state-greeting.txt") }
    val STATE_ACTIVE_LISTENING by lazy { load("prompts/psy/state-active-listening.txt") }

    private fun load(path: String): String =
        this::class.java.classLoader.getResource(path)!!.readText().trim()
}
```

## Template Pattern

```
// resources/prompts/psy/state-active-listening.txt
Reflect and paraphrase what the client shares.
Ask open-ended questions.
Turns so far: {{turnCount}}.
Detected emotions: {{emotions}}.
```

```kotlin
// PsyPromptBuilder.kt
fun buildStatePrompt(state: SessionState): String = when (state) {
    is ActiveListening -> PsyPrompts.STATE_ACTIVE_LISTENING
        .replace("{{turnCount}}", state.turnCount.toString())
        .replace("{{emotions}}", state.detectedEmotions.joinToString())
    // ...
}
```

## Anti-Patterns

```kotlin
// BAD — prompt text in Kotlin class
private const val SYSTEM_PROMPT = """You are MindGuard..."""

// BAD — prompt text in PromptBuilder
fun build() = "You are MindGuard, a supportive..."

// BAD — multiple prompts in one .txt file
// resources/prompts/all-prompts.txt

// GOOD — PsyPrompts.SYSTEM loads from resources/prompts/psy/system.txt
// GOOD — PsyPromptBuilder uses PsyPrompts.SYSTEM + runtime data
```

