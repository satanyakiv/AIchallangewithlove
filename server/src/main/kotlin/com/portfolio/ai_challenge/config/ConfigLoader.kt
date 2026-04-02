package com.portfolio.ai_challenge.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import com.sksamuel.hoplite.addEnvironmentSource

fun loadServerConfig(): ServerConfig = ConfigLoaderBuilder.default()
    .addEnvironmentSource()
    .addResourceSource("/application.yaml")
    .build()
    .loadConfigOrThrow<ServerConfig>()
