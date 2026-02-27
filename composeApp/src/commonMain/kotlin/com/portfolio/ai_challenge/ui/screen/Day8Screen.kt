package com.portfolio.ai_challenge.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Palette ──────────────────────────────────────────────────────────────────
private val ColorCorrect   = Color(0xFF4CAF50)
private val ColorPartial   = Color(0xFFFFC107)
private val ColorPass      = Color(0xFF2196F3)
private val ColorNotTested = Color(0xFF424242)
private val ColorBg        = Color(0xFF0D1117)
private val ColorCard      = Color(0xFF161B22)
private val ColorBorder    = Color(0xFF30363D)
private val ColorMono      = Color(0xFF58A6FF)
private val ColorDimText   = Color(0xFF8B949E)
private val ColorBodyText  = Color(0xFFCDD9E5)

// ── Data models ───────────────────────────────────────────────────────────────
private data class TokenPoint(val step: Int, val tokens: Int)
private data class DilutionCheck(val id: String, val tokens: Int)
private data class RunSummary(
    val name: String,
    val peakTokens: String,
    val dilutionScore: String,
    val badge: String,
    val correct: Boolean,
    val description: String,
)
private data class HeatmapRowData(
    val mode: String,
    val case1: CellResult,
    val case2: CellResult,
    val case3: CellResult,
)
private enum class CellResult { PASSED, PARTIAL, NOT_TESTED, SPECIAL_PASS }
private data class Insight(val title: String, val summary: String, val detail: String)

// ── Static data ───────────────────────────────────────────────────────────────
private val tokenGrowthData = listOf(
    TokenPoint(1, 783), TokenPoint(25, 11893), TokenPoint(50, 23295),
    TokenPoint(75, 34697), TokenPoint(100, 46099), TokenPoint(125, 57501),
    TokenPoint(150, 68903), TokenPoint(175, 80305), TokenPoint(200, 91707),
    TokenPoint(225, 103109), TokenPoint(250, 114511), TokenPoint(272, 125146),
    TokenPoint(280, 125800), TokenPoint(295, 126705),
)

private val dilutionChecks = listOf(
    DilutionCheck("C2-1", 2218), DilutionCheck("C2-3", 4877),
    DilutionCheck("C3-1", 7968), DilutionCheck("C3-3", 9221),
    DilutionCheck("C3-5", 10172), DilutionCheck("C3-7", 11395),
    DilutionCheck("V2-4", 125552), DilutionCheck("V3-4", 126549),
)

private val runsData = listOf(
    RunSummary("Run 1", "20K", "0/5", "DESIGN FLAW", false, "Study 1 never in context"),
    RunSummary("Run 2", "70K", "8/8", "VALIDATED", true, "Fixed: continuous conversation"),
    RunSummary("Run 3", "126K", "8/8", "CONFIRMED", true, "Final: 10 rounds, 300s timeout"),
)

private val heatmapData = listOf(
    HeatmapRowData("Attention Dilution",  CellResult.NOT_TESTED, CellResult.PASSED,       CellResult.PASSED),
    HeatmapRowData("Confabulation",       CellResult.NOT_TESTED, CellResult.PASSED,       CellResult.PASSED),
    HeatmapRowData("Cross-contamination", CellResult.NOT_TESTED, CellResult.PASSED,       CellResult.PARTIAL),
    HeatmapRowData("Source Attribution",  CellResult.PASSED,     CellResult.PASSED,       CellResult.PASSED),
    HeatmapRowData("Recency Bias",        CellResult.NOT_TESTED, CellResult.PASSED,       CellResult.PARTIAL),
    HeatmapRowData("Attention Sink",      CellResult.SPECIAL_PASS, CellResult.SPECIAL_PASS, CellResult.SPECIAL_PASS),
    HeatmapRowData("Lost in the Middle",  CellResult.NOT_TESTED, CellResult.PASSED,       CellResult.PASSED),
)

private val insightsData = listOf(
    Insight(
        title = "Context correctness > context length",
        summary = "What's IN context matters more than HOW MUCH context there is.",
        detail = "Original case_3 hallucinated because Study 1 wasn't in context. Fixed case_3 recalled it perfectly at 126K. The model's accuracy depends on WHAT's in context, not HOW MUCH. Fixing the design flipped the result from 0/5 HALLUCINATED to 8/8 CORRECT.",
    ),
    Insight(
        title = "23/25 studies enumerated at 126K",
        summary = "Predicted as \"near-impossible\" — actually near-perfect.",
        detail = "The model listed all 25 studies with correct authors, years, and sample sizes from a 126K-token conversation spanning 10 repetition rounds. Only 2 misses: secondary control group counts (N=48+30 and N=94+50). All primary sample sizes and author names were correct.",
    ),
    Insight(
        title = "Recency bias is local, not global",
        summary = "Bias appears only in the last few messages, not across the full context.",
        detail = "At 10K tokens (right after loading Study 18), the model cited Study 18 as the most important finding. At 125K tokens (same question asked again), it cited Study 1. The recency effect fades as context grows — the model doesn't persistently favor the most recent study.",
    ),
    Insight(
        title = "Detail omissions are consistent",
        summary = "The model drops secondary stats regardless of context depth.",
        detail = "V2-2 and V2-3 were PARTIAL at both 7K tokens (standalone Case 2) and 125K tokens (Case 3 fixed). The model consistently omits secondary counts (n=79, d=0.78, PET subsample) at all context sizes. This is selective encoding — not degradation caused by long context.",
    ),
)

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Day8Screen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day 8 — Context Stress Test") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorCard,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = ColorBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroCard()
            MethodologyCard()
            JourneySection()
            TokenChartSection()
            AttentionDilutionSection()
            FailureModeHeatmapSection()
            InsightsSection()
            DesignNoteCard()
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────
@Composable
private fun HeroCard() {
    DarkCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "126,778 tokens.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ColorMono,
                    fontSize = 28.sp,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                "Zero failures.",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "DeepSeek V3 context stress test — 295 API calls, 25 studies, 10 repetition rounds",
                style = MaterialTheme.typography.bodyMedium.copy(color = ColorDimText),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatPill("126K", "peak tokens")
                StatPill("23/25", "studies recalled")
                StatPill("8/8", "dilution checks")
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF21262D))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ColorMono,
            ),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = ColorDimText),
        )
    }
}

// ── Methodology ───────────────────────────────────────────────────────────────
@Composable
private fun MethodologyCard() {
    var expanded by remember { mutableStateOf(false) }
    DarkCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "How this experiment works",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
            )
            Text(
                if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.bodyMedium.copy(color = ColorDimText),
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MethodSection(
                    "Test Content",
                    "25 fictional medical studies about atomoxetine (ADHD medication) were generated as realistic research summaries. Each study has a unique lead author, sample size (N), methodology, and specific numerical findings. Example: \"Study 1 — Zhang et al. (2019), N=342, AISRS reduction −16.4, response rate 67.8%\". The content is dense with numbers, names, and cross-references — designed to stress-test factual recall.",
                )
                MethodSection(
                    "3 Test Cases, escalating context",
                    "• Case 1 (~2K tokens): 3 articles + 4 verifications. Baseline.\n• Case 2 (~27K tokens): Studies 1–10 fed sequentially with 3 checkpoints + 7 verifications.\n• Case 3 (~126K tokens): Continues Case 2's conversation. Studies 11–25 added, then ALL 25 studies repeated 10× to push to 126K. Verifications asked at peak.",
                )
                MethodSection(
                    "7 Failure Modes Tested",
                    "1. Attention Dilution — same question (\"N in Study 1?\") asked 8× at 2K→126K\n2. Confabulation — trap questions about things that don't exist\n3. Cross-contamination — trick with two correct answers\n4. Source Attribution — assign findings to correct researcher\n5. Recency Bias — does model favor recently-loaded studies?\n6. Attention Sink — system prompt rule tested at 2K, 27K, 126K\n7. Lost in the Middle — recall from mid-context studies",
                )
                MethodSection(
                    "Execution",
                    "JUnit test in Kotlin Multiplatform. Day8ExperimentRunner maintains conversation history, sends messages to DeepSeek via Ktor HTTP client, records TokenUsage per step. delay(1000) between calls. Temperature 0.3. Total runtime: ~90 minutes, 295 API calls. Three runs — the first had a design flaw, runs 2 and 3 were corrected.",
                )
            }
        }
    }
}

@Composable
private fun MethodSection(title: String, body: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = ColorMono,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall.copy(color = ColorBodyText, lineHeight = 20.sp),
        )
    }
}

// ── Journey: 3 runs ───────────────────────────────────────────────────────────
@Composable
private fun JourneySection() {
    SectionLabel("The Journey — 3 Runs")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        runsData.forEach { RunCard(it) }
    }
}

@Composable
private fun RunCard(run: RunSummary) {
    val badgeColor = if (run.correct) ColorCorrect else Color(0xFFF44336)
    Column(
        modifier = Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCard)
            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                run.badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(run.name, style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Text(
            run.peakTokens + " tokens",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                color = ColorMono,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "N=342: ${run.dilutionScore} ${if (run.correct) "✓" else "✗"}",
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (run.correct) ColorCorrect else Color(0xFFF44336),
                fontFamily = FontFamily.Monospace,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(run.description, style = MaterialTheme.typography.bodySmall.copy(color = ColorDimText))
    }
}

// ── Token chart ───────────────────────────────────────────────────────────────
@Composable
private fun TokenChartSection() {
    SectionLabel("Token Growth — 295 Steps")
    DarkCard {
        Text(
            "prompt_tokens per API call — monotonic growth, no plateau, no truncation",
            style = MaterialTheme.typography.bodySmall.copy(color = ColorDimText),
        )
        Spacer(Modifier.height(12.dp))
        TokenLineChart(
            data = tokenGrowthData,
            verificationStartStep = 273,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(ColorMono, "Token growth")
            LegendDot(ColorPartial, "Verification zone (steps 273–295)")
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("0", "50", "100", "150", "200", "250", "295").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall.copy(color = ColorDimText, fontFamily = FontFamily.Monospace))
            }
        }
        Text(
            "step →",
            style = MaterialTheme.typography.labelSmall.copy(color = ColorDimText),
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun TokenLineChart(
    data: List<TokenPoint>,
    verificationStartStep: Int,
    modifier: Modifier = Modifier,
) {
    val lineColor = ColorMono
    val verifyColor = ColorPartial
    val gridColor = Color(0xFF21262D)
    val maxStep = 295
    val maxTokensF = 130_000f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padLeft = 40.dp.toPx()
        val chartW = w - padLeft
        val chartH = h

        fun stepToX(step: Int) = padLeft + (step.toFloat() / maxStep) * chartW
        fun tokensToY(tokens: Int) = chartH - (tokens.toFloat() / maxTokensF) * chartH

        // Horizontal grid lines + Y labels baked into grid
        listOf(0, 25_000, 50_000, 75_000, 100_000, 125_000).forEach { t ->
            val y = tokensToY(t)
            drawLine(gridColor, Offset(padLeft, y), Offset(w, y), strokeWidth = 1f)
        }

        // Vertical grid
        listOf(0, 50, 100, 150, 200, 250, 295).forEach { s ->
            drawLine(gridColor, Offset(stepToX(s), 0f), Offset(stepToX(s), chartH), strokeWidth = 1f)
        }

        // Verification zone
        val vx = stepToX(verificationStartStep)
        drawRect(color = verifyColor.copy(alpha = 0.1f), topLeft = Offset(vx, 0f), size = Size(w - vx, chartH))
        drawLine(verifyColor.copy(alpha = 0.6f), Offset(vx, 0f), Offset(vx, chartH), strokeWidth = 1.5f)

        // Data path
        val path = Path()
        data.forEachIndexed { i, p ->
            val x = stepToX(p.step); val y = tokensToY(p.tokens)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Dots at data points
        data.forEach { p ->
            drawCircle(lineColor, radius = 3.5f, center = Offset(stepToX(p.step), tokensToY(p.tokens)))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(width = 12.dp, height = 3.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = ColorDimText))
    }
}

// ── Attention Dilution ────────────────────────────────────────────────────────
@Composable
private fun AttentionDilutionSection() {
    SectionLabel("Attention Dilution — Same Question, 8 Answers")
    DarkCard {
        Text(
            "\"What was the sample size in Study 1 (Zhang et al.)?\"",
            style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
        )
        Text(
            "Expected: N=342 — asked 8 times from 2K to 126K tokens",
            style = MaterialTheme.typography.bodySmall.copy(color = ColorDimText),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            dilutionChecks.forEach { DilutionCircle(it) }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF44336).copy(alpha = 0.08f))
                .border(1.dp, Color(0xFFF44336).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            Text(
                "Original run (broken): 5/5 HALLUCINATED — answered \"N=536\" every time, because Study 1 was never in context",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF44336)),
            )
        }
    }
}

@Composable
private fun DilutionCircle(check: DilutionCheck) {
    val label = when {
        check.tokens >= 100_000 -> "${check.tokens / 1000}K"
        check.tokens >= 10_000  -> "${check.tokens / 1000}.${(check.tokens % 1000) / 100}K"
        else                    -> "${check.tokens / 1000}.${(check.tokens % 1000) / 100}K"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(ColorCorrect.copy(alpha = 0.12f))
                .border(2.dp, ColorCorrect, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = ColorCorrect, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            check.id,
            style = MaterialTheme.typography.labelSmall.copy(color = ColorDimText, fontSize = 9.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = ColorMono,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Heatmap ───────────────────────────────────────────────────────────────────
@Composable
private fun FailureModeHeatmapSection() {
    SectionLabel("Failure Mode Heatmap")
    DarkCard {
        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("", modifier = Modifier.weight(1.6f))
            listOf("Case 1\n~2K", "Case 2\n~27K", "Case 3\n~126K").forEach { h ->
                Text(
                    h,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(color = ColorDimText),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        heatmapData.forEach { row ->
            HeatmapRowItem(row)
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            HeatLegend(ColorCorrect, "Passed")
            HeatLegend(ColorPartial, "Partial")
            HeatLegend(ColorPass, "Sink pass")
            HeatLegend(ColorNotTested, "—")
        }
    }
}

@Composable
private fun HeatmapRowItem(row: HeatmapRowData) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            row.mode,
            modifier = Modifier.weight(1.6f),
            style = MaterialTheme.typography.bodySmall.copy(color = ColorBodyText),
        )
        listOf(row.case1, row.case2, row.case3).forEach { cell ->
            val (bg, symbol, fg) = cellStyle(cell)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(symbol, color = fg, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

private data class CellStyle(val bg: Color, val symbol: String, val fg: Color)

private fun cellStyle(cell: CellResult) = when (cell) {
    CellResult.PASSED       -> CellStyle(ColorCorrect.copy(alpha = 0.18f), "✓", ColorCorrect)
    CellResult.PARTIAL      -> CellStyle(ColorPartial.copy(alpha = 0.18f), "~", ColorPartial)
    CellResult.SPECIAL_PASS -> CellStyle(ColorPass.copy(alpha = 0.18f), "●", ColorPass)
    CellResult.NOT_TESTED   -> CellStyle(ColorNotTested.copy(alpha = 0.35f), "—", ColorDimText)
}

@Composable
private fun HeatLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.8f)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = ColorDimText))
    }
}

// ── Insights ──────────────────────────────────────────────────────────────────
@Composable
private fun InsightsSection() {
    SectionLabel("Key Insights")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        insightsData.forEach { InsightCardItem(it) }
    }
}

@Composable
private fun InsightCardItem(insight: Insight) {
    var expanded by remember { mutableStateOf(false) }
    DarkCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    insight.title,
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                )
                Text(
                    insight.summary,
                    style = MaterialTheme.typography.bodySmall.copy(color = ColorDimText),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.bodyMedium.copy(color = ColorDimText),
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Text(
                insight.detail,
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall.copy(color = ColorBodyText, lineHeight = 20.sp),
            )
        }
    }
}

// ── Design Note ───────────────────────────────────────────────────────────────
@Composable
private fun DesignNoteCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorPartial.copy(alpha = 0.07f))
            .border(1.dp, ColorPartial.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            "Experiment Design Note\n\nFirst run revealed a design flaw: case_3 started fresh without Studies 1–10. All \"attention dilution failures\" were actually confabulation of missing data. We fixed the experiment and reran twice. Scientific honesty > impressive results.",
            style = MaterialTheme.typography.bodySmall.copy(color = ColorBodyText, lineHeight = 20.sp),
        )
    }
}

// ── Shared components ─────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = ColorDimText,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun DarkCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorCard)
            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column(content = content)
    }
}
