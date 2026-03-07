package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.psy_agent.Day13PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.PsyChatResponse
import com.portfolio.ai_challenge.agent.psy_agent.PsyResponseMapper
import com.portfolio.ai_challenge.agent.psy_agent.UpdatePreferencesUseCase
import com.portfolio.ai_challenge.agent.psy_agent.memory.InMemoryContextStore
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyUserProfile
import com.portfolio.ai_challenge.agent.psy_agent.model.TurnContext
import com.portfolio.ai_challenge.routes.PsyStartResponse
import com.portfolio.ai_challenge.routes.day13PsyAgentRoutes
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

class Day13IntegrationTest {

    private fun fakeChatResult(
        response: String = "I hear you. Let's breathe together.",
        userId: String = "test1",
        state: String = "active_listening",
    ): PsyChatResult = PsyChatResult(
        response = response,
        state = state,
        session = PsySessionContext(sessionId = "sid", userId = userId, messages = emptyList()),
        profile = PsyUserProfile(userId = userId),
        turnContext = TurnContext(attemptCount = 1),
    )

    private fun buildMockAgent(response: String = "I hear you. Let's breathe together."): Day13PsyAgent {
        val mock = mockk<Day13PsyAgent>()
        coEvery { mock.startSession(any()) } answers {
            val store = InMemoryContextStore()
            val sessionId = java.util.UUID.randomUUID().toString()
            store.createSession(sessionId, firstArg())
            sessionId
        }
        coEvery { mock.chat(any(), any()) } returns fakeChatResult(response)
        return mock
    }

    @Test
    fun `POST psy13 start returns sessionId`() = testApplication {
        val mockAgent = buildMockAgent()
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        val testStore = InMemoryContextStore()
        routing { day13PsyAgentRoutes(mockAgent, PsyResponseMapper(), UpdatePreferencesUseCase(testStore), testStore) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.post("/api/agent/psy13/start") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("userId", "test1") }.toString())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<PsyStartResponse>()
        assertNotNull(body.sessionId)
        assertTrue(body.sessionId.isNotBlank())
    }

    @Test
    fun `POST psy13 start with empty userId returns 400`() = testApplication {
        val mockAgent = buildMockAgent()
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        val testStore = InMemoryContextStore()
        routing { day13PsyAgentRoutes(mockAgent, PsyResponseMapper(), UpdatePreferencesUseCase(testStore), testStore) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.post("/api/agent/psy13/start") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("userId", "") }.toString())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST psy13 chat returns response with state and memoryLayers`() = testApplication {
        val realStore = InMemoryContextStore()
        val mockAgent = mockk<Day13PsyAgent>()

        coEvery { mockAgent.startSession("user1") } answers {
            val id = "test-session-id"
            realStore.createSession(id, "user1")
            id
        }
        coEvery { mockAgent.chat("test-session-id", any()) } returns PsyChatResult(
            response = "That sounds really tough. I'm here with you.",
            state = "active_listening",
            session = PsySessionContext(sessionId = "test-session-id", userId = "user1", messages = emptyList()),
            profile = PsyUserProfile(userId = "user1"),
            turnContext = TurnContext(attemptCount = 1),
        )

        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        val testStore = InMemoryContextStore()
        routing { day13PsyAgentRoutes(mockAgent, PsyResponseMapper(), UpdatePreferencesUseCase(testStore), testStore) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val chatResponse = client.post("/api/agent/psy13/chat") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("sessionId", "test-session-id")
                put("message", "I feel anxious all the time")
            }.toString())
        }

        assertEquals(HttpStatusCode.OK, chatResponse.status)
        val body = chatResponse.body<PsyChatResponse>()
        assertEquals("That sounds really tough. I'm here with you.", body.response)
        assertEquals("active_listening", body.state)
        assertNotNull(body.memoryLayers)
    }

    @Test
    fun `POST psy13 chat with blank sessionId returns 400`() = testApplication {
        val mockAgent = buildMockAgent()
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        val testStore = InMemoryContextStore()
        routing { day13PsyAgentRoutes(mockAgent, PsyResponseMapper(), UpdatePreferencesUseCase(testStore), testStore) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.post("/api/agent/psy13/chat") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("sessionId", ""); put("message", "hello") }.toString())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST psy13 chat with blank message returns 400`() = testApplication {
        val mockAgent = buildMockAgent()
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        val testStore = InMemoryContextStore()
        routing { day13PsyAgentRoutes(mockAgent, PsyResponseMapper(), UpdatePreferencesUseCase(testStore), testStore) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.post("/api/agent/psy13/chat") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("sessionId", "some-id"); put("message", "") }.toString())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
