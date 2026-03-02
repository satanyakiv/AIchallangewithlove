package com.portfolio.ai_challenge.di

import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.ModelApi
import com.portfolio.ai_challenge.data.TemperatureApi
import com.portfolio.ai_challenge.data.createHttpClient
import com.portfolio.ai_challenge.database.ChatRepository
import com.portfolio.ai_challenge.database.Day10Repository
import com.portfolio.ai_challenge.ui.screen.Day10BranchingViewModel
import com.portfolio.ai_challenge.ui.screen.Day10FactsViewModel
import com.portfolio.ai_challenge.ui.screen.Day10SlidingViewModel
import com.portfolio.ai_challenge.ui.screen.Day4ViewModel
import com.portfolio.ai_challenge.ui.screen.Day5ViewModel
import com.portfolio.ai_challenge.ui.screen.Day6ViewModel
import com.portfolio.ai_challenge.ui.screen.Day7ViewModel
import com.portfolio.ai_challenge.ui.screen.Day9ViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { createHttpClient() }

    single { TemperatureApi(get()) }
    single { ModelApi(get()) }
    single { AgentApi(get()) }

    viewModel { Day4ViewModel(get()) }
    viewModel { Day5ViewModel(get()) }
    viewModel { Day6ViewModel(get()) }
    viewModel { Day7ViewModel(get<AgentApi>(), get<ChatRepository>()) }
    viewModel { Day9ViewModel(get<AgentApi>(), get<ChatRepository>()) }
    viewModel { Day10SlidingViewModel(get<AgentApi>(), get<Day10Repository>()) }
    viewModel { Day10FactsViewModel(get<AgentApi>(), get<Day10Repository>()) }
    viewModel { Day10BranchingViewModel(get<AgentApi>(), get<Day10Repository>()) }
}
