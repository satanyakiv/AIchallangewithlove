package com.portfolio.ai_challange_with_love.di

import com.portfolio.ai_challange_with_love.data.AgentApi
import com.portfolio.ai_challange_with_love.data.ModelApi
import com.portfolio.ai_challange_with_love.data.TemperatureApi
import com.portfolio.ai_challange_with_love.data.createHttpClient
import com.portfolio.ai_challange_with_love.ui.screen.Day4ViewModel
import com.portfolio.ai_challange_with_love.ui.screen.Day5ViewModel
import com.portfolio.ai_challange_with_love.ui.screen.Day6ViewModel
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
}
