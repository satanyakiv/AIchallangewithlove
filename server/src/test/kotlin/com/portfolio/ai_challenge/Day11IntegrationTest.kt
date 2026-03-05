package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyAgent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyChatResponse
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyStartResponse
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.InMemoryContextStore
import com.portfolio.ai_challenge.routes.psyAgentRoutes
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Day11IntegrationTest {

    private fun buildPsyAgent(response: String = "I hear you. That sounds difficult."): PsyAgent {
        val mockAgent = mockk<PsyAgent>()
        coEvery { mockAgent.startSession(any()) } answers {
            val store = InMemoryContextStore()
            val sessionId = java.util.UUID.randomUUID().toString()
            store.createSession(sessionId, firstArg())
            sessionId
        }
        coEvery { mockAgent.chat(any(), any()) } returns com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyChatResponse(
            response = response,
            state = "active",
            memoryLayers = com.portfolio.ai_challenge.agent.day_11_psy_agent.MemoryLayersDebug(
                turn = "{ plan: null, attemptCount: 1, detectedEmotion: null }",
                session = "{ messageCount: 2, detectedEmotions: [] }",
                profile = "{ userId: test1, preferredName: null, concerns: [] }",
            ),
        )
        return mockAgent
    }

    @Test
    fun `POST psy start returns sessionId`() = testApplication {
        val mockAgent = buildPsyAgent()

        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        routing { psyAgentRoutes(mockAgent) }

        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val response = client.post("/api/agent/psy/start") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("userId", "test1") }.toString())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<PsyStartResponse>()
        assertNotNull(body.sessionId)
        assertTrue(body.sessionId.isNotBlank())
    }

    @Test
    fun `POST psy start with empty userId returns 400`() = testApplication {
        val mockAgent = buildPsyAgent()

        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        routing { psyAgentRoutes(mockAgent) }

        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val response = client.post("/api/agent/psy/start") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("userId", "") }.toString())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST psy chat returns response and memoryLayers`() = testApplication {
        val realStore = InMemoryContextStore()
        val realManager = ContextWindowManager()
        val mockAgent = mockk<PsyAgent>()

        coEvery { mockAgent.startSession("user1") } answers {
            val id = "test-session-id"
            realStore.createSession(id, "user1")
            id
        }
        coEvery { mockAgent.chat("test-session-id", any()) } returns PsyChatResponse(
            response = "I understand. Let's try Box Breathing together.",
            state = "active",
            memoryLayers = com.portfolio.ai_challenge.agent.day_11_psy_agent.MemoryLayersDebug(
                turn = "{ plan: null, attemptCount: 1, detectedEmotion: null }",
                session = "{ messageCount: 2, detectedEmotions: [] }",
                profile = "{ userId: user1, preferredName: null, concerns: [] }",
            ),
        )

        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        routing { psyAgentRoutes(mockAgent) }

        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val chatResponse = client.post("/api/agent/psy/chat") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("sessionId", "test-session-id")
                    put("message", "I feel anxious")
                }.toString()
            )
        }

        assertEquals(HttpStatusCode.OK, chatResponse.status)
        val body = chatResponse.body<PsyChatResponse>()
        assertEquals("I understand. Let's try Box Breathing together.", body.response)
        assertEquals("active", body.state)
        assertNotNull(body.memoryLayers)
        assertTrue(body.memoryLayers.session.contains("messageCount"))
        assertTrue(body.memoryLayers.profile.contains("userId"))
    }

    @Test
    fun `POST psy chat with missing sessionId returns 400`() = testApplication {
        val mockAgent = buildPsyAgent()

        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        routing { psyAgentRoutes(mockAgent) }

        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val response = client.post("/api/agent/psy/chat") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("sessionId", "")
                put("message", "hello")
            }.toString())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
