package com.portfolio.ai_challange_with_love.routes

import com.portfolio.ai_challange_with_love.agent.Day6Agent
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class AgentChatRequest(val message: String)

@Serializable
data class AgentChatResponse(val response: String)

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
