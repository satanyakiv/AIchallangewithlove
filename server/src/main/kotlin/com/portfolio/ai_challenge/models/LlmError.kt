package com.portfolio.ai_challenge.models

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse

sealed interface LlmError {
    data class HttpError(val statusCode: Int, val body: String) : LlmError
    data class ParseError(val cause: Throwable) : LlmError
}

fun <V> Result<V, LlmError>.getOrThrow(): V = getOrElse { error ->
    when (error) {
        is LlmError.HttpError -> throw Exception("DeepSeek error (${error.statusCode}): ${error.body}")
        is LlmError.ParseError -> throw Exception("Failed to parse LLM response", error.cause)
    }
}
