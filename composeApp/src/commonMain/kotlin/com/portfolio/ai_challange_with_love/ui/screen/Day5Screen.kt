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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challange_with_love.data.ModelApi
import com.portfolio.ai_challange_with_love.ui.formatAiResponse
import com.portfolio.ai_challange_with_love.data.ModelCompareAnalysis
import com.portfolio.ai_challange_with_love.data.ModelMetrics
import com.portfolio.ai_challange_with_love.data.ModelResultForAnalysis
import com.portfolio.ai_challange_with_love.data.ModelStreamEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val EXPERIMENT_PROMPT = "How many tennis balls could be placed to F16?"

private val MODEL_LABELS = listOf("Weak", "Medium", "Strong")
private val MODEL_CARD_COLORS = listOf(
    Color(0xFFE3F2FD), // light blue
    Color(0xFFFFF3E0), // light orange
    Color(0xFFE8F5E9), // light green
)

private enum class Day5Phase {
    IDLE, STREAMING, ANALYZING, COMPLETE, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Day5Screen(onBack: () -> Unit) {
    val api = remember { ModelApi() }
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(Day5Phase.IDLE) }
    var errorMessage by remember { mutableStateOf("") }
    val completedModels = remember { mutableStateListOf(false, false, false) }
    val streamingTexts = remember { mutableStateListOf("", "", "") }
    val metrics = remember { mutableStateListOf<ModelMetrics?>(null, null, null) }
    var analysis by remember { mutableStateOf<ModelCompareAnalysis?>(null) }
    var experimentJob by remember { mutableStateOf<Job?>(null) }

    fun resetState() {
        phase = Day5Phase.IDLE
        errorMessage = ""
        for (i in 0..2) {
            completedModels[i] = false
            streamingTexts[i] = ""
            metrics[i] = null
        }
        analysis = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 5: Model Versions") },
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
                    phase = Day5Phase.STREAMING
                    experimentJob = scope.launch {
                        try {
                            api.streamModelComparison(EXPERIMENT_PROMPT) { event ->
                                when (event) {
                                    is ModelStreamEvent.ModelStart -> {
                                        // All models start in parallel, nothing to track
                                    }
                                    is ModelStreamEvent.ModelResult -> {
                                        if (event.index in 0..2) {
                                            streamingTexts[event.index] = event.content
                                            completedModels[event.index] = true
                                        }
                                    }
                                    is ModelStreamEvent.Metrics -> {
                                        if (event.index in 0..2) {
                                            metrics[event.index] = event.data
                                        }
                                    }
                                    is ModelStreamEvent.Done -> {
                                        // All done
                                    }
                                    is ModelStreamEvent.Error -> {
                                        errorMessage = event.message
                                        phase = Day5Phase.ERROR
                                    }
                                }
                            }

                            if (phase == Day5Phase.ERROR) return@launch

                            // Analyze
                            phase = Day5Phase.ANALYZING
                            val results = (0..2).mapNotNull { i ->
                                val m = metrics[i] ?: return@mapNotNull null
                                ModelResultForAnalysis(
                                    modelLabel = m.modelLabel,
                                    content = streamingTexts[i],
                                    responseTimeMs = m.responseTimeMs,
                                    totalTokens = m.totalTokens,
                                    estimatedCost = m.estimatedCost,
                                )
                            }
                            if (results.size == 3) {
                                analysis = api.analyzeModelResults(results)
                            }
                            phase = Day5Phase.COMPLETE
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            errorMessage = e.message ?: "Unknown error"
                            phase = Day5Phase.ERROR
                        }
                    }
                },
                enabled = phase == Day5Phase.IDLE || phase == Day5Phase.COMPLETE || phase == Day5Phase.ERROR,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (phase) {
                        Day5Phase.STREAMING -> "Generating responses..."
                        Day5Phase.ANALYZING -> "Analyzing results..."
                        else -> "Run Experiment"
                    }
                )
            }

            // Content
            when (phase) {
                Day5Phase.IDLE -> {
                    Text(
                        "Tap 'Run Experiment' to compare Weak, Medium and Strong models on the same prompt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Day5Phase.STREAMING, Day5Phase.ANALYZING, Day5Phase.COMPLETE -> {
                    // Measurements row
                    val hasMetrics = metrics.any { it != null }
                    if (hasMetrics) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            (0..2).forEach { index ->
                                MetricsCard(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    label = MODEL_LABELS[index],
                                    metrics = metrics[index],
                                    isActive = phase == Day5Phase.STREAMING && !completedModels[index],
                                    containerColor = MODEL_CARD_COLORS[index],
                                )
                            }
                        }
                    }

                    // Responses row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        (0..2).forEach { index ->
                            val text = streamingTexts[index]
                            val isActive = phase == Day5Phase.STREAMING && !completedModels[index]

                            ModelResponseCard(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                label = MODEL_LABELS[index],
                                text = text,
                                isStreaming = isActive,
                                isWaiting = false,
                                containerColor = MODEL_CARD_COLORS[index],
                            )
                        }
                    }

                    // Comparison table
                    if (phase == Day5Phase.COMPLETE && metrics.all { it != null }) {
                        HorizontalDivider()
                        ComparisonTable(metrics = metrics.filterNotNull())
                    }

                    if (phase == Day5Phase.ANALYZING) {
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

                    if (phase == Day5Phase.COMPLETE && analysis != null) {
                        HorizontalDivider()
                        Day5AnalysisSection(analysis!!.comparison)
                    }
                }
                Day5Phase.ERROR -> {
                    // Show any partial results
                    val hasPartial = streamingTexts.any { it.isNotEmpty() }
                    if (hasPartial) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            (0..2).forEach { index ->
                                if (streamingTexts[index].isNotEmpty()) {
                                    ModelResponseCard(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        label = MODEL_LABELS[index],
                                        text = streamingTexts[index],
                                        isStreaming = false,
                                        isWaiting = false,
                                        containerColor = MODEL_CARD_COLORS[index],
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
                Text("$${String.format("%.6f", metrics.estimatedCost)}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
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
private fun ComparisonTable(metrics: List<ModelMetrics>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Weak", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Medium", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Strong", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            HorizontalDivider()

            // Speed row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Speed", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                metrics.forEach { m ->
                    Text("${m.responseTimeMs / 1000.0}s", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }

            // Tokens row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Tokens", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                metrics.forEach { m ->
                    Text("${m.totalTokens}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }

            // Cost row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Cost", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                metrics.forEach { m ->
                    Text("$${String.format("%.6f", m.estimatedCost)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }

            // Quality row (based on token count as proxy)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Quality", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                val labels = listOf("Basic", "Good", "Best")
                labels.forEach { l ->
                    Text(l, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun Day5AnalysisSection(comparison: String) {
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
