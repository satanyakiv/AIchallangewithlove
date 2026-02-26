package com.portfolio.ai_challange_with_love.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challange_with_love.data.AnalyzeResponse
import com.portfolio.ai_challange_with_love.data.Recommendation
import com.portfolio.ai_challange_with_love.ui.formatAiResponse
import org.koin.compose.viewmodel.koinViewModel

private val CARD_COLORS = listOf(
    Color(0xFFE3F2FD),
    Color(0xFFFFF3E0),
    Color(0xFFE8F5E9),
)

@Composable
fun Day4Screen(onBack: () -> Unit) {
    val viewModel: Day4ViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    Day4View(
        state = state,
        onBack = {
            viewModel.cancelExperiment()
            onBack()
        },
        onRunExperiment = viewModel::runExperiment,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day4View(
    state: Day4ViewState,
    onBack: () -> Unit,
    onRunExperiment: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 4: Temperature") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
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
            PromptCard(DAY4_PROMPT)

            RunExperimentButton(
                phase = state.phase,
                onClick = onRunExperiment,
            )

            Day4Content(state)
        }
    }
}

@Composable
private fun PromptCard(prompt: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Prompt:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(prompt, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RunExperimentButton(phase: ExperimentPhase, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = phase == ExperimentPhase.IDLE || phase == ExperimentPhase.COMPLETE || phase == ExperimentPhase.ERROR,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            when (phase) {
                ExperimentPhase.STREAMING -> "Generating responses..."
                ExperimentPhase.ANALYZING -> "Analyzing results..."
                else -> "Run Experiment"
            }
        )
    }
}

@Composable
private fun Day4Content(state: Day4ViewState) {
    when (state.phase) {
        ExperimentPhase.IDLE -> {
            Text(
                "Tap 'Run Experiment' to send the prompt with temperatures 0, 0.7, and 1.2.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ExperimentPhase.STREAMING, ExperimentPhase.ANALYZING, ExperimentPhase.COMPLETE -> {
            TemperatureResultsRow(state)

            if (state.phase == ExperimentPhase.ANALYZING) {
                HorizontalDivider()
                AnalyzingIndicator()
            }

            if (state.phase == ExperimentPhase.COMPLETE && state.analysis != null) {
                HorizontalDivider()
                ComparisonSection(state.analysis.comparison)
                RecommendationsSection(state.analysis.recommendations)
            }
        }
        ExperimentPhase.ERROR -> {
            Day4ErrorContent(state)
        }
    }
}

@Composable
private fun TemperatureResultsRow(state: Day4ViewState) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TEMPERATURES.forEachIndexed { index, temp ->
            StreamingResultCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                temperature = temp,
                text = state.streamingTexts[index],
                isStreaming = state.phase == ExperimentPhase.STREAMING && index == state.activeTemperatureIndex,
                isWaiting = state.phase == ExperimentPhase.STREAMING && index > state.activeTemperatureIndex,
                containerColor = CARD_COLORS[index],
            )
        }
    }
}

@Composable
private fun AnalyzingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp).size(20.dp))
        Text("Analyzing responses...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Day4ErrorContent(state: Day4ViewState) {
    val hasPartial = state.streamingTexts.any { it.isNotEmpty() }
    if (hasPartial) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TEMPERATURES.forEachIndexed { index, temp ->
                val text = state.streamingTexts[index]
                if (text.isNotEmpty()) {
                    StreamingResultCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        temperature = temp,
                        text = text,
                        isStreaming = false,
                        isWaiting = false,
                        containerColor = CARD_COLORS[index],
                    )
                }
            }
        }
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            text = "Error: ${state.errorMessage}",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
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
                    color = Color.Black,
                )
                text.isEmpty() && isStreaming -> Text(
                    "Generating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                )
                else -> Text(
                    formatAiResponse(text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                )
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
