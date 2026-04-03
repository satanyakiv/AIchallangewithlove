package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.freud_agent.FreudPromptBuilder
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudAgentContext
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudUserProfile
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionState
import com.portfolio.ai_challenge.models.MessageRole
import org.junit.Test
import kotlin.test.assertTrue

class FreudPromptBuilderTest {

    private val builder = FreudPromptBuilder()

    private fun contextWith(language: String): FreudAgentContext =
        FreudAgentContext(
            sessionId = "s1",
            userId = "u1",
            currentState = "begruessung",
            currentMessages = emptyList(),
            userProfile = FreudUserProfile(userId = "u1", language = language),
        )

    @Test
    fun testBuildMessages_ukrainianProfile_systemPromptContainsLanguageInstruction() {
        val context = contextWith("uk")
        val messages = builder.buildMessages(context, FreudSessionState.Begruessung)
        val systemContent = messages.first { it.role == MessageRole.SYSTEM }.content
        assertTrue(systemContent.contains("uk"), "System prompt should contain language instruction 'uk'")
    }

    @Test
    fun testBuildMessages_englishProfile_systemPromptContainsEnglish() {
        val context = contextWith("en")
        val messages = builder.buildMessages(context, FreudSessionState.Begruessung)
        val systemContent = messages.first { it.role == MessageRole.SYSTEM }.content
        assertTrue(systemContent.contains("en"), "System prompt should contain language instruction 'en'")
    }

    @Test
    fun testBuildMessages_systemPromptNoHardcodedEnglishOnly() {
        val context = contextWith("uk")
        val messages = builder.buildMessages(context, FreudSessionState.Begruessung)
        val systemContent = messages.first { it.role == MessageRole.SYSTEM }.content
        val hasHardcodedEnglish = systemContent.contains("All responses in English")
        assertTrue(!hasHardcodedEnglish, "System prompt should NOT contain hardcoded 'All responses in English'")
    }
}
