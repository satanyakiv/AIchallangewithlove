package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challenge.navigation.AppScreen

private data class HubCard(
    val title: String,
    val subtitle: String,
    val description: String,
    val destination: AppScreen,
)

private val hubCards = listOf(
    HubCard(
        title = "Sliding Window",
        subtitle = "Strategy A",
        description = "Keep only the last N messages. Older messages are dropped before each API call. Zero overhead, bounded context.",
        destination = AppScreen.Day10Sliding,
    ),
    HubCard(
        title = "Sticky Facts",
        subtitle = "Strategy B",
        description = "Extract key-value facts from every exchange using DeepSeek. Each request sends facts dict + last messages. 2 API calls per message.",
        destination = AppScreen.Day10Facts,
    ),
    HubCard(
        title = "Branching",
        subtitle = "Strategy C",
        description = "Fork the conversation at any checkpoint. Each branch remembers shared history up to the fork, then its own messages.",
        destination = AppScreen.Day10Branching,
    ),
    HubCard(
        title = "Strategy Comparison",
        subtitle = "Metrics",
        description = "Compare all 3 strategies side by side: avg tokens/message, context retention %, and memory overhead score.",
        destination = AppScreen.Day10Comparison,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Day10HubScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 10: Context Strategies") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("\u2190 Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(hubCards.size) { index ->
                val card = hubCards[index]
                HubStrategyCard(card = card, onClick = { onNavigate(card.destination) })
            }
        }
    }
}

@Composable
private fun HubStrategyCard(card: HubCard, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = card.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
