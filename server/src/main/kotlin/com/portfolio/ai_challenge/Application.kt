package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.Day10BranchingAgent
import com.portfolio.ai_challenge.agent.Day10FactsAgent
import com.portfolio.ai_challenge.agent.Day10SlidingAgent
import com.portfolio.ai_challenge.agent.Day6Agent
import com.portfolio.ai_challenge.agent.Day7Agent
import com.portfolio.ai_challenge.agent.Day9Agent
import com.portfolio.ai_challenge.agent.psy_agent.Day12PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.Day13PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.PsyResponseMapper
import com.portfolio.ai_challenge.agent.psy_agent.UpdatePreferencesUseCase
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.di.serverModule
import com.portfolio.ai_challenge.routes.day6AgentRoutes
import com.portfolio.ai_challenge.routes.agentV10Routes
import com.portfolio.ai_challenge.routes.agentV7Routes
import com.portfolio.ai_challenge.routes.agentV9Routes
import com.portfolio.ai_challenge.routes.day11PsyAgentRoutes
import com.portfolio.ai_challenge.routes.day12PsyAgentRoutes
import com.portfolio.ai_challenge.routes.day13PsyAgentRoutes
import com.portfolio.ai_challenge.routes.modelRoutes
import com.portfolio.ai_challenge.routes.temperatureRoutes
import io.ktor.client.HttpClient
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
import org.koin.core.context.startKoin

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val koin = startKoin { modules(serverModule) }.koin

    install(ServerContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        get("/") { call.respondText("Ktor: ${Greeting().greet()}") }
        temperatureRoutes(koin.get<HttpClient>(), koin.get<String>())
        modelRoutes(koin.get<HttpClient>(), koin.get<String>())
        day6AgentRoutes(koin.get<Day6Agent>())
        agentV7Routes(koin.get<Day7Agent>())
        agentV9Routes(koin.get<Day9Agent>())
        agentV10Routes(koin.get<Day10SlidingAgent>(), koin.get<Day10FactsAgent>(), koin.get<Day10BranchingAgent>())
        day11PsyAgentRoutes(koin.get<PsyAgent>(), koin.get<PsyResponseMapper>(), koin.get<ContextStore>())
        day12PsyAgentRoutes(koin.get<Day12PsyAgent>(), koin.get<PsyResponseMapper>(), koin.get<UpdatePreferencesUseCase>(), koin.get<ContextStore>())
        day13PsyAgentRoutes(koin.get<Day13PsyAgent>(), koin.get<PsyResponseMapper>(), koin.get<UpdatePreferencesUseCase>(), koin.get<ContextStore>())
    }
}
