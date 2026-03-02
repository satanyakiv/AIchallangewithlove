package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challenge.ui.formatAiResponse
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private val DEMO_PROMPTS = listOf(
    "I want to build a task management app for small teams",
    "Target platform: iOS and Android. Budget: \$80k",
    "Deadline is end of Q2 2026. Team: 2 devs, 1 designer",
    "Key features: task assignment, deadlines, notifications",
    "Users are project managers at mid-size companies",
)

@Composable
fun Day10SlidingScreen(onBack: () -> Unit) {
    val viewModel: Day10SlidingViewModel = koinViewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val windowSize by viewModel.windowSize.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    SlidingView(
        messages = messages,
        isLoading = isLoading,
        windowSize = windowSize,
        stats = stats,
        listState = listState,
        onBack = onBack,
        onSend = viewModel::sendMessage,
        onWindowSizeChange = viewModel::setWindowSize,
        onClear = viewModel::clearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlidingView(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    windowSize: Int,
    stats: SlidingStats?,
    listState: LazyListState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onWindowSizeChange: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 10a: Sliding Window") },
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
                SlidingStatsBar(windowSize = windowSize, stats = stats, totalMessages = messages.size, onWindowSizeChange = onWindowSizeChange)
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
                    Text("\uD83E\uDE9F Sliding Window", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Only the last N messages are sent to the API.\nAdjust window size with the slider above.\nUse demo prompts below to see dropped message count grow.",
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
private fun SlidingStatsBar(
    windowSize: Int,
    stats: SlidingStats?,
    totalMessages: Int,
    onWindowSizeChange: (Int) -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Window size: N=$windowSize", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Total messages: $totalMessages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = windowSize.toFloat(),
                onValueChange = { onWindowSizeChange(it.roundToInt()) },
                valueRange = 3f..20f,
                steps = 16,
                modifier = Modifier.fillMaxWidth(),
            )
            if (stats != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Kept: ${stats.windowedCount}  |  Dropped: ${stats.droppedCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stats.droppedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Prompt tokens: ${stats.promptTokens}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
