package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.Day6Agent
import com.portfolio.ai_challenge.agent.Day7Agent
import com.portfolio.ai_challenge.routes.agentRoutes
import com.portfolio.ai_challenge.routes.agentV7Routes
import com.portfolio.ai_challenge.routes.modelRoutes
import com.portfolio.ai_challenge.routes.temperatureRoutes
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val apiKey = System.getenv("DEEPSEEK_API_KEY")
        ?: error("DEEPSEEK_API_KEY environment variable is not set")

    val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 120_000
        }
    }

    val day6Agent = Day6Agent(apiKey)
    val day7Agent = Day7Agent(httpClient, apiKey)

    install(ServerContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        temperatureRoutes(httpClient, apiKey)
        modelRoutes(httpClient, apiKey)
        agentRoutes(day6Agent)
        agentV7Routes(day7Agent)
    }
}
