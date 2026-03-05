package com.portfolio.ai_challenge.agent

object Prompts {
  object Day7 {
    val SYSTEM: String
      get() = "prompts/day7/system.txt".load()
  }

  private fun String.load(): String = this::class.java.classLoader.getResource(this)!!.readText().trim()
}