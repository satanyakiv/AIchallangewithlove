package com.portfolio.ai_challenge.experiment

import com.portfolio.ai_challenge.experiment.models.TestCase
import com.portfolio.ai_challenge.experiment.models.TestStep

private val SECTION_DIVIDER = Regex("""^=== .+ ===$""")
private val MESSAGE_HEADER = Regex("""^MESSAGE (\d+)$""")
private val CHECKPOINT_HEADER = Regex("""^CHECKPOINT (C[\w-]+) \((.+)\)$""")
private val VERIFICATION_HEADER = Regex("""^VERIFICATION (V[\w-]+) \((.+)\)$""")

fun parseTestData(name: String, rawText: String): TestCase {
    val lines = rawText.lines()
    val sections = mutableListOf<Pair<String, String>>() // header to body

    var currentHeader: String? = null
    val currentBody = StringBuilder()

    for (line in lines) {
        if (SECTION_DIVIDER.matches(line.trim())) {
            if (currentHeader != null) {
                sections.add(currentHeader to currentBody.toString().trim())
            }
            currentHeader = line.trim().removePrefix("=== ").removeSuffix(" ===")
            currentBody.clear()
        } else {
            currentBody.appendLine(line)
        }
    }
    if (currentHeader != null) {
        sections.add(currentHeader to currentBody.toString().trim())
    }

    var systemPrompt = ""
    val steps = mutableListOf<TestStep>()

    for ((header, body) in sections) {
        when {
            header == "SYSTEM PROMPT" -> systemPrompt = body
            header.startsWith("NOTE FOR AUTOTEST") -> { /* skip */ }
            MESSAGE_HEADER.matches(header) -> {
                val num = MESSAGE_HEADER.find(header)!!.groupValues[1]
                steps.add(TestStep.Message(id = "M$num", content = body))
            }
            CHECKPOINT_HEADER.matches(header) -> {
                val groups = CHECKPOINT_HEADER.find(header)!!.groupValues
                steps.add(TestStep.Checkpoint(id = groups[1], failureMode = groups[2], content = body))
            }
            VERIFICATION_HEADER.matches(header) -> {
                val groups = VERIFICATION_HEADER.find(header)!!.groupValues
                steps.add(TestStep.Verification(id = groups[1], failureMode = groups[2], content = body))
            }
        }
    }

    return TestCase(name = name, systemPrompt = systemPrompt, steps = steps)
}
