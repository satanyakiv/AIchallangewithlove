package com.portfolio.ai_challenge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.AgentChatV10FactsRequest
import com.portfolio.ai_challenge.data.ApiMessage
import com.portfolio.ai_challenge.database.Day10Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FactsStats(
    val promptTokens: Int,
    val totalTokens: Int,
)

class Day10FactsViewModel(
    private val agentApi: AgentApi,
    private val repository: Day10Repository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _stats = MutableStateFlow<FactsStats?>(null)
    val stats: StateFlow<FactsStats?> = _stats.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repository.observeFactsMessages()
        .map { entities -> entities.map { ChatMessage(text = it.content, isUser = it.role == "user") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val facts: StateFlow<Map<String, String>> = repository.observeFacts()
        .map { entities -> entities.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearFactsHistory()
            _stats.value = null
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            repository.saveFactsUserMessage(trimmed)

            val allEntities = repository.getFactsMessages()
            val apiMessages = allEntities.map { ApiMessage(role = it.role, content = it.content) }
            val currentFacts = repository.getAllFacts().associate { it.key to it.value }

            try {
                val response = agentApi.chatV10Facts(
                    AgentChatV10FactsRequest(
                        messages = apiMessages,
                        existingFacts = currentFacts,
                    )
                )
                repository.saveFactsAssistantMessage(response.response)

                // Persist updated facts
                response.updatedFacts.forEach { (key, value) ->
                    repository.upsertFact(key, value)
                }

                _stats.value = FactsStats(
                    promptTokens = response.promptTokens,
                    totalTokens = response.totalTokens,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                repository.saveFactsAssistantMessage("Error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
