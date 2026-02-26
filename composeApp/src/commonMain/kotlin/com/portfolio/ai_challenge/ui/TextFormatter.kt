package com.portfolio.ai_challenge.ui

/**
 * Cleans up markdown and LaTeX formatting from AI responses
 * to make plain text more readable.
 */
fun formatAiResponse(raw: String): String {
    var text = raw

    // Handle literal \n that wasn't decoded
    text = text.replace("\\n", "\n")

    // Remove LaTeX block math delimiters
    text = text.replace("\\[", "")
    text = text.replace("\\]", "")

    // Remove LaTeX inline math delimiters
    text = text.replace("\\(", "")
    text = text.replace("\\)", "")

    // \frac{a}{b} → a/b
    val fracRegex = Regex("""\\frac\{([^}]*)}\{([^}]*)}""")
    text = fracRegex.replace(text) { "${it.groupValues[1]}/${it.groupValues[2]}" }

    // \text{...} → content
    val textRegex = Regex("""\\text\{([^}]*)}""")
    text = textRegex.replace(text) { it.groupValues[1] }

    // \sqrt{...} → √(content)
    val sqrtRegex = Regex("""\\sqrt\{([^}]*)}""")
    text = sqrtRegex.replace(text) { "√(${it.groupValues[1]})" }

    // Common LaTeX symbols
    text = text.replace("\\pi", "π")
    text = text.replace("\\approx", "≈")
    text = text.replace("\\times", "×")
    text = text.replace("\\div", "÷")
    text = text.replace("\\cdot", "·")
    text = text.replace("\\leq", "≤")
    text = text.replace("\\geq", "≥")
    text = text.replace("\\neq", "≠")
    text = text.replace("\\infty", "∞")
    text = text.replace("\\sum", "Σ")
    text = text.replace("\\pm", "±")
    text = text.replace("\\degree", "°")
    text = text.replace("\\quad", " ")
    text = text.replace("\\ ", " ")

    // V_{subscript} → V_subscript (clean braces around subscripts)
    val subRegex = Regex("""_\{([^}]*)}""")
    text = subRegex.replace(text) { "_${it.groupValues[1]}" }

    // ^{superscript} → ^superscript
    val supRegex = Regex("""\^\{([^}]*)}""")
    text = supRegex.replace(text) { "^${it.groupValues[1]}" }

    // Markdown: ## Header → Header
    text = text.replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")

    // Markdown: **bold** → bold
    text = text.replace(Regex("""\*\*([^*]+)\*\*""")) { it.groupValues[1] }

    // Markdown: *italic* → italic
    text = text.replace(Regex("""\*([^*]+)\*""")) { it.groupValues[1] }

    // Markdown: --- horizontal rule → newline
    text = text.replace(Regex("""^-{3,}\s*$""", RegexOption.MULTILINE), "")

    // Remove remaining backslashes before letters (unknown LaTeX commands)
    text = text.replace(Regex("""\\([a-zA-Z]+)""")) { it.groupValues[1] }

    // Clean up excessive blank lines
    text = text.replace(Regex("""\n{3,}"""), "\n\n")

    return text.trim()
}
