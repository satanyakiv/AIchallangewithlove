package com.portfolio.ai_challenge.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challenge.data.MemoryLayersDebug
import com.portfolio.ai_challenge.ui.formatAiResponse
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun Day11Screen(onBack: () -> Unit) {
    val viewModel: Day11ViewModel = koinViewModel()
    val sessionId by viewModel.sessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val memoryDebug by viewModel.memoryDebug.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Day11View(
        sessionId = sessionId,
        messages = messages,
        memoryDebug = memoryDebug,
        isLoading = isLoading,
        error = error,
        listState = listState,
        onBack = onBack,
        onStartSession = viewModel::startSession,
        onSendMessage = viewModel::sendMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day11View(
    sessionId: String?,
    messages: List<ChatMessage>,
    memoryDebug: MemoryLayersDebug?,
    isLoading: Boolean,
    error: String?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onBack: () -> Unit,
    onStartSession: (String) -> Unit,
    onSendMessage: (String) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 11: Psy-Agent") },
                navigationIcon = { TextButton(onClick = onBack) { Text("\u2190 Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = {
            if (sessionId != null) {
                Column {
                    if (memoryDebug != null) {
                        MemoryDebugCard(memoryDebug = memoryDebug)
                    }
                    QuickReplyChips(isLoading = isLoading, onSend = onSendMessage)
                    PsyChatInput(
                        inputText = inputText,
                        isLoading = isLoading,
                        onInputChange = { inputText = it },
                        onSend = { onSendMessage(inputText); inputText = "" },
                    )
                }
            }
        },
    ) { padding ->
        if (sessionId == null) {
            SessionStartScreen(
                isLoading = isLoading,
                error = error,
                modifier = Modifier.fillMaxSize().padding(padding),
                onStart = onStartSession,
            )
        } else if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Text("\uD83E\uDDE0 MindGuard", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Your compassionate mental health support AI.\nTell me what's on your mind.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            PsyChatMessageList(
                messages = messages,
                isLoading = isLoading,
                listState = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun SessionStartScreen(
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    onStart: (String) -> Unit,
) {
    var userId by remember { mutableStateOf("") }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                "\uD83E\uDDE0 MindGuard — 3-Layer Memory",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Layer 1: Turn (ephemeral)\nLayer 2: Session (in-memory)\nLayer 3: Profile (persistent)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("Your name or ID") },
                placeholder = { Text("e.g. alice") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
            )
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { onStart(userId) },
                enabled = userId.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Start Session")
                }
            }
        }
    }
}

@Composable
private fun MemoryDebugCard(memoryDebug: MemoryLayersDebug) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Turn", "Session", "Profile")

    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "\uD83D\uDDC4 Memory Debug",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    @OptIn(ExperimentalMaterial3Api::class)
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    val content = when (selectedTab) {
                        0 -> memoryDebug.turn
                        1 -> memoryDebug.session
                        else -> memoryDebug.profile
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickReplyChips(isLoading: Boolean, onSend: (String) -> Unit) {
    val phrases = listOf(
        "My name is Katya", "Call me Kolian",
        "I feel really anxious lately", "I cant sleep at night", "I feel sad and hopeless",
        "Work deadlines stress me out", "Having issues with my partner", "I feel so lonely and isolated",
        "Tell me more about that technique", "Yes, lets try it", "I feel better now, thank you",
        "I want to end the session",
        "hi", "Do I have depression?", "What medication should I take?",
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(phrases) { phrase ->
            SuggestionChip(
                onClick = { onSend(phrase) },
                label = { Text(phrase, style = MaterialTheme.typography.labelSmall) },
                enabled = !isLoading,
            )
        }
    }
}

@Composable
private fun PsyChatInput(
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
                placeholder = { Text("Share what's on your mind...") },
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
private fun PsyChatMessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages) { message ->
            PsyChatBubble(message)
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
                        "MindGuard is thinking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PsyChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        ElevatedCard(
            modifier = Modifier.padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 48.dp,
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isUser) "You" else "\uD83E\uDDE0 MindGuard",
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
}
