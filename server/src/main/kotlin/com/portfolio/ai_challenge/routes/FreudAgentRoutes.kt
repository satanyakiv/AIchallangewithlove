package com.portfolio.ai_challenge.routes

import com.portfolio.ai_challenge.agent.freud_agent.FreudAgent
import com.portfolio.ai_challenge.agent.freud_agent.FreudResponseMapper
import com.portfolio.ai_challenge.agent.freud_agent.memory.FreudContextStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.freudAgentRoutes(
    agent: FreudAgent,
    responseMapper: FreudResponseMapper,
    contextStore: FreudContextStore,
) {
    route("/api/agent/freud") {
        post("/start") {
            val request = call.receive<PsyStartRequest>()
            if (request.userId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId cannot be empty"))
                return@post
            }
            val sessionId = agent.startSession(request.userId)
            call.respond(PsyStartResponse(sessionId = sessionId))
        }

        post("/chat") {
            val request = call.receive<PsyChatRequest>()
            if (request.sessionId.isBlank() || request.message.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "sessionId and message cannot be empty"))
                return@post
            }
            try {
                val result = agent.chat(request.sessionId, request.message)
                call.respond(responseMapper.toChatResponse(result))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Session not found")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Agent error")))
            }
        }

        get("/profile") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                return@get
            }
            call.respond(contextStore.loadProfile(userId))
        }
    }
}
