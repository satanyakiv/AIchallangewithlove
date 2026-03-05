package com.portfolio.ai_challenge.di

import com.portfolio.ai_challenge.agent.Day10BranchingAgent
import com.portfolio.ai_challenge.agent.Day10FactsAgent
import com.portfolio.ai_challenge.agent.Day10SlidingAgent
import com.portfolio.ai_challenge.agent.Day6Agent
import com.portfolio.ai_challenge.agent.Day7Agent
import com.portfolio.ai_challenge.agent.Day9Agent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.ProfileExtractor
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyAgent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.UpdateProfileUseCase
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyPromptBuilder
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyResponseMapper
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.InMemoryContextStore
import com.portfolio.ai_challenge.models.LlmClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.dsl.bind
import org.koin.dsl.module

val serverModule = module {
    // Infrastructure
    single {
        HttpClient(CIO) { engine { requestTimeout = 120_000 } }
    }
    single {
        System.getenv("DEEPSEEK_API_KEY")
            ?: error("DEEPSEEK_API_KEY environment variable is not set")
    }

    // Shared
    single { LlmClient(get(), get()) }

    // Legacy agents (Day 6–10)
    single { Day6Agent(get()) }
    single { Day7Agent(get()) }
    single { Day9Agent(get()) }
    single { Day10SlidingAgent(get()) }
    single { Day10FactsAgent(get()) }
    single { Day10BranchingAgent(get()) }

    // Psy-Agent (Day 11+)
    single<ContextStore> { InMemoryContextStore() } bind ContextStore::class
    single { ContextWindowManager() }
    single { PsyPromptBuilder(get()) }
    single { PsyResponseMapper() }
    single { ProfileExtractor() }
    single { UpdateProfileUseCase(get(), get()) }
    single { PsyAgent(get(), get(), get(), get()) }
}
