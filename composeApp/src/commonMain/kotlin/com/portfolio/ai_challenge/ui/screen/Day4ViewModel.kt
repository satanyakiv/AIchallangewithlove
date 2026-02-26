package com.portfolio.ai_challenge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.ai_challenge.data.AnalyzeResponse
import com.portfolio.ai_challenge.data.TemperatureApi
import com.portfolio.ai_challenge.data.TemperatureResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val TEMPERATURES = listOf(0.0, 0.7, 1.2)
const val DAY4_PROMPT = "How many meters of cable are needed to run internet to each apartment in a 16-story building?"

enum class ExperimentPhase {
    IDLE, STREAMING, ANALYZING, COMPLETE, ERROR
}

data class Day4ViewState(
    val phase: ExperimentPhase = ExperimentPhase.IDLE,
    val streamingTexts: List<String> = listOf("", "", ""),
    val activeTemperatureIndex: Int = -1,
    val analysis: AnalyzeResponse? = null,
    val errorMessage: String = "",
)

class Day4ViewModel(private val temperatureApi: TemperatureApi) : ViewModel() {
    private val _state = MutableStateFlow(Day4ViewState())
    val state: StateFlow<Day4ViewState> = _state.asStateFlow()

    private var experimentJob: Job? = null

    fun runExperiment() {
        experimentJob?.cancel()
        _state.value = Day4ViewState(phase = ExperimentPhase.STREAMING)

        experimentJob = viewModelScope.launch {
            try {
                val texts = mutableListOf("", "", "")

                for ((index, temp) in TEMPERATURES.withIndex()) {
                    _state.update { it.copy(activeTemperatureIndex = index) }
                    temperatureApi.streamTemperatureResult(DAY4_PROMPT, temp) { token ->
                        texts[index] = texts[index] + token
                        _state.update { it.copy(streamingTexts = texts.toList()) }
                    }
                }

                _state.update { it.copy(activeTemperatureIndex = -1, phase = ExperimentPhase.ANALYZING) }

                val results = TEMPERATURES.mapIndexed { i, temp ->
                    TemperatureResponse(temp, texts[i])
                }
                val analysis = temperatureApi.analyzeResults(results)

                _state.update { it.copy(analysis = analysis, phase = ExperimentPhase.COMPLETE) }
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
