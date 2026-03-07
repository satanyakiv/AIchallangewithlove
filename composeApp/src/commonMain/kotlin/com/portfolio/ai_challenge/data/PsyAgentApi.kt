package com.portfolio.ai_challenge.data

import com.portfolio.ai_challenge.SERVER_PORT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
data class TransitionDebugDto(val from: String, val to: String, val event: String)

@Serializable
data class PsyChatResponse(
    val response: String,
    val state: String,
    val memoryLayers: MemoryLayersDebug,
    val profileUpdates: List<String> = emptyList(),
    val intent: String = "",
    val transitions: List<TransitionDebugDto> = emptyList(),
    val violations: List<String> = emptyList(),
)

@Serializable
data class CommunicationPreferences(
    val language: String = "en",
    val formality: String = "INFORMAL",
    val responseLength: String = "MEDIUM",
    val avoidTopics: List<String> = emptyList(),
)

@Serializable
data class PsyUserProfileDto(
    val userId: String,
    val preferredName: String? = null,
    val primaryConcerns: List<String> = emptyList(),
    val knownTriggers: List<String> = emptyList(),
    val preferences: CommunicationPreferences = CommunicationPreferences(),
)

@Serializable
data class PsyPreferencesRequest(
    val userId: String,
    val language: String = "en",
    val formality: String = "INFORMAL",
    val responseLength: String = "MEDIUM",
    val avoidTopics: List<String> = emptyList(),
)

class PsyAgentApi(private val client: HttpClient, private val basePath: String) {
    private val baseUrl = "http://${getServerHost()}:$SERVER_PORT/api/agent/$basePath"

    suspend fun startSession(userId: String): PsyStartResponse {
        val httpResponse = client.post("$baseUrl/start") {
            contentType(ContentType.Application.Json)
            setBody(PsyStartRequest(userId))
        }
        if (!httpResponse.status.isSuccess()) {
            throw Exception("Psy agent error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        return httpResponse.body<PsyStartResponse>()
    }

    suspend fun chat(sessionId: String, message: String): PsyChatResponse {
        val httpResponse = client.post("$baseUrl/chat") {
            contentType(ContentType.Application.Json)
            setBody(PsyChatRequest(sessionId, message))
        }
        if (!httpResponse.status.isSuccess()) {
            throw Exception("Psy agent error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        return httpResponse.body<PsyChatResponse>()
    }

    suspend fun getProfile(userId: String): PsyUserProfileDto {
        val httpResponse = client.get("$baseUrl/profile") {
            parameter("userId", userId)
        }
        if (!httpResponse.status.isSuccess()) {
            throw Exception("Profile error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        return httpResponse.body<PsyUserProfileDto>()
    }

    suspend fun updatePreferences(userId: String, preferences: CommunicationPreferences): PsyUserProfileDto {
        val httpResponse = client.post("$baseUrl/profile/preferences") {
            contentType(ContentType.Application.Json)
            setBody(PsyPreferencesRequest(
                userId = userId,
                language = preferences.language,
                formality = preferences.formality,
                responseLength = preferences.responseLength,
                avoidTopics = preferences.avoidTopics,
            ))
        }
        if (!httpResponse.status.isSuccess()) {
            throw Exception("Preferences error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        return httpResponse.body<PsyUserProfileDto>()
    }
}
