package com.portfolio.ai_challange_with_love.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challange_with_love.data.ModelCompareAnalysis
import com.portfolio.ai_challange_with_love.data.ModelMetrics
import com.portfolio.ai_challange_with_love.ui.formatAiResponse
import org.koin.compose.viewmodel.koinViewModel

private val MODEL_LABELS = listOf("Weak", "Medium", "Strong")
private val MODEL_CARD_COLORS = listOf(
    Color(0xFFE3F2FD),
    Color(0xFFFFF3E0),
    Color(0xFFE8F5E9),
)

@Composable
fun Day5Screen(onBack: () -> Unit) {
    val viewModel: Day5ViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    Day5View(
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
private fun Day5View(
    state: Day5ViewState,
    onBack: () -> Unit,
    onRunExperiment: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 5: Model Versions") },
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
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Prompt:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(DAY5_PROMPT, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Button(
                onClick = onRunExperiment,
                enabled = state.phase == ExperimentPhase.IDLE
                    || state.phase == ExperimentPhase.COMPLETE
                    || state.phase == ExperimentPhase.ERROR,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (state.phase) {
                        ExperimentPhase.STREAMING -> "Generating responses..."
                        ExperimentPhase.ANALYZING -> "Analyzing results..."
                        else -> "Run Experiment"
                    }
                )
            }

            Day5Content(state)
        }
    }
}

@Composable
private fun Day5Content(state: Day5ViewState) {
    when (state.phase) {
        ExperimentPhase.IDLE -> {
            Text(
                "Tap 'Run Experiment' to compare Weak, Medium and Strong models on the same prompt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ExperimentPhase.STREAMING, ExperimentPhase.ANALYZING, ExperimentPhase.COMPLETE -> {
            Day5ActiveContent(state)
        }
        ExperimentPhase.ERROR -> {
            Day5ErrorContent(state)
        }
    }
}

@Composable
private fun Day5ActiveContent(state: Day5ViewState) {
    val hasMetrics = state.metrics.any { it != null }
    if (hasMetrics) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (0..2).forEach { index ->
                MetricsCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = MODEL_LABELS[index],
                    metrics = state.metrics[index],
                    isActive = state.phase == ExperimentPhase.STREAMING && !state.completedModels[index],
                    containerColor = MODEL_CARD_COLORS[index],
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        (0..2).forEach { index ->
            ModelResponseCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = MODEL_LABELS[index],
                text = state.streamingTexts[index],
                isStreaming = state.phase == ExperimentPhase.STREAMING && !state.completedModels[index],
                containerColor = MODEL_CARD_COLORS[index],
            )
        }
    }

    if (state.phase == ExperimentPhase.COMPLETE && state.metrics.all { it != null }) {
        HorizontalDivider()
        ComparisonTable(metrics = state.metrics.filterNotNull())
    }

    if (state.phase == ExperimentPhase.ANALYZING) {
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

    if (state.phase == ExperimentPhase.COMPLETE && state.analysis != null) {
        HorizontalDivider()
        Day5AnalysisSection(state.analysis)
    }
}

@Composable
private fun Day5ErrorContent(state: Day5ViewState) {
    val hasPartial = state.streamingTexts.any { it.isNotEmpty() }
    if (hasPartial) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            (0..2).forEach { index ->
                if (state.streamingTexts[index].isNotEmpty()) {
                    ModelResponseCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = MODEL_LABELS[index],
                        text = state.streamingTexts[index],
                        isStreaming = false,
                        containerColor = MODEL_CARD_COLORS[index],
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
private fun MetricsCard(
    modifier: Modifier = Modifier,
    label: String,
    metrics: ModelMetrics?,
    isActive: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                if (isActive) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                }
            }
            if (metrics != null) {
                Text("${metrics.responseTimeMs / 1000.0}s", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                Text("${metrics.totalTokens} tokens", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                Text("\$${String.format("%.6f", metrics.estimatedCost)}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
            } else {
                Text("Waiting...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModelResponseCard(
    modifier: Modifier = Modifier,
    label: String,
    text: String,
    isStreaming: Boolean,
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
                        text = label,
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
                text.isEmpty() && isStreaming -> Text(
                    "Generating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ComparisonTable(metrics: List<ModelMetrics>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                listOf("Weak", "Medium", "Strong").forEach { header ->
                    Text(
                        header,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            HorizontalDivider()

            ComparisonRow("Speed", metrics.map { "${it.responseTimeMs / 1000.0}s" })
            ComparisonRow("Tokens", metrics.map { "${it.totalTokens}" })
            ComparisonRow("Cost", metrics.map { "\$${String.format("%.6f", it.estimatedCost)}" })
            ComparisonRow("Quality", listOf("Basic", "Good", "Best"))
        }
    }
}

@Composable
private fun ComparisonRow(label: String, values: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        values.forEach { value ->
            Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Day5AnalysisSection(analysis: ModelCompareAnalysis) {
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
            Text(analysis.comparison, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
