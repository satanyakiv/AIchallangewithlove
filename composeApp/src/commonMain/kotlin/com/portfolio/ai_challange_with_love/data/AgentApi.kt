package com.portfolio.ai_challange_with_love.data

import com.portfolio.ai_challange_with_love.SERVER_PORT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class AgentChatRequest(val message: String)

@Serializable
data class AgentChatResponse(val response: String)

class AgentApi(private val client: HttpClient) {
    private val baseUrl = "http://${getServerHost()}:$SERVER_PORT"

    suspend fun chat(message: String): String {
        val response: AgentChatResponse = client.post("$baseUrl/api/agent/chat") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatRequest(message))
        }.body()
        return response.response
    }
}