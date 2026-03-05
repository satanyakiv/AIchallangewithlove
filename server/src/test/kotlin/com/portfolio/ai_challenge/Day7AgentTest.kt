package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.ApiMessageDto
import com.portfolio.ai_challenge.agent.Day7Agent
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Day7AgentTest {

    private val llmClient = mockk<LlmClient>()
    private val agent = Day7Agent(llmClient)

    @Test
    fun testChat_withUserMessage_passesSystemAndUserMessages() = runBlocking {
        val slot = slot<List<DeepSeekMessage>>()
        coEvery { llmClient.complete(capture(slot), any(), any()) } returns "Mr. Anderson."

        agent.chat(listOf(ApiMessageDto(MessageRole.USER, "hello")))

        assertTrue(slot.captured.any { it.role == MessageRole.SYSTEM })
        assertTrue(slot.captured.any { it.content.contains("hello") })
    }

    @Test
    fun testChat_returnsLlmClientResponse() = runBlocking {
        coEvery { llmClient.complete(any(), any(), any()) } returns "Mr. Anderson."

        val result = agent.chat(listOf(ApiMessageDto(MessageRole.USER, "hi")))

        assertEquals("Mr. Anderson.", result)
    }

    @Test
    fun testChat_whenLlmThrows_propagatesException() {
        runBlocking {
            coEvery { llmClient.complete(any(), any(), any()) } throws Exception("timeout")

            assertFailsWith<Exception> {
                agent.chat(listOf(ApiMessageDto(MessageRole.USER, "hi")))
            }
        }
    }

    @Test
    fun testChat_multipleMessages_allPassedToLlm() = runBlocking {
        val slot = slot<List<DeepSeekMessage>>()
        coEvery { llmClient.complete(capture(slot), any(), any()) } returns "Inevitable."

        agent.chat(listOf(
            ApiMessageDto(MessageRole.USER, "first"),
            ApiMessageDto(MessageRole.ASSISTANT, "reply"),
            ApiMessageDto(MessageRole.USER, "second"),
        ))

        assertEquals(4, slot.captured.size)
        assertEquals(MessageRole.SYSTEM, slot.captured.first().role)
    }
}
