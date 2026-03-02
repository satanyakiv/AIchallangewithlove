package com.portfolio.ai_challenge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.AgentChatV10SlidingRequest
import com.portfolio.ai_challenge.data.ApiMessage
import com.portfolio.ai_challenge.database.Day10Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SlidingStats(
    val promptTokens: Int,
    val totalTokens: Int,
    val windowedCount: Int,
    val droppedCount: Int,
)

class Day10SlidingViewModel(
    private val agentApi: AgentApi,
    private val repository: Day10Repository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _windowSize = MutableStateFlow(10)
    val windowSize: StateFlow<Int> = _windowSize.asStateFlow()

    private val _stats = MutableStateFlow<SlidingStats?>(null)
    val stats: StateFlow<SlidingStats?> = _stats.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repository.observeSlidingMessages()
        .map { entities -> entities.map { ChatMessage(text = it.content, isUser = it.role == "user") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWindowSize(n: Int) {
        _windowSize.value = n
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearSlidingHistory()
            _stats.value = null
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            repository.saveSlidingUserMessage(trimmed)

            val allEntities = repository.getSlidingMessages()
            val apiMessages = allEntities.map { ApiMessage(role = it.role, content = it.content) }

            try {
                val response = agentApi.chatV10Sliding(
                    AgentChatV10SlidingRequest(
                        messages = apiMessages,
                        windowSize = _windowSize.value,
                    )
                )
                repository.saveSlidingAssistantMessage(response.response)
                _stats.value = SlidingStats(
                    promptTokens = response.promptTokens,
                    totalTokens = response.totalTokens,
                    windowedCount = response.windowedCount,
                    droppedCount = response.droppedCount,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                repository.saveSlidingAssistantMessage("Error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
