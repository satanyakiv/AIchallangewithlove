package com.portfolio.ai_challenge.routes

import com.portfolio.ai_challenge.agent.ApiMessageDto
import com.portfolio.ai_challenge.agent.Day6Agent
import com.portfolio.ai_challenge.agent.Day7Agent
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class AgentChatRequest(val message: String)

@Serializable
data class AgentChatResponse(val response: String)

@Serializable
data class AgentChatV7Request(val messages: List<ApiMessageDto>)

fun Route.agentV7Routes(agent: Day7Agent) {
    route("/api/agent") {
        post("/chat-v7") {
            val request = call.receive<AgentChatV7Request>()
            if (request.messages.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Messages cannot be empty"))
                return@post
            }
            try {
                val result = agent.chat(request.messages)
                call.respond(AgentChatResponse(response = result))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Agent error"))
                )
            }
        }
    }
}

fun Route.agentRoutes(agent: Day6Agent) {
    route("/api/agent") {
        post("/chat") {
            val request = call.receive<AgentChatRequest>()
            if (request.message.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Message cannot be empty"))
                return@post
            }
            try {
                val result = agent.chat(request.message)
                call.respond(AgentChatResponse(response = result))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Agent error"))
                )
            }
        }
    }
}
