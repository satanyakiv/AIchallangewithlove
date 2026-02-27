package com.portfolio.ai_challenge.experiment.models

sealed class TestStep {
    data class Message(val id: String, val content: String) : TestStep()
    data class Checkpoint(val id: String, val failureMode: String, val content: String) : TestStep()
    data class Verification(val id: String, val failureMode: String, val content: String) : TestStep()
}

val TestStep.id: String get() = when (this) {
    is TestStep.Message -> id
    is TestStep.Checkpoint -> id
    is TestStep.Verification -> id
}

val TestStep.content: String get() = when (this) {
    is TestStep.Message -> content
    is TestStep.Checkpoint -> content
    is TestStep.Verification -> content
}

val TestStep.failureMode: String? get() = when (this) {
    is TestStep.Message -> null
    is TestStep.Checkpoint -> failureMode
    is TestStep.Verification -> failureMode
}

val TestStep.type: String get() = when (this) {
    is TestStep.Message -> "message"
    is TestStep.Checkpoint -> "checkpoint"
    is TestStep.Verification -> "verification"
}

data class TestCase(
    val name: String,
    val systemPrompt: String,
    val steps: List<TestStep>,
)
