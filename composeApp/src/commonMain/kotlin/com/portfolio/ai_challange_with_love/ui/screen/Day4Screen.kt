package com.portfolio.ai_challange_with_love.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challange_with_love.data.AnalyzeResponse
import com.portfolio.ai_challange_with_love.ui.formatAiResponse
import com.portfolio.ai_challange_with_love.data.Recommendation
import com.portfolio.ai_challange_with_love.data.TemperatureApi
import com.portfolio.ai_challange_with_love.data.TemperatureResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val TEMPERATURES = listOf(0.0, 0.7, 1.2)
private const val EXPERIMENT_PROMPT = "How many meters of cable are needed to run internet to each apartment in a 16-story building?"

private enum class Phase {
    IDLE, STREAMING, ANALYZING, COMPLETE, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Day4Screen(onBack: () -> Unit) {
    val api = remember { TemperatureApi() }
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(Phase.IDLE) }
    var errorMessage by remember { mutableStateOf("") }
    var activeTemperatureIndex by remember { mutableStateOf(-1) }
    val streamingTexts = remember { mutableStateListOf("", "", "") }
    var analysis by remember { mutableStateOf<AnalyzeResponse?>(null) }
    var experimentJob by remember { mutableStateOf<Job?>(null) }

    fun resetState() {
        phase = Phase.IDLE
        errorMessage = ""
        activeTemperatureIndex = -1
        streamingTexts[0] = ""
        streamingTexts[1] = ""
        streamingTexts[2] = ""
        analysis = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 4: Temperature") },
                navigationIcon = {
                    TextButton(onClick = {
                        experimentJob?.cancel()
                        onBack()
                    }) {
                        Text("\u2190 Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Prompt card
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Prompt:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(EXPERIMENT_PROMPT, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Run button
            Button(
                onClick = {
                    resetState()
                    phase = Phase.STREAMING
                    experimentJob = scope.launch {
                        try {
                            // Stream each temperature sequentially
                            for ((index, temp) in TEMPERATURES.withIndex()) {
                                activeTemperatureIndex = index
                                api.streamTemperatureResult(EXPERIMENT_PROMPT, temp) { token ->
                                    streamingTexts[index] = streamingTexts[index] + token
                                }
                            }
                            activeTemperatureIndex = -1

                            // Analyze
                            phase = Phase.ANALYZING
                            val results = TEMPERATURES.mapIndexed { i, temp ->
                                TemperatureResponse(temp, streamingTexts[i])
                            }
                            analysis = api.analyzeResults(results)
                            phase = Phase.COMPLETE
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            errorMessage = e.message ?: "Unknown error"
                            phase = Phase.ERROR
                        }
                    }
                },
                enabled = phase == Phase.IDLE || phase == Phase.COMPLETE || phase == Phase.ERROR,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (phase) {
                        Phase.STREAMING -> "Generating responses..."
                        Phase.ANALYZING -> "Analyzing results..."
                        else -> "Run Experiment"
                    }
                )
            }

            // Content
            when (phase) {
                Phase.IDLE -> {
                    Text(
                        "Tap 'Run Experiment' to send the prompt with temperatures 0, 0.7, and 1.2.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Phase.STREAMING, Phase.ANALYZING, Phase.COMPLETE -> {
                    // Show all 3 cards in a horizontal row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val cardColors = listOf(
                            Color(0xFFE3F2FD), // light blue
                            Color(0xFFFFF3E0), // light orange
                            Color(0xFFE8F5E9), // light green
                        )
                        TEMPERATURES.forEachIndexed { index, temp ->
                            val text = streamingTexts[index]
                            val isActive = phase == Phase.STREAMING && index == activeTemperatureIndex
                            val isWaiting = phase == Phase.STREAMING && index > activeTemperatureIndex

                            StreamingResultCard(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                temperature = temp,
                                text = text,
                                isStreaming = isActive,
                                isWaiting = isWaiting,
                                containerColor = cardColors[index],
                            )
                        }
                    }

                    if (phase == Phase.ANALYZING) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp).size(20.dp))
                            Text("Analyzing responses...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (phase == Phase.COMPLETE && analysis != null) {
                        HorizontalDivider()
                        ComparisonSection(analysis!!.comparison)
                        RecommendationsSection(analysis!!.recommendations)
                    }
                }
                Phase.ERROR -> {
                    // Show any partial results in a row
                    val hasPartial = streamingTexts.any { it.isNotEmpty() }
                    if (hasPartial) {
                        val errorCardColors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFFFF3E0),
                            Color(0xFFE8F5E9),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TEMPERATURES.forEachIndexed { index, temp ->
                                val text = streamingTexts[index]
                                if (text.isNotEmpty()) {
                                    StreamingResultCard(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        temperature = temp,
                                        text = text,
                                        isStreaming = false,
                                        isWaiting = false,
                                        containerColor = errorCardColors[index],
                                    )
                                }
                            }
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            text = "Error: $errorMessage",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingResultCard(
    modifier: Modifier = Modifier,
    temperature: Double,
    text: String,
    isStreaming: Boolean,
    isWaiting: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "Temperature: $temperature",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (isStreaming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            when {
                isWaiting -> Text(
                    "Waiting...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                text.isEmpty() && isStreaming -> Text(
                    "Generating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(formatAiResponse(text), style = MaterialTheme.typography.bodyMedium, color = Color.Black)
            }
        }
    }
}

@Composable
private fun ComparisonSection(comparison: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(comparison, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecommendationsSection(recommendations: List<Recommendation>) {
    if (recommendations.isEmpty()) return

    Text(
        "Best Use Cases",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )

    recommendations.forEach { rec ->
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = "Temperature: ${rec.temperature}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Text(rec.bestFor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
