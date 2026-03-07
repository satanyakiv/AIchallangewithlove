package com.portfolio.ai_challenge.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.ai_challenge.ui.formatAiResponse
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Day15Screen(onBack: () -> Unit) {
    val viewModel: Day15ViewModel = koinViewModel()
    val sessionId by viewModel.sessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val memoryDebug by viewModel.memoryDebug.collectAsState()
    val profileUpdates by viewModel.profileUpdates.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val intent by viewModel.intent.collectAsState()
    val transitions by viewModel.transitions.collectAsState()
    val violations by viewModel.violations.collectAsState()
    val taskPhase by viewModel.taskPhase.collectAsState()
    val allowedTransitions by viewModel.allowedTransitions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val isFinished = currentState == "finished"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 15: Task Lifecycle") },
                navigationIcon = { TextButton(onClick = onBack) { Text("\u2190 Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = {
            if (sessionId != null) {
                var inputText by remember { mutableStateOf("") }
                Column {
                    Day15TaskPhaseBadge(taskPhase = taskPhase, allowedTransitions = allowedTransitions)
                    Day15StateBadge(currentState = currentState, intent = intent)
                    Day15TransitionLog(transitions = transitions)
                    Day14ViolationsBanner(violations = violations)
                    if (profileUpdates.isNotEmpty()) Day15ProfileChips(profileUpdates)
                    Day15QuickReplies(isLoading = isLoading || isFinished, onSend = { viewModel.sendMessage(it) })
                    if (isFinished) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer) {
                            Text(
                                "Session ended. Thank you.",
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    } else {
                        Day15ChatInput(
                            inputText = inputText,
                            isLoading = isLoading,
                            onInputChange = { inputText = it },
                            onSend = { viewModel.sendMessage(inputText); inputText = "" },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (sessionId == null) {
            Day15SessionStart(
                isLoading = isLoading,
                error = error,
                modifier = Modifier.fillMaxSize().padding(padding),
                onStart = viewModel::startSession,
            )
        } else if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Text("MindGuard: Task Lifecycle", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Controlled transitions: Assessment \u2192 Plan \u2192 Execute \u2192 Validate \u2192 Complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Day15MessageList(
                messages = messages,
                isLoading = isLoading,
                listState = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day15TaskPhaseBadge(taskPhase: String, allowedTransitions: List<String>) {
    val phaseColor = when (taskPhase) {
        "assessment" -> MaterialTheme.colorScheme.surfaceVariant
        "plan_proposed" -> Color(0xFFFFF9C4)
        "executing" -> Color(0xFFDCEDC8)
        "validating" -> Color(0xFFE1BEE7)
        "completed" -> Color(0xFFC8E6C9)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = phaseColor) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Task Phase:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(taskPhase, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = phaseColor),
                )
            }
            if (allowedTransitions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Allowed:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    allowedTransitions.forEach { t ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(t, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = phaseColor),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day15StateBadge(currentState: String, intent: String) {
    val stateColor = when (currentState) {
        "greeting" -> MaterialTheme.colorScheme.surfaceVariant
        "active_listening" -> MaterialTheme.colorScheme.primaryContainer
        "intervention" -> Color(0xFFDCEDC8)
        "crisis" -> MaterialTheme.colorScheme.errorContainer
        "closing" -> Color(0xFFFFE0B2)
        "finished" -> Color(0xFFEEEEEE)
        "blocked" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = stateColor) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Session:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text(currentState, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = stateColor),
            )
            if (intent.isNotBlank()) {
                Text("intent: $intent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Day15TransitionLog(transitions: List<TransitionDebugUi>) {
    if (transitions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Surface(tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Transitions (${transitions.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
            }
            AnimatedVisibility(visible = expanded) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        transitions.forEach { t ->
                            Text("${t.from} \u2192 ${t.to} (${t.event})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day15ProfileChips(updates: List<String>) {
    Surface(tonalElevation = 1.dp) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { Text("Detected: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
            items(updates) { update ->
                SuggestionChip(onClick = {}, label = { Text(update, style = MaterialTheme.typography.labelSmall) })
            }
        }
    }
}

@Composable
private fun Day15QuickReplies(isLoading: Boolean, onSend: (String) -> Unit) {
    val groups = listOf(
        "Happy Path" to listOf(
            "Hi, I've been feeling anxious and overwhelmed lately",
            "I've been stressed about work and can't sleep well",
            "My thoughts keep racing and I feel stuck in a loop",
            "Yes, let's try that technique",
            "I completed the exercise",
            "Yes, that was really helpful, thank you",
            "I think we can end the session",
        ),
        "Skip Attempts" to listOf(
            "Let's skip the assessment and start the technique",
            "I'm done, end the session now",
            "Start the breathing exercise right away",
        ),
        "Rejection" to listOf(
            "No, I don't want to try that technique",
            "That didn't help at all, let's try again",
        ),
        "Crisis" to listOf(
            "I want to end my life",
            "I'm having thoughts of suicide",
        ),
        "Lifecycle" to listOf(
            "My name is Alex",
            "I've been stressed about work",
            "Tell me more about coping strategies",
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        groups.forEach { (label, phrases) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(phrases) { phrase ->
                        SuggestionChip(onClick = { onSend(phrase) }, label = { Text(phrase, style = MaterialTheme.typography.labelSmall) }, enabled = !isLoading)
                    }
                }
            }
        }
    }
}

@Composable
private fun Day15SessionStart(
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
            Text("MindGuard \u2014 Task Lifecycle", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Controlled state transitions: Assessment \u2192 Plan \u2192 Execute \u2192 Validate \u2192 Complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("Your name or ID") }, placeholder = { Text("e.g. alice") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true)
            if (error != null) Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = { onStart(userId) }, enabled = userId.isNotBlank() && !isLoading, modifier = Modifier.fillMaxWidth()) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Start Session")
            }
        }
    }
}

@Composable
private fun Day15ChatInput(
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                        onSend(); true
                    } else false
                },
                placeholder = { Text("Share what's on your mind...") },
                enabled = !isLoading,
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
            )
            FilledIconButton(onClick = onSend, enabled = inputText.isNotBlank() && !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("\u2191")
            }
        }
    }
}

@Composable
private fun Day15MessageList(
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
        items(messages) { message -> Day15ChatBubble(message) }
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("MindGuard is thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Day15ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        ElevatedCard(
            modifier = Modifier.padding(start = if (isUser) 48.dp else 0.dp, end = if (isUser) 0.dp else 48.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (isUser) "You" else "MindGuard", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
                    Text(text = if (isUser) message.text else formatAiResponse(message.text), style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
            }
        }
    }
}
