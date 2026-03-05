package com.portfolio.ai_challenge.di

import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.PsyAgentApi
import com.portfolio.ai_challenge.database.ChatRepository
import com.portfolio.ai_challenge.database.Day10Repository
import com.portfolio.ai_challenge.ui.screen.Day10BranchingViewModel
import com.portfolio.ai_challenge.ui.screen.Day10FactsViewModel
import com.portfolio.ai_challenge.ui.screen.Day10SlidingViewModel
import com.portfolio.ai_challenge.ui.screen.Day11ViewModel
import com.portfolio.ai_challenge.ui.screen.Day12ViewModel
import com.portfolio.ai_challenge.ui.screen.Day13ViewModel
import com.portfolio.ai_challenge.ui.screen.Day4ViewModel
import com.portfolio.ai_challenge.ui.screen.Day5ViewModel
import com.portfolio.ai_challenge.ui.screen.Day6ViewModel
import com.portfolio.ai_challenge.ui.screen.Day7ViewModel
import com.portfolio.ai_challenge.ui.screen.Day9ViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val day4to9Module = module {
    viewModel { Day4ViewModel(get()) }
    viewModel { Day5ViewModel(get()) }
    viewModel { Day6ViewModel(get()) }
    viewModel { Day7ViewModel(get<AgentApi>(), get<ChatRepository>()) }
    viewModel { Day9ViewModel(get<AgentApi>(), get<ChatRepository>()) }
}

val day10Module = module {
    viewModel { Day10SlidingViewModel(get<AgentApi>(), get<Day10Repository>()) }
    viewModel { Day10FactsViewModel(get<AgentApi>(), get<Day10Repository>()) }
    viewModel { Day10BranchingViewModel(get<AgentApi>(), get<Day10Repository>()) }
}

val day11Module = module {
    viewModel { Day11ViewModel(get<PsyAgentApi>(named("psy11"))) }
}

val day12Module = module {
    viewModel { Day12ViewModel(get<PsyAgentApi>(named("psy12"))) }
}

val day13Module = module {
    viewModel { Day13ViewModel(get<PsyAgentApi>(named("psy13"))) }
}

val appModule = module {
    includes(networkModule, day4to9Module, day10Module, day11Module, day12Module, day13Module)
}