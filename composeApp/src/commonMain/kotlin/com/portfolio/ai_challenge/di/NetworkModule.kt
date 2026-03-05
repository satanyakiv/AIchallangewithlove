package com.portfolio.ai_challenge.di

import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.ModelApi
import com.portfolio.ai_challenge.data.PsyAgentApi
import com.portfolio.ai_challenge.data.TemperatureApi
import com.portfolio.ai_challenge.data.createHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
    single { TemperatureApi(get()) }
    single { ModelApi(get()) }
    single { AgentApi(get()) }
    single(named("psy11")) { PsyAgentApi(get(), "psy11") }
    single(named("psy12")) { PsyAgentApi(get(), "psy12") }
    single(named("psy13")) { PsyAgentApi(get(), "psy13") }
}