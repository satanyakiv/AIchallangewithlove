package com.portfolio.ai_challange_with_love.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challange_with_love.data.AgentApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
)

data class Day6ViewState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class Day6ViewModel(private val agentApi: AgentApi) : ViewModel() {
    private val _state = MutableStateFlow(Day6ViewState())
    val state: StateFlow<Day6ViewState> = _state.asStateFlow()

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isLoading) return

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(text = trimmed, isUser = true),
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val response = agentApi.chat(trimmed)
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(text = response, isUser = false),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val errorMsg = e.message ?: "Unknown error"
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(text = "Error: $errorMsg", isUser = false),
                        isLoading = false,
                        errorMessage = errorMsg,
                    )
                }
            }
        }
    }
}
