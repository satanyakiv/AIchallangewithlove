package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Day10ComparisonScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 10: Strategy Comparison") },
                navigationIcon = { TextButton(onClick = onBack) { Text("\u2190 Back") } },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Scenario header
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Test Scenario",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "\"Task Management App\" — 5 messages",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "Budget, platform, deadline, features, and tech stack questions sent to each strategy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            }

            // Metric 1: Avg Tokens / Message
            MetricCard(
                title = "Metric 1: Avg Tokens / Message",
                subtitle = "Fewer tokens = cheaper & faster per message",
            ) {
                Day10ComparisonData.strategies.forEach { strategy ->
                    val progress = strategy.avgTokensPerMessage.toFloat() / Day10ComparisonData.maxAvgTokens
                    StrategyBar(
                        name = strategy.strategyName,
                        label = "${strategy.avgTokensPerMessage} tokens",
                        progress = progress,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Metric 2: Context Retention %
            MetricCard(
                title = "Metric 2: Context Retention %",
                subtitle = "How much of the original context is available at round 5",
            ) {
                Day10ComparisonData.strategies.forEach { strategy ->
                    val progress = strategy.contextRetentionPercent / 100f
                    StrategyBar(
                        name = strategy.strategyName,
                        label = "${strategy.contextRetentionPercent}%",
                        progress = progress,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            // Metric 3: Memory Overhead
            MetricCard(
                title = "Metric 3: Memory Overhead (1–5)",
                subtitle = "Storage + computation cost to maintain context",
            ) {
                Day10ComparisonData.strategies.forEach { strategy ->
                    val progress = strategy.memoryOverheadScore / 5f
                    StrategyBar(
                        name = strategy.strategyName,
                        label = "${strategy.memoryOverheadScore}/5 — ${strategy.memoryOverheadNote}",
                        progress = progress,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Summary
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Key Takeaways", style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "\uD83E\uDE9F Sliding Window — lowest cost, but forgets early context.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "\uD83D\uDCCB Sticky Facts — compact but loses conversational nuance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "\uD83D\uDD00 Branching — full context always, best for exploring alternatives.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun StrategyBar(
    name: String,
    label: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = MaterialTheme.typography.labelMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
