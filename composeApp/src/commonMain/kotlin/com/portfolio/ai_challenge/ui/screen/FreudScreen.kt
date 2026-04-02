package com.portfolio.ai_challenge.ui.screen

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import com.portfolio.ai_challenge.ui.formatAiResponse
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreudScreen(onBack: () -> Unit) {
    val viewModel: FreudViewModel = koinViewModel()
    val sessionId by viewModel.sessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val profileUpdates by viewModel.profileUpdates.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val intent by viewModel.intent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val isFinished = currentState == "abschluss"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dr. Freud's Office") },
                navigationIcon = { TextButton(onClick = onBack) { Text("\u2190 Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5D4037),
                    titleContentColor = Color(0xFFFFECB3),
                ),
            )
        },
        bottomBar = {
            if (sessionId != null) {
                FreudBottomBar(
                    currentState = currentState,
                    intent = intent,
                    profileUpdates = profileUpdates,
                    isLoading = isLoading,
                    isFinished = isFinished,
                    onSend = { viewModel.sendMessage(it) },
                )
            }
        },
    ) { padding ->
        when {
            sessionId == null -> FreudSessionStart(isLoading, error, Modifier.fillMaxSize().padding(padding)) { viewModel.startSession(it) }
            messages.isEmpty() -> FreudEmptyState(Modifier.fillMaxSize().padding(padding))
            else -> FreudMessageList(messages, isLoading, listState, Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun FreudBottomBar(
    currentState: String,
    intent: String,
    profileUpdates: List<String>,
    isLoading: Boolean,
    isFinished: Boolean,
    onSend: (String) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    Column {
        if (isFinished) {
            Surface(color = Color(0xFF5D4037)) {
                Text(
                    "The session has concluded. Auf Wiedersehen.",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFECB3),
                )
            }
        } else {
            FreudChatInput(inputText, isLoading, { inputText = it }) { onSend(inputText); inputText = "" }
        }
    }
}

@Composable
private fun FreudSessionStart(isLoading: Boolean, error: String?, modifier: Modifier, onStart: (String) -> Unit) {
    var userId by remember { mutableStateOf("") }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text("Dr. Freud's Psychoanalytic Office", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF5D4037))
            Text("Free association, dream analysis, defense mechanisms, the unconscious", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("Your name") }, placeholder = { Text("e.g. Anna O.") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true)
            if (error != null) Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = { onStart(userId) }, enabled = userId.isNotBlank() && !isLoading, modifier = Modifier.fillMaxWidth()) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Begin Analysis")
            }
        }
    }
}

@Composable
private fun FreudEmptyState(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Tell me your dreams", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF5D4037))
    }
}
