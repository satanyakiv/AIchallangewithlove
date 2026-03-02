package com.portfolio.ai_challenge.di

import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.ModelApi
import com.portfolio.ai_challenge.data.TemperatureApi
import com.portfolio.ai_challenge.data.createHttpClient
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
    single { TemperatureApi(get()) }
    single { ModelApi(get()) }
    single { AgentApi(get()) }
}