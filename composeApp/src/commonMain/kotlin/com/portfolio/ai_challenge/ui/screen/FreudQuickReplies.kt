package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val quickReplyGroups = listOf(
    "Everyday" to listOf(
        "I've been stressed at work lately",
        "My boss is impossible to deal with",
        "I keep losing my keys and forgetting things",
        "I'm obsessed with keeping everything clean",
        "I can't stop eating when I'm anxious",
    ),
    "Dreams" to listOf(
        "I dreamed I was swimming in a dark ocean",
        "I had a dream about climbing endless stairs",
        "I dreamt about a train entering a tunnel",
        "Last night I dreamed my teeth were falling out",
    ),
    "Resistance" to listOf(
        "That makes absolutely no sense",
        "I think you're completely wrong about this",
        "This is ridiculous, not everything is about my mother",
        "I don't believe in any of this",
    ),
    "Meta" to listOf(
        "Why do you keep mentioning my mother?",
        "Is that cigar a phallic symbol?",
        "Tell me about your cocaine research",
        "What would Jung say about this?",
    ),
)

@Composable
internal fun FreudQuickReplies(isLoading: Boolean, onSend: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        quickReplyGroups.forEach { (label, phrases) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 16.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
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
        }
    }
}
