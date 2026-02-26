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
data class AgentChatRequest(val message: String)

@Serializable
data class AgentChatResponse(val response: String)

@Serializable
data class ApiMessage(val role: String, val content: String)

@Serializable
data class AgentChatV7Request(val messages: List<ApiMessage>)

class AgentApi(private val client: HttpClient) {
    private val baseUrl = "http://${getServerHost()}:$SERVER_PORT"

    suspend fun chat(message: String): String {
        val response: AgentChatResponse = client.post("$baseUrl/api/agent/chat") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatRequest(message))
        }.body()
        return response.response
    }

    suspend fun chatV7(messages: List<ApiMessage>): String {
        val httpResponse = client.post("$baseUrl/api/agent/chat-v7") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatV7Request(messages))
        }
        if (!httpResponse.status.isSuccess()) {
            val errorBody = httpResponse.bodyAsText()
            throw Exception("Agent error (${httpResponse.status.value}): $errorBody")
        }
        return httpResponse.body<AgentChatResponse>().response
    }
}