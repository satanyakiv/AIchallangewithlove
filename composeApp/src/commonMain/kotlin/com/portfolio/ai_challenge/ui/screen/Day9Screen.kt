package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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

// Demo prompts — each generates a meaty response (~300-500 tokens),
// so accumulated prompt tokens hit TOKEN_THRESHOLD after 2 exchanges.
private val DEMO_PROMPTS = listOf(
    "Explain how the TCP/IP stack works — all 4 layers, their roles and what happens at each step when you open a website",
    "Describe all 5 SOLID principles with a concrete code example for each one",
    "What are qubits, superposition and entanglement in quantum computing and why do they matter?",
    "Tell me about the Unix philosophy, its history and how it shaped modern operating systems",
)

@Composable
fun Day9Screen(onBack: () -> Unit) {
    val viewModel: Day9ViewModel = koinViewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val compressionEnabled by viewModel.compressionEnabled.collectAsState()
    val tokenStats by viewModel.tokenStats.collectAsState()
    val currentSummary by viewModel.currentSummary.collectAsState()
    val cumulativeTokens by viewModel.cumulativePromptTokens.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Day9View(
        messages = messages,
        isLoading = isLoading,
        compressionEnabled = compressionEnabled,
        tokenStats = tokenStats,
        currentSummary = currentSummary,
        cumulativeTokens = cumulativeTokens,
        listState = listState,
        onBack = onBack,
        onSendMessage = viewModel::sendMessage,
        onToggleCompression = viewModel::toggleCompression,
        onClearHistory = viewModel::clearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day9View(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    compressionEnabled: Boolean,
    tokenStats: TokenStats?,
    currentSummary: String?,
    cumulativeTokens: Int,
    listState: LazyListState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleCompression: () -> Unit,
    onClearHistory: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 9: Context Compression") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
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
                Day9StatsBar(
                    compressionEnabled = compressionEnabled,
                    tokenStats = tokenStats,
                    currentSummary = currentSummary,
                    cumulativeTokens = cumulativeTokens,
                    onToggleCompression = onToggleCompression,
                )
                // Demo suggestion chips — visible only when input is empty
                if (inputText.isEmpty()) {
                    Day9SuggestionChips(
                        onSuggestionClick = { inputText = it },
                    )
                }
                Day9ChatInput(
                    inputText = inputText,
                    isLoading = isLoading,
                    onInputChange = { inputText = it },
                    onSend = {
                        onSendMessage(inputText)
                        inputText = ""
                    },
                )
            }
        },
    ) { padding ->
        if (messages.isEmpty()) {
            Day9EmptyPlaceholder(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            Day9ChatMessageList(
                messages = messages,
                isLoading = isLoading,
                listState = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun Day9StatsBar(
    compressionEnabled: Boolean,
    tokenStats: TokenStats?,
    currentSummary: String?,
    cumulativeTokens: Int,
    onToggleCompression: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Compression toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "History Compression",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (compressionEnabled) {
                            "ON — compresses when accumulated prompt tokens > $TOKEN_THRESHOLD"
                        } else {
                            "OFF — full history sent every time"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = compressionEnabled,
                    onCheckedChange = { onToggleCompression() },
                )
            }

            // Token accumulation progress bar (shown when compression is ON)
            if (compressionEnabled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                val progress = (cumulativeTokens.toFloat() / TOKEN_THRESHOLD).coerceIn(0f, 1f)
                val willCompressNext = cumulativeTokens >= TOKEN_THRESHOLD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Accumulated prompt tokens:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$cumulativeTokens / $TOKEN_THRESHOLD",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (willCompressNext) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = if (willCompressNext) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                if (willCompressNext) {
                    Text(
                        text = "\u26A1 Next message will trigger compression!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // Last request token stats
            if (tokenStats != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (tokenStats.wasCompressed) "\uD83D\uDDDC Compressed" else "\uD83D\uDCE4 Full history",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tokenStats.wasCompressed) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "prompt: ${tokenStats.promptTokens}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "total: ${tokenStats.totalTokens}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Summary preview
            if (currentSummary != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text(
                    text = "\uD83D\uDCDD Compressed context summary:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = currentSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Day9SuggestionChips(
    onSuggestionClick: (String) -> Unit,
) {
    Surface(tonalElevation = 1.dp) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DEMO_PROMPTS.forEachIndexed { index, prompt ->
                val shortLabel = when (index) {
                    0 -> "TCP/IP stack"
                    1 -> "SOLID principles"
                    2 -> "Quantum computing"
                    else -> "Unix philosophy"
                }
                FilterChip(
                    selected = false,
                    onClick = { onSuggestionClick(prompt) },
                    label = { Text(shortLabel, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun Day9ChatInput(
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter
                            && event.type == KeyEventType.KeyDown
                            && !event.isShiftPressed
                        ) {
                            onSend()
                            true
                        } else {
                            false
                        }
                    },
                placeholder = { Text("Type a message or pick a demo prompt above...") },
                enabled = !isLoading,
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
            )
            FilledIconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("\u2191")
                }
            }
        }
    }
}

@Composable
private fun Day9EmptyPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                "\uD83E\uDDE0 Context Compression",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "1. Enable compression toggle\n" +
                    "2. Pick demo prompts below (they generate long responses)\n" +
                    "3. After ~2 exchanges the token bar fills → next message compresses history",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Day9ChatMessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages) { message ->
            Day9ChatBubble(message)
        }
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "Agent is thinking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Day9ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        ElevatedCard(
            modifier = Modifier.widthIn(max = 600.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "You" else "Agent",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                )
                Text(
                    text = if (isUser) message.text else formatAiResponse(message.text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}
