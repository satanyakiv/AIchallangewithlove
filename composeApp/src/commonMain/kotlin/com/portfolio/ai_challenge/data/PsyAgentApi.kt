package com.portfolio.ai_challenge.data

import com.portfolio.ai_challenge.SERVER_PORT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

@Serializable
data class PsyStartRequest(val userId: String)

@Serializable
data class PsyStartResponse(val sessionId: String)

@Serializable
data class PsyChatRequest(val sessionId: String, val message: String)

@Serializable
data class MemoryLayersDebug(
    val turn: String,
    val session: String,
    val profile: String,
)

@Serializable
data class PsyChatResponse(
    val response: String,
    val state: String,
    val memoryLayers: MemoryLayersDebug,
)

class PsyAgentApi(private val client: HttpClient) {
    private val baseUrl = "http://${getServerHost()}:$SERVER_PORT"

    suspend fun startSession(userId: String): PsyStartResponse {
        val httpResponse = client.post("$baseUrl/api/agent/psy/start") {
            contentType(ContentType.Application.Json)
            setBody(PsyStartRequest(userId))
        }
        if (!httpResponse.status.isSuccess()) {
            val errorBody = httpResponse.bodyAsText()
            throw Exception("Psy agent error (${httpResponse.status.value}): $errorBody")
        }
        return httpResponse.body<PsyStartResponse>()
    }

    suspend fun chat(sessionId: String, message: String): PsyChatResponse {
        val httpResponse = client.post("$baseUrl/api/agent/psy/chat") {
            contentType(ContentType.Application.Json)
            setBody(PsyChatRequest(sessionId, message))
        }
        if (!httpResponse.status.isSuccess()) {
            val errorBody = httpResponse.bodyAsText()
            throw Exception("Psy agent error (${httpResponse.status.value}): $errorBody")
        }
        return httpResponse.body<PsyChatResponse>()
    }
}
