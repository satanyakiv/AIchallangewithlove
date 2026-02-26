package com.portfolio.ai_challenge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.ApiMessage
import com.portfolio.ai_challenge.database.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Day7ViewModel(
    private val agentApi: AgentApi,
    private val repository: ChatRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // DB is the single source of truth — UI observes this Flow
    val messages: StateFlow<List<ChatMessage>> = repository.observeMessages()
        .map { entities ->
            entities.map { ChatMessage(text = it.content, isUser = it.role == "user") }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            // Step 1: save user message to DB
            repository.saveUserMessage(trimmed)

            // Step 2: load all messages ordered by id ASC
            val allEntities = repository.getAllMessages()

            // Step 3: map DB entities to API format
            val apiMessages = allEntities.map { ApiMessage(role = it.role, content = it.content) }

            try {
                // Step 4: send full history to DeepSeek via server
                val response = agentApi.chatV7(apiMessages)
                // Step 5: save assistant response to DB
                repository.saveAssistantMessage(response)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                repository.saveAssistantMessage("Error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
