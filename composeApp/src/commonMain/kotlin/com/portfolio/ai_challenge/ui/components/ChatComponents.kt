package com.portfolio.ai_challenge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.portfolio.ai_challenge.ui.screen.ChatMessage

@Composable
internal fun ChatInput(
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
                placeholder = { Text("Type a message...") },
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
internal fun ChatMessageList(
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
            ChatBubble(message)
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
internal fun ChatBubble(message: ChatMessage) {
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
