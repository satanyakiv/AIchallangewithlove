package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

private val DEMO_PROMPTS = listOf(
    "I want to build a task management app for small teams",
    "Target platform: iOS and Android. Budget: \$80k",
    "Deadline is end of Q2 2026. Team: 2 devs, 1 designer",
    "Key features: task assignment, deadlines, notifications",
    "Users are project managers at mid-size companies",
)

@Composable
fun Day10FactsScreen(onBack: () -> Unit) {
    val viewModel: Day10FactsViewModel = koinViewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val facts by viewModel.facts.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    FactsView(
        messages = messages,
        isLoading = isLoading,
        facts = facts,
        stats = stats,
        listState = listState,
        onBack = onBack,
        onSend = viewModel::sendMessage,
        onClear = viewModel::clearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FactsView(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    facts: Map<String, String>,
    stats: FactsStats?,
    listState: LazyListState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 10b: Sticky Facts") },
                navigationIcon = { TextButton(onClick = onBack) { Text("\u2190 Back") } },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Text("\uD83D\uDDD1", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = {
            Column {
                FactsStatsBar(facts = facts, stats = stats)
                if (inputText.isEmpty()) {
                    SuggestionChipsRow(prompts = DEMO_PROMPTS, onSuggestionClick = { inputText = it })
                }
                ChatInputBar(
                    inputText = inputText,
                    isLoading = isLoading,
                    onInputChange = { inputText = it },
                    onSend = { onSend(inputText); inputText = "" },
                )
            }
        },
    ) { padding ->
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Text("\uD83D\uDCCB Sticky Facts", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "After each message, DeepSeek extracts key facts.\nFacts are shown above and injected into every API call.\nUse demo prompts to build up a rich facts dictionary.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Day10ChatMessageList(messages = messages, isLoading = isLoading, listState = listState, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun FactsStatsBar(
    facts: Map<String, String>,
    stats: FactsStats?,
) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "\uD83D\uDCCB Facts (${facts.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (stats != null) {
                    Text(
                        "Prompt tokens: ${stats.promptTokens}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (facts.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                facts.forEach { (key, value) ->
                    Text(
                        "  $key: $value",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
