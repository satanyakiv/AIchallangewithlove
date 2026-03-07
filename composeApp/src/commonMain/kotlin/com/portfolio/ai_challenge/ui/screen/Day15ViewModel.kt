package com.portfolio.ai_challenge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challenge.data.MemoryLayersDebug
import com.portfolio.ai_challenge.data.PsyAgentApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Day15ViewModel(private val psyAgentApi: PsyAgentApi) : ViewModel() {

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _memoryDebug = MutableStateFlow<MemoryLayersDebug?>(null)
    val memoryDebug: StateFlow<MemoryLayersDebug?> = _memoryDebug.asStateFlow()

    private val _profileUpdates = MutableStateFlow<List<String>>(emptyList())
    val profileUpdates: StateFlow<List<String>> = _profileUpdates.asStateFlow()

    private val _currentState = MutableStateFlow("greeting")
    val currentState: StateFlow<String> = _currentState.asStateFlow()

    private val _intent = MutableStateFlow("")
    val intent: StateFlow<String> = _intent.asStateFlow()

    private val _transitions = MutableStateFlow<List<TransitionDebugUi>>(emptyList())
    val transitions: StateFlow<List<TransitionDebugUi>> = _transitions.asStateFlow()

    private val _violations = MutableStateFlow<List<String>>(emptyList())
    val violations: StateFlow<List<String>> = _violations.asStateFlow()

    private val _taskPhase = MutableStateFlow("assessment")
    val taskPhase: StateFlow<String> = _taskPhase.asStateFlow()

    private val _allowedTransitions = MutableStateFlow<List<String>>(emptyList())
    val allowedTransitions: StateFlow<List<String>> = _allowedTransitions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startSession(userId: String) {
        if (userId.isBlank() || _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = psyAgentApi.startSession(userId.trim())
                _sessionId.value = response.sessionId
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _error.value = e.message ?: "Failed to start session"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _sessionId.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _messages.value = _messages.value + ChatMessage(text = trimmed, isUser = true)
            try {
                val result = psyAgentApi.chat(sessionId, trimmed)
                _messages.value = _messages.value + ChatMessage(text = result.response, isUser = false)
                _memoryDebug.value = result.memoryLayers
                _profileUpdates.value = result.profileUpdates
                _currentState.value = result.state
                _intent.value = result.intent
                _transitions.value = _transitions.value + result.transitions.map { t ->
                    TransitionDebugUi(t.from, t.to, t.event)
                }
                _violations.value = result.violations
                _taskPhase.value = result.taskPhase.ifEmpty { _taskPhase.value }
                _allowedTransitions.value = result.allowedTransitions
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _messages.value = _messages.value + ChatMessage(
                    text = "Error: ${e.message ?: "Unknown error"}",
                    isUser = false,
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
