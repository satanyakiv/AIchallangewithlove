package com.portfolio.ai_challenge.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.portfolio.ai_challenge.database.Day10BranchEntity
import org.koin.compose.viewmodel.koinViewModel

private val DEMO_PROMPTS = listOf(
    "I want to build a task management app for small teams",
    "Target platform: iOS and Android. Budget: \$80k",
    "Deadline is end of Q2 2026. Team: 2 devs, 1 designer",
    "Key features: task assignment, deadlines, notifications",
    "Users are project managers at mid-size companies",
)

@Composable
fun Day10BranchingScreen(onBack: () -> Unit) {
    val viewModel: Day10BranchingViewModel = koinViewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val currentBranchId by viewModel.currentBranchId.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    BranchingView(
        messages = messages,
        isLoading = isLoading,
        branches = branches,
        currentBranchId = currentBranchId,
        stats = stats,
        listState = listState,
        onBack = onBack,
        onSend = viewModel::sendMessage,
        onSwitchBranch = viewModel::switchBranch,
        onForkBranch = viewModel::forkBranch,
        onClear = viewModel::clearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchingView(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    branches: List<Day10BranchEntity>,
    currentBranchId: String,
    stats: BranchingStats?,
    listState: LazyListState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onSwitchBranch: (String) -> Unit,
    onForkBranch: (String) -> Unit,
    onClear: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var showForkDialog by remember { mutableStateOf(false) }
    var forkName by remember { mutableStateOf("") }

    if (showForkDialog) {
        AlertDialog(
            onDismissRequest = { showForkDialog = false },
            title = { Text("Fork Branch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a new branch from current checkpoint (last main message).")
                    OutlinedTextField(
                        value = forkName,
                        onValueChange = { forkName = it },
                        label = { Text("Branch name") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (forkName.isNotBlank()) {
                            onForkBranch(forkName.trim())
                            forkName = ""
                            showForkDialog = false
                        }
                    }
                ) { Text("Fork") }
            },
            dismissButton = {
                TextButton(onClick = { showForkDialog = false; forkName = "" }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 10c: Branching") },
                navigationIcon = { TextButton(onClick = onBack) { Text("\u2190 Back") } },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClear) {
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
                BranchingStatsBar(
                    branches = branches,
                    currentBranchId = currentBranchId,
                    stats = stats,
                    onSwitchBranch = onSwitchBranch,
                    onShowForkDialog = { showForkDialog = true },
                )
                if (inputText.isEmpty()) {
                    SuggestionChipsRow(prompts = DEMO_PROMPTS, onSuggestionClick = { inputText = it })
                }
                ChatInputBar(
                    inputText = inputText,
                    isLoading = isLoading,
                    onInputChange = { inputText = it },
                    onSend = { onSend(inputText); inputText = "" },
                )
            }
        },
    ) { padding ->
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Text("\uD83D\uDD00 Branching", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Chat on the main branch, then fork at any point.\nEach branch remembers shared history up to the fork.\nSwitch between branches to explore different directions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Day10ChatMessageList(messages = messages, isLoading = isLoading, listState = listState, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun BranchingStatsBar(
    branches: List<Day10BranchEntity>,
    currentBranchId: String,
    stats: BranchingStats?,
    onSwitchBranch: (String) -> Unit,
    onShowForkDialog: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Branch: ${if (currentBranchId == "main") "main" else branches.find { it.id == currentBranchId }?.name ?: currentBranchId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onShowForkDialog) {
                    Text("\uD83D\uDD00 Fork here", style = MaterialTheme.typography.labelSmall)
                }
            }
            // Branch chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = currentBranchId == "main",
                    onClick = { onSwitchBranch("main") },
                    label = { Text("main", style = MaterialTheme.typography.labelSmall) },
                )
                branches.forEach { branch ->
                    FilterChip(
                        selected = currentBranchId == branch.id,
                        onClick = { onSwitchBranch(branch.id) },
                        label = { Text(branch.name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            if (stats != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Prompt tokens: ${stats.promptTokens}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Branch depth: ${stats.branchDepth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
