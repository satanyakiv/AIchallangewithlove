package com.portfolio.ai_challenge.di

import com.portfolio.ai_challenge.agent.Day10BranchingAgent
import com.portfolio.ai_challenge.agent.Day10FactsAgent
import com.portfolio.ai_challenge.agent.Day10SlidingAgent
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.portfolio.ai_challenge.agent.Day6Agent
import com.portfolio.ai_challenge.agent.Day7Agent
import com.portfolio.ai_challenge.agent.Day9Agent
import com.portfolio.ai_challenge.agent.psy_agent.Day12PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.Day13PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.Day14PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.Day15PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.Day15ResultAssembler
import com.portfolio.ai_challenge.agent.psy_agent.EnforceTaskPhaseUseCase
import com.portfolio.ai_challenge.agent.psy_agent.SyncTaskPhaseUseCase
import com.portfolio.ai_challenge.agent.psy_agent.DetectCrisisUseCase
import com.portfolio.ai_challenge.agent.psy_agent.ValidateAndRetryUseCase
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantChecker
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantPromptInjector
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoDiagnosisInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoMedicationInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoPromptLeakInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoProfanityInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.ResponseLengthInvariant
import com.portfolio.ai_challenge.agent.psy_agent.SessionStateToIntentMapper
import com.portfolio.ai_challenge.agent.psy_agent.PersonalizeResponseUseCase
import com.portfolio.ai_challenge.agent.psy_agent.ProfileExtractor
import com.portfolio.ai_challenge.agent.psy_agent.PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.UpdatePreferencesUseCase
import com.portfolio.ai_challenge.agent.psy_agent.UpdateProfileUseCase
import com.portfolio.ai_challenge.agent.psy_agent.PsyPromptBuilder
import com.portfolio.ai_challenge.agent.psy_agent.PsyResponseMapper
import com.portfolio.ai_challenge.agent.freud_agent.DetectFreudEventUseCase
import com.portfolio.ai_challenge.agent.freud_agent.FreudAgent
import com.portfolio.ai_challenge.agent.freud_agent.FreudProfileExtractor
import com.portfolio.ai_challenge.agent.freud_agent.FreudPromptBuilder
import com.portfolio.ai_challenge.agent.freud_agent.FreudResponseMapper
import com.portfolio.ai_challenge.agent.freud_agent.FreudStateToIntentMapper
import com.portfolio.ai_challenge.agent.freud_agent.UpdateFreudProfileUseCase
import com.portfolio.ai_challenge.agent.freud_agent.memory.FreudContextStore
import com.portfolio.ai_challenge.agent.freud_agent.memory.FreudInMemoryContextStore
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.psy_agent.memory.InMemoryContextStore
import com.portfolio.ai_challenge.config.ServerConfig
import com.portfolio.ai_challenge.config.loadServerConfig
import com.portfolio.ai_challenge.models.LlmClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.dsl.bind
import org.koin.dsl.module

val serverModule = module {
    // Infrastructure
    single { loadServerConfig() }
    single {
        val config = get<ServerConfig>()
        HttpClient(CIO) { engine { requestTimeout = config.deepseek.requestTimeoutMs } }
    }
    single {
        get<ServerConfig>().deepseek.apiKey
    }

    // Shared
    single {
        val config = get<ServerConfig>()
        LlmClient(get(), config.deepseek.apiKey, config.deepseek.apiUrl, config.deepseek.model)
    }

    // Legacy agents (Day 6–10)
    single {
        val client = DeepSeekLLMClient(get<String>())
        Day6Agent(client, SingleLLMPromptExecutor(client))
    }
    single { Day7Agent(get()) }
    single { Day9Agent(get()) }
    single { Day10SlidingAgent(get()) }
    single { Day10FactsAgent(get()) }
    single { Day10BranchingAgent(get()) }

    // Psy-Agent (Day 11+)
    single<ContextStore> { InMemoryContextStore() } bind ContextStore::class
    single { ContextWindowManager() }
    single { PersonalizeResponseUseCase() }
    single { PsyPromptBuilder(get(), get()) }
    single { PsyResponseMapper() }
    single { ProfileExtractor() }
    single { UpdateProfileUseCase(get(), get()) }
    single { UpdatePreferencesUseCase(get()) }
    single { DetectCrisisUseCase() }
    single { SessionStateToIntentMapper() }
    single {
        InvariantChecker(
            listOf(NoDiagnosisInvariant(), NoMedicationInvariant(), NoProfanityInvariant(), ResponseLengthInvariant(), NoPromptLeakInvariant())
        )
    }
    single {
        InvariantPromptInjector(
            listOf(NoDiagnosisInvariant(), NoMedicationInvariant(), NoProfanityInvariant(), ResponseLengthInvariant(), NoPromptLeakInvariant())
        )
    }
    single { ValidateAndRetryUseCase(get(), get(), get()) }
    single { PsyAgent(get(), get(), get(), get()) }
    single { Day12PsyAgent(get(), get(), get(), get()) }
    single { Day13PsyAgent(get(), get(), get(), get(), get(), get()) }
    single { Day14PsyAgent(get(), get(), get(), get(), get(), get(), get()) }
    single { EnforceTaskPhaseUseCase() }
    single { SyncTaskPhaseUseCase() }
    single { Day15ResultAssembler(get(), get()) }
    single { Day15PsyAgent(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    // Freud Agent
    single<FreudContextStore> { FreudInMemoryContextStore() }
    single { FreudProfileExtractor() }
    single { UpdateFreudProfileUseCase(get(), get()) }
    single { FreudStateToIntentMapper() }
    single { FreudPromptBuilder() }
    single { FreudResponseMapper() }
    single { DetectFreudEventUseCase() }
    single { FreudAgent(get(), get(), get(), get(), get(), get()) }
}
