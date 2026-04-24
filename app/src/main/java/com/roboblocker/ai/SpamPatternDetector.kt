package com.roboblocker.ai

data class SpamAnalysisResult(
    val isSpam: Boolean,
    val confidence: Float,        // 0.0 – 1.0
    val reason: String,
    val category: SpamCategory,
    val needsAiCheck: Boolean = false
)

enum class SpamCategory {
    TELEMARKETING, SCAM, ROBOCALL, FREQUENCY_ABUSE, PATTERN_MATCH, LEGITIMATE, UNKNOWN
}

/**
 * Fast, offline spam pattern detector.
 * Checks Brazilian and international robocall patterns before hitting the AI.
 */
object SpamPatternDetector {

    // Known Brazilian telemarketing / robocall prefixes
    private val BR_SPAM_PREFIXES = listOf(
        "0800", "0300", "0900",                // free/premium service numbers
        "4003", "3003", "3004", "4004",        // bank/service short codes
        "08007",                                // specific spam prefix
        "+55110", "+55119",                    // São Paulo spam DDI patterns
    )

    // Common international spam country codes
    private val INTL_SPAM_CODES = listOf(
        "+1268", "+1876", "+1649", "+1242",    // Caribbean scam codes
        "+374", "+234", "+255",                // Known scam origins
        "+7",                                   // Russia robocall
        "+86",                                  // China spam calls
    )

    // Numbers with highly repetitive digits are usually spoofed
    private val SUSPICIOUS_PATTERNS = listOf(
        Regex("^(\\d)\\1{7,}$"),              // 11111111, 99999999, etc.
        Regex("^(12345|123456|1234567|12345678)"),
        Regex("^0{4,}"),                       // 0000xxxx
    )

    // Telemarketing robocall signature: exactly 4-digit service numbers
    private val SHORT_SERVICE_NUMBERS = Regex("^[3-9]\\d{3}$")

    fun analyze(rawNumber: String): SpamAnalysisResult {
        val number = normalize(rawNumber)

        // 1. Empty / private number
        if (number.isEmpty() || rawNumber == "-1" || rawNumber == "unknown") {
            return SpamAnalysisResult(
                isSpam = false,
                confidence = 0.3f,
                reason = "Número oculto / privado",
                category = SpamCategory.UNKNOWN,
                needsAiCheck = false
            )
        }

        // 2. Blacklisted prefix (Brazilian spam)
        for (prefix in BR_SPAM_PREFIXES) {
            if (number.startsWith(prefix) || rawNumber.replace("+55", "").startsWith(prefix)) {
                return SpamAnalysisResult(
                    isSpam = true,
                    confidence = 0.85f,
                    reason = "Prefixo de telemarketing brasileiro ($prefix)",
                    category = SpamCategory.TELEMARKETING
                )
            }
        }

        // 3. International spam codes
        for (code in INTL_SPAM_CODES) {
            if (rawNumber.startsWith(code)) {
                return SpamAnalysisResult(
                    isSpam = true,
                    confidence = 0.80f,
                    reason = "Código internacional suspeito ($code)",
                    category = SpamCategory.SCAM
                )
            }
        }

        // 4. Suspicious repetitive patterns
        val digitsOnly = number.filter { it.isDigit() }
        for (pattern in SUSPICIOUS_PATTERNS) {
            if (pattern.containsMatchIn(digitsOnly)) {
                return SpamAnalysisResult(
                    isSpam = true,
                    confidence = 0.90f,
                    reason = "Padrão numérico suspeito (número gerado automaticamente)",
                    category = SpamCategory.ROBOCALL
                )
            }
        }

        // 5. Short service numbers (4 digits - typical telemarketing)
        if (SHORT_SERVICE_NUMBERS.matches(digitsOnly)) {
            return SpamAnalysisResult(
                isSpam = true,
                confidence = 0.75f,
                reason = "Número de serviço de 4 dígitos (típico de telemarketing)",
                category = SpamCategory.TELEMARKETING
            )
        }

        // 6. Very long international number (spoofed)
        if (digitsOnly.length > 15) {
            return SpamAnalysisResult(
                isSpam = true,
                confidence = 0.70f,
                reason = "Número internacional muito longo (possível spoofing)",
                category = SpamCategory.SCAM
            )
        }

        // 7. Borderline – send to AI for deeper analysis
        val borderline = digitsOnly.length < 8 || rawNumber.startsWith("+") && digitsOnly.length > 11
        return SpamAnalysisResult(
            isSpam = false,
            confidence = 0.0f,
            reason = "Sem padrão local detectado",
            category = SpamCategory.UNKNOWN,
            needsAiCheck = borderline
        )
    }

    // ─── Frequency abuse check ────────────────────────────────────────────────

    fun isFrequencyAbuse(callCount: Int, thresholdPerHour: Int): SpamAnalysisResult? {
        return if (callCount >= thresholdPerHour) {
            SpamAnalysisResult(
                isSpam = true,
                confidence = 0.95f,
                reason = "Abuso de frequência: $callCount chamadas na última hora",
                category = SpamCategory.FREQUENCY_ABUSE
            )
        } else null
    }

    private fun normalize(number: String): String {
        return number
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trimStart('+')
    }
}
