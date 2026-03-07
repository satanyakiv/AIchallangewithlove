package com.portfolio.ai_challenge.agent.psy_agent.model

// Assembled context passed to ContextWindowManager — contains all 3 layers
data class PsyAgentContext(
    val sessionId: String,
    val userId: String,
    val currentState: String,
    val currentMessages: List<ConversationEntry>,
    val userProfile: PsyUserProfile,
    val recentSessions: List<PsySessionSummary>,
    val domain: DomainKnowledge,
)
