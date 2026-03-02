package com.portfolio.ai_challenge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.AgentChatV9Request
import com.portfolio.ai_challenge.data.ApiMessage
import com.portfolio.ai_challenge.database.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Compression triggers when accumulated prompt tokens exceed this threshold
internal const val TOKEN_THRESHOLD = 500

// How many recent messages to keep "as-is" when compression fires
private const val RECENT_KEEP = 2

data class TokenStats(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val wasCompressed: Boolean,
)

class Day9ViewModel(
    private val agentApi: AgentApi,
    private val repository: ChatRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _compressionEnabled = MutableStateFlow(false)
    val compressionEnabled: StateFlow<Boolean> = _compressionEnabled.asStateFlow()

    private val _tokenStats = MutableStateFlow<TokenStats?>(null)
    val tokenStats: StateFlow<TokenStats?> = _tokenStats.asStateFlow()

    private val _currentSummary = MutableStateFlow<String?>(null)
    val currentSummary: StateFlow<String?> = _currentSummary.asStateFlow()

    // Running total of prompt tokens across all exchanges in this conversation
    private val _cumulativePromptTokens = MutableStateFlow(0)
    val cumulativePromptTokens: StateFlow<Int> = _cumulativePromptTokens.asStateFlow()

    // Observe only real chat messages (exclude context_summary role)
    val messages: StateFlow<List<ChatMessage>> = repository.observeAllExcludingSummary()
        .map { entities ->
            entities.map { ChatMessage(text = it.content, isUser = it.role == "user") }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            _currentSummary.value = repository.getLatestSummary()?.content
        }
    }

    fun toggleCompression() {
        _compressionEnabled.value = !_compressionEnabled.value
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _currentSummary.value = null
            _tokenStats.value = null
            _cumulativePromptTokens.value = 0
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            repository.saveUserMessage(trimmed)

            val allEntities = repository.getAllExcludingSummary()
            val allMessages = allEntities.map { ApiMessage(role = it.role, content = it.content) }

            // Compression fires when accumulated prompt tokens exceed threshold
            val shouldCompress = _compressionEnabled.value
                && _cumulativePromptTokens.value >= TOKEN_THRESHOLD

            val (recentMessages, oldMessages) = if (shouldCompress) {
                val recent = allMessages.takeLast(RECENT_KEEP)
                val old = allMessages.dropLast(RECENT_KEEP)
                recent to old
            } else {
                allMessages to emptyList()
            }

            val existingSummary = if (shouldCompress) _currentSummary.value else null

            try {
                val response = agentApi.chatV9(
                    AgentChatV9Request(
                        recentMessages = recentMessages,
                        oldMessages = oldMessages,
                        existingSummary = existingSummary,
                        compressionEnabled = shouldCompress,
                    )
                )

                repository.saveAssistantMessage(response.response)

                if (response.newSummary != null) {
                    repository.saveSummary(response.newSummary)
                    _currentSummary.value = response.newSummary
                }

                // After compression, reset counter to the current request's token count
                // (fresh start with compressed context). Otherwise accumulate.
                if (shouldCompress) {
                    _cumulativePromptTokens.value = response.promptTokens
                } else {
                    _cumulativePromptTokens.value += response.promptTokens
                }

                _tokenStats.value = TokenStats(
                    promptTokens = response.promptTokens,
                    completionTokens = response.completionTokens,
                    totalTokens = response.totalTokens,
                    wasCompressed = shouldCompress,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                repository.saveAssistantMessage("Error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
