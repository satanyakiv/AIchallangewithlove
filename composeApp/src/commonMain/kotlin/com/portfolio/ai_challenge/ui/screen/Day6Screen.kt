package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
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
fun Day6Screen(onBack: () -> Unit) {
    val viewModel: Day6ViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Day6View(
        state = state,
        listState = listState,
        onBack = onBack,
        onSendMessage = viewModel::sendMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Day6View(
    state: Day6ViewState,
    listState: LazyListState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 6: Agent Smith") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190 Back")
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
                isLoading = state.isLoading,
                onInputChange = { inputText = it },
                onSend = {
                    onSendMessage(inputText)
                    inputText = ""
                },
            )
        },
    ) { padding ->
        if (state.messages.isEmpty()) {
            EmptyChatPlaceholder(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            ChatMessageList(
                messages = state.messages,
                isLoading = state.isLoading,
                listState = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(modifier: Modifier = Modifier) {
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
                "A Koog-powered agent with Agent Smith persona from The Matrix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
