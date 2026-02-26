package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.ApiMessageDto
import com.portfolio.ai_challenge.agent.Day7Agent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Day7AgentTest {

    private val fakeDeepSeekResponse = """
        {
          "choices": [
            {
              "message": { "role": "assistant", "content": "Mr. Anderson." },
              "finish_reason": "stop"
            }
          ]
        }
    """.trimIndent()

    private fun readBodyAsString(request: HttpRequestData): String =
        (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString() ?: ""

    private fun buildMockClient(
        onRequest: ((HttpRequestData) -> Unit)? = null,
        responseBody: String = fakeDeepSeekResponse,
        responseStatus: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient {
        val engine = MockEngine { request ->
            onRequest?.invoke(request)
            respond(
                content = ByteReadChannel(responseBody),
                status = responseStatus,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        return HttpClient(engine)
    }

    @Test
    fun `chat request body includes model field`() = runBlocking {
        var capturedBody = ""
        val client = buildMockClient(onRequest = { request ->
            capturedBody = readBodyAsString(request)
        })
        val agent = Day7Agent(client, apiKey = "fake-key")

        agent.chat(listOf(ApiMessageDto("user", "hello")))

        assertContains(capturedBody, "\"model\"", message = "Request body must include 'model' field")
        assertContains(capturedBody, "deepseek-chat", message = "Model value must be 'deepseek-chat'")
    }

    @Test
    fun `chat request body includes messages field`() = runBlocking {
        var capturedBody = ""
        val client = buildMockClient(onRequest = { request ->
            capturedBody = readBodyAsString(request)
        })
        val agent = Day7Agent(client, apiKey = "fake-key")

        agent.chat(listOf(ApiMessageDto("user", "hello world")))

        assertContains(capturedBody, "\"messages\"")
        assertContains(capturedBody, "hello world")
    }

    @Test
    fun `chat request body includes system message`() = runBlocking {
        var capturedBody = ""
        val client = buildMockClient(onRequest = { request ->
            capturedBody = readBodyAsString(request)
        })
        val agent = Day7Agent(client, apiKey = "fake-key")

        agent.chat(listOf(ApiMessageDto("user", "hello")))

        assertContains(capturedBody, "\"system\"", message = "Request must include system message")
    }

    @Test
    fun `chat returns content from choices`() = runBlocking {
        val client = buildMockClient()
        val agent = Day7Agent(client, apiKey = "fake-key")

        val result = agent.chat(listOf(ApiMessageDto("user", "hello")))

        assertEquals("Mr. Anderson.", result)
    }

    @Test
    fun `chat throws when DeepSeek returns error status`() = runBlocking {
        val client = buildMockClient(
            responseBody = """{"error":{"message":"unauthorized","type":"auth_error"}}""",
            responseStatus = HttpStatusCode.Unauthorized,
        )
        val agent = Day7Agent(client, apiKey = "bad-key")

        val ex = assertFailsWith<Exception> {
            agent.chat(listOf(ApiMessageDto("user", "hello")))
        }
        assertContains(ex.message ?: "", "401")
    }
}
