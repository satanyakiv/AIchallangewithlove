package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.ApiMessageDto
import com.portfolio.ai_challenge.agent.Day7Agent
import com.portfolio.ai_challenge.routes.AgentChatResponse
import com.portfolio.ai_challenge.routes.AgentChatV7Request
import com.portfolio.ai_challenge.routes.agentV7Routes
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Day7AgentRouteTest {

    private val mockAgent = mockk<Day7Agent>()

    @Test
    fun `POST chat-v7 returns response field in JSON`() = testApplication {
        val agentReply = "Mr. Anderson. Welcome back."
        coEvery { mockAgent.chat(any()) } returns agentReply

        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        routing {
            agentV7Routes(mockAgent)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/api/agent/chat-v7") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatV7Request(messages = listOf(ApiMessageDto("user", "Hello"))))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AgentChatResponse>()
        assertNotNull(body.response)
        assertEquals(agentReply, body.response)
    }

    @Test
    fun `POST chat-v7 with empty messages returns 400`() = testApplication {
        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        routing {
            agentV7Routes(mockAgent)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/api/agent/chat-v7") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatV7Request(messages = emptyList()))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST chat-v7 sends full message history to agent`() = testApplication {
        val capturedMessages = mutableListOf<List<ApiMessageDto>>()
        coEvery { mockAgent.chat(capture(capturedMessages)) } returns "Inevitable."

        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        routing {
            agentV7Routes(mockAgent)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val history = listOf(
            ApiMessageDto("user", "Hello"),
            ApiMessageDto("assistant", "Mr. Anderson."),
            ApiMessageDto("user", "Are you real?"),
        )

        client.post("/api/agent/chat-v7") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatV7Request(messages = history))
        }

        assertEquals(1, capturedMessages.size)
        assertEquals(3, capturedMessages.first().size)
        assertEquals("user", capturedMessages.first().last().role)
        assertEquals("Are you real?", capturedMessages.first().last().content)
    }

    @Test
    fun `POST chat-v7 returns 500 when agent throws`() = testApplication {
        coEvery { mockAgent.chat(any()) } throws RuntimeException("DeepSeek timeout")

        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        routing {
            agentV7Routes(mockAgent)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/api/agent/chat-v7") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatV7Request(messages = listOf(ApiMessageDto("user", "Hello"))))
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `AgentChatV7Request serializes messages correctly`() {
        val json = Json { ignoreUnknownKeys = true }
        val request = AgentChatV7Request(
            messages = listOf(
                ApiMessageDto(role = "user", content = "Are you real?"),
                ApiMessageDto(role = "assistant", content = "Inevitably."),
            )
        )
        val serialized = json.encodeToString(AgentChatV7Request.serializer(), request)
        assertTrue(serialized.contains("\"role\""))
        assertTrue(serialized.contains("\"content\""))
        assertTrue(serialized.contains("Are you real?"))

        val deserialized = json.decodeFromString<AgentChatV7Request>(serialized)
        assertEquals(2, deserialized.messages.size)
        assertEquals("user", deserialized.messages[0].role)
    }
}
