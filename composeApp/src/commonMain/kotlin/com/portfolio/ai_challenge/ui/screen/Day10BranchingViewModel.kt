package com.portfolio.ai_challenge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challenge.data.AgentApi
import com.portfolio.ai_challenge.data.AgentChatV10BranchingRequest
import com.portfolio.ai_challenge.data.ApiMessage
import com.portfolio.ai_challenge.database.Day10BranchEntity
import com.portfolio.ai_challenge.database.Day10Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BranchingStats(
    val promptTokens: Int,
    val totalTokens: Int,
    val branchDepth: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class Day10BranchingViewModel(
    private val agentApi: AgentApi,
    private val repository: Day10Repository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentBranchId = MutableStateFlow("main")
    val currentBranchId: StateFlow<String> = _currentBranchId.asStateFlow()

    private val _stats = MutableStateFlow<BranchingStats?>(null)
    val stats: StateFlow<BranchingStats?> = _stats.asStateFlow()

    val branches: StateFlow<List<Day10BranchEntity>> = repository.observeBranches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<ChatMessage>> = _currentBranchId
        .flatMapLatest { branchId -> repository.observeBranchMessages(branchId) }
        .map { entities -> entities.map { ChatMessage(text = it.content, isUser = it.role == "user") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun switchBranch(branchId: String) {
        _currentBranchId.value = branchId
    }

    fun forkBranch(name: String) {
        viewModelScope.launch {
            val checkpointId = repository.getLastMainMessageId()
            val branchId = "branch_${System.currentTimeMillis()}"
            repository.createBranch(
                id = branchId,
                name = name,
                checkpointMessageId = checkpointId,
            )
            _currentBranchId.value = branchId
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearBranchingHistory()
            _currentBranchId.value = "main"
            _stats.value = null
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        val branchId = _currentBranchId.value

        viewModelScope.launch {
            _isLoading.value = true
            repository.saveBranchingUserMessage(branchId, trimmed)

            val contextMessages = repository.getBranchMessagesForContext(branchId)
            val apiMessages = contextMessages.map { ApiMessage(role = it.role, content = it.content) }

            try {
                val response = agentApi.chatV10Branching(
                    AgentChatV10BranchingRequest(messages = apiMessages)
                )
                repository.saveBranchingAssistantMessage(branchId, response.response)

                val currentBranches = repository.getBranches()
                val branchDepth = if (branchId == "main") 0 else currentBranches.size

                _stats.value = BranchingStats(
                    promptTokens = response.promptTokens,
                    totalTokens = response.totalTokens,
                    branchDepth = branchDepth,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                repository.saveBranchingAssistantMessage(branchId, "Error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
