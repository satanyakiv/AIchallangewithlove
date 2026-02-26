package com.portfolio.ai_challenge.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AgentApiTest {

    private fun buildClient(mockEngine: MockEngine): HttpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Test
    fun `chatV7 returns response when server returns 200`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"response":"Mr. Anderson. We meet again."}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val api = AgentApi(buildClient(engine))
        val result = api.chatV7(listOf(ApiMessage("user", "Hello")))
        assertEquals("Mr. Anderson. We meet again.", result)
    }

    @Test
    fun `chatV7 throws exception with error body when server returns 500`() = runTest {
        val errorBody = """{"error":"DeepSeek API timeout"}"""
        val engine = MockEngine { _ ->
            respond(
                content = errorBody,
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val api = AgentApi(buildClient(engine))

        val ex = assertFailsWith<Exception> {
            api.chatV7(listOf(ApiMessage("user", "Hello")))
        }

        assertTrue(ex.message?.contains("500") == true, "Exception should mention status 500")
        assertTrue(ex.message?.contains(errorBody) == true, "Exception should contain error body")
    }

    @Test
    fun `chatV7 throws exception with status code when server returns 400`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"error":"Messages cannot be empty"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val api = AgentApi(buildClient(engine))

        val ex = assertFailsWith<Exception> {
            api.chatV7(emptyList())
        }

        assertTrue(ex.message?.contains("400") == true, "Exception should mention status 400")
    }

    @Test
    fun `chatV7 does not throw SerializationException on error response`() = runTest {
        // Regression test: before the fix, {"error":"..."} caused
        // "Field 'response' is required" SerializationException
        val engine = MockEngine { _ ->
            respond(
                content = """{"error":"something went wrong"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val api = AgentApi(buildClient(engine))

        val ex = assertFailsWith<Exception> {
            api.chatV7(listOf(ApiMessage("user", "ping")))
        }

        // Must NOT be a kotlinx.serialization.SerializationException about missing 'response' field
        val isSerialization = ex::class.simpleName?.contains("SerializationException") == true
            || ex.message?.contains("Field 'response' is required") == true
        assertTrue(!isSerialization, "Should not throw SerializationException: ${ex.message}")
    }

    @Test
    fun `chatV7 sends messages to correct endpoint`() = runTest {
        var capturedUrl = ""
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """{"response":"Inevitable."}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val api = AgentApi(buildClient(engine))
        api.chatV7(listOf(ApiMessage("user", "Are you real?")))

        assertTrue(capturedUrl.contains("/api/agent/chat-v7"), "URL should contain /api/agent/chat-v7, was: $capturedUrl")
    }
}
