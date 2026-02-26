package com.portfolio.ai_challange_with_love.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challange_with_love.data.ModelApi
import com.portfolio.ai_challange_with_love.data.ModelCompareAnalysis
import com.portfolio.ai_challange_with_love.data.ModelMetrics
import com.portfolio.ai_challange_with_love.data.ModelResultForAnalysis
import com.portfolio.ai_challange_with_love.data.ModelStreamEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val DAY5_PROMPT = "How many tennis balls could be placed to F16?"

data class Day5ViewState(
    val phase: ExperimentPhase = ExperimentPhase.IDLE,
    val streamingTexts: List<String> = listOf("", "", ""),
    val completedModels: List<Boolean> = listOf(false, false, false),
    val metrics: List<ModelMetrics?> = listOf(null, null, null),
    val analysis: ModelCompareAnalysis? = null,
    val errorMessage: String = "",
)

class Day5ViewModel(private val modelApi: ModelApi) : ViewModel() {
    private val _state = MutableStateFlow(Day5ViewState())
    val state: StateFlow<Day5ViewState> = _state.asStateFlow()

    private var experimentJob: Job? = null

    fun runExperiment() {
        experimentJob?.cancel()
        _state.value = Day5ViewState(phase = ExperimentPhase.STREAMING)

        experimentJob = viewModelScope.launch {
            try {
                val texts = mutableListOf("", "", "")
                val completedModels = mutableListOf(false, false, false)
                val metrics = mutableListOf<ModelMetrics?>(null, null, null)

                modelApi.streamModelComparison(DAY5_PROMPT) { event ->
                    when (event) {
                        is ModelStreamEvent.ModelStart -> Unit
                        is ModelStreamEvent.ModelResult -> {
                            if (event.index in 0..2) {
                                texts[event.index] = event.content
                                completedModels[event.index] = true
                                _state.update {
                                    it.copy(
                                        streamingTexts = texts.toList(),
                                        completedModels = completedModels.toList(),
                                    )
                                }
                            }
                        }
                        is ModelStreamEvent.Metrics -> {
                            if (event.index in 0..2) {
                                metrics[event.index] = event.data
                                _state.update { it.copy(metrics = metrics.toList()) }
                            }
                        }
                        is ModelStreamEvent.Done -> Unit
                        is ModelStreamEvent.Error -> {
                            _state.update {
                                it.copy(errorMessage = event.message, phase = ExperimentPhase.ERROR)
                            }
                        }
                    }
                }

                if (_state.value.phase == ExperimentPhase.ERROR) return@launch

                _state.update { it.copy(phase = ExperimentPhase.ANALYZING) }

                val results = (0..2).mapNotNull { i ->
                    val m = metrics[i] ?: return@mapNotNull null
                    ModelResultForAnalysis(
                        modelLabel = m.modelLabel,
                        content = texts[i],
                        responseTimeMs = m.responseTimeMs,
                        totalTokens = m.totalTokens,
                        estimatedCost = m.estimatedCost,
                    )
                }

                if (results.size == 3) {
                    val analysis = modelApi.analyzeModelResults(results)
                    _state.update { it.copy(analysis = analysis) }
                }

                _state.update { it.copy(phase = ExperimentPhase.COMPLETE) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(errorMessage = e.message ?: "Unknown error", phase = ExperimentPhase.ERROR) }
            }
        }
    }

    fun cancelExperiment() {
        experimentJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        experimentJob?.cancel()
    }
}
