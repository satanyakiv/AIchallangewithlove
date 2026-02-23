package com.portfolio.ai_challange_with_love.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challange_with_love.model.ChallengeDay
import com.portfolio.ai_challange_with_love.ui.components.ChallengeCard

private val challengeDays = listOf(
    ChallengeDay(
        id = 4,
        dayNumber = 4,
        title = "Temperature",
        description = "Experimenting with temperature: 0, 0.7, 1.2 — comparing accuracy, creativity and diversity",
        emoji = "\uD83C\uDF21\uFE0F",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onDayClick: (Int) -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Challenge with Love") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(challengeDays, key = { it.id }) { day ->
                ChallengeCard(day = day, onClick = { onDayClick(day.id) })
            }
        }
    }
}
