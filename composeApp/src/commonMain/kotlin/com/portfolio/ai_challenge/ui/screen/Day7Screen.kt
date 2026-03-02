package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.portfolio.ai_challenge.ui.components.ChatInput
import com.portfolio.ai_challenge.ui.components.ChatMessageList
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun Day7Screen(onBack: () -> Unit) {
    val viewModel: Day7ViewModel = koinViewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Day7View(
        messages = messages,
        isLoading = isLoading,
        listState = listState,
        onBack = onBack,
        onSendMessage = viewModel::sendMessage,
        onClearHistory = viewModel::clearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day7View(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    listState: LazyListState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 7: Agent Smith [Memory]") },
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
            ChatInput(
                inputText = inputText,
                isLoading = isLoading,
                onInputChange = { inputText = it },
                onSend = {
                    onSendMessage(inputText)
                    inputText = ""
                },
            )
        },
    ) { padding ->
        if (messages.isEmpty()) {
            Day7EmptyPlaceholder(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            ChatMessageList(
                messages = messages,
                isLoading = isLoading,
                listState = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun Day7EmptyPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Agent Smith",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Persistent memory — Agent Smith remembers everything across restarts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
