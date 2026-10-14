package com.yaan.core.domain.privacy

/**
 * Result of rule-based context minimisation and data redaction.
 */
data class RedactionResult(
    val redactedText: String,
    val substitutionMap: Map<String, String>, // Placeholder -> Original Secret
    val redactionsApplied: Int
) {
    /**
     * Swaps placeholders back to original secrets on-device after receiving
     * cloud model responses. The cloud never sees the real plaintext values.
     */
    fun restore(modelOutput: String): String {
        var restored = modelOutput
        for ((placeholder, original) in substitutionMap) {
            restored = restored.replace(placeholder, original)
        }
        return restored
    }
}

/**
 * Rule-based data redaction and context minimisation engine.
 *
 * IMPORTANT PRIVACY NOTICE:
 * This rule-based engine REDUCES the transmission of sensitive personally identifiable
 * information (PII) to external cloud providers. It does NOT guarantee 100% removal
 * of all possible sensitive nuances.
 */
object VaultRedactor {

    // Regex for Email addresses (RFC 5322 subset)
    private val EMAIL_REGEX = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")

    // Regex for International and US Phone numbers
    private val PHONE_REGEX = Regex("(?:\\+\\d{1,3}[-. ]|\\b\\d{1,3}[-. ])?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}\\b")

    // Regex for Credit Card / 16-digit Account Numbers
    private val CARD_REGEX = Regex("\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b")

    // Regex for Currency amounts (e.g. $14,200, $180,000, €5,000)
    private val AMOUNT_REGEX = Regex("[$€£]\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?\\b")

    // Regex for Secret tokens / alphanumeric PINs (e.g. 9482-Bravo-Hotel)
    private val PIN_CODE_REGEX = Regex("\\b\\d{4}-[A-Za-z]+-[A-Za-z]+\\b")

    /**
     * Redacts PII patterns from [input] and returns a [RedactionResult]
     * with bidirectional substitution mapping.
     * Optionally accepts [userProtectedTerms] (e.g., custom names or account codes).
     */
    fun redact(
        input: String,
        userProtectedTerms: List<String> = emptyList()
    ): RedactionResult {
        var text = input
        val map = LinkedHashMap<String, String>()
        var count = 0

        // 1. Redact Emails
        text = EMAIL_REGEX.replace(text) { match ->
            val placeholder = "[EMAIL_${++count}]"
            map[placeholder] = match.value
            placeholder
        }

        // 2. Redact Phone numbers
        text = PHONE_REGEX.replace(text) { match ->
            val placeholder = "[PHONE_${++count}]"
            map[placeholder] = match.value
            placeholder
        }

        // 3. Redact Credit Card numbers
        text = CARD_REGEX.replace(text) { match ->
            val placeholder = "[CARD_${++count}]"
            map[placeholder] = match.value
            placeholder
        }

        // 4. Redact Specific PIN / Security Code patterns
        text = PIN_CODE_REGEX.replace(text) { match ->
            val placeholder = "[SECRET_PIN_${++count}]"
            map[placeholder] = match.value
            placeholder
        }

        // 5. Redact Currency amounts if sensitive
        text = AMOUNT_REGEX.replace(text) { match ->
            val placeholder = "[AMOUNT_${++count}]"
            map[placeholder] = match.value
            placeholder
        }

        // 6. User-defined protected terms
        for (term in userProtectedTerms) {
            if (term.isNotBlank() && text.contains(term, ignoreCase = true)) {
                val placeholder = "[PROTECTED_${++count}]"
                map[placeholder] = term
                text = text.replace(term, placeholder, ignoreCase = true)
            }
        }

        return RedactionResult(
            redactedText = text,
            substitutionMap = map,
            redactionsApplied = count
        )
    }

    /**
     * Minimises context by extracting only the top-[maxSnippets] items,
     * truncating each snippet to [maxSnippetChars], and stripping instruction-like text.
     */
    fun minimiseContext(
        snippets: List<String>,
        maxSnippets: Int = 3,
        maxSnippetChars: Int = 300
    ): List<String> {
        return snippets.take(maxSnippets).map { snippet ->
            val sanitized = sanitizeInstructionLikeText(snippet)
            if (sanitized.length > maxSnippetChars) {
                sanitized.take(maxSnippetChars) + "..."
            } else {
                sanitized
            }
        }
    }

    /**
     * Strips prompt injection artifacts or instruction-like text from retrieved notes.
     */
    fun sanitizeInstructionLikeText(snippet: String): String {
        return snippet
            .replace(Regex("(?i)ignore (all )?previous instructions"), "[REDACTED_INSTRUCTION]")
            .replace(Regex("(?i)system prompt override"), "[REDACTED_INSTRUCTION]")
            .replace(Regex("<script.*?>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
    }
}
