package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val FREUD_BROWN = Color(0xFF5D4037)
private val FREUD_AMBER = Color(0xFFFFECB3)
private val FREUD_WARM = Color(0xFFFFF8E1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FreudStateBadge(currentState: String, intent: String) {
    val stateColor = when (currentState) {
        "begruessung" -> FREUD_WARM
        "free_association" -> Color(0xFFFFE0B2)
        "interpretation" -> Color(0xFFD7CCC8)
        "dream_analysis" -> Color(0xFFE1BEE7)
        "transference" -> Color(0xFFFFCDD2)
        "abschluss" -> Color(0xFFEEEEEE)
        else -> FREUD_WARM
    }
    Surface(color = stateColor) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
internal fun FreudProfileChips(updates: List<String>) {
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
internal fun FreudChatInput(inputText: String, isLoading: Boolean, onInputChange: (String) -> Unit, onSend: () -> Unit) {
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
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) { onSend(); true } else false
                },
                placeholder = { Text("Tell Dr. Freud what troubles you...") },
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
internal fun FreudMessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(messages) { message -> FreudChatBubble(message) }
        if (isLoading) {
            item {
                Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Dr. Freud is contemplating...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FreudChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else Color(0xFFD7CCC8)
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else FREUD_BROWN

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        ElevatedCard(
            modifier = Modifier.padding(start = if (isUser) 48.dp else 0.dp, end = if (isUser) 0.dp else 48.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (isUser) "You" else "Dr. Freud", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
                    Text(text = if (isUser) message.text else formatAiResponse(message.text), style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
            }
        }
    }
}
