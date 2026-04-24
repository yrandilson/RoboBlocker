package com.roboblocker.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.roboblocker.App
import com.roboblocker.ai.ClaudeAIAnalyzer
import com.roboblocker.ai.SpamAnalysisResult
import com.roboblocker.ai.SpamCategory
import com.roboblocker.ai.SpamPatternDetector
import com.roboblocker.data.db.BlockReason
import com.roboblocker.data.db.BlockedNumber
import com.roboblocker.data.db.CallAction
import com.roboblocker.data.db.CallLogEntry
import com.roboblocker.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class RoboCallScreeningService : CallScreeningService() {

    private val TAG = "RoboBlocker"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var aiAnalyzer: ClaudeAIAnalyzer? = null

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        Log.d(TAG, "Screening call from: $rawNumber")

        scope.launch {
            val prefs = App.instance.preferences
            val repo = App.instance.repository

            // Initialize AI if needed
            if (aiAnalyzer == null && prefs.aiApiKey.isNotBlank()) {
                aiAnalyzer = ClaudeAIAnalyzer(prefs.aiApiKey)
            }

            // ── Guard: blocking disabled globally
            if (!prefs.isBlockingEnabled) {
                respondToCall(rawNumber, allow = true, reason = "Bloqueio desativado")
                return@launch
            }

            // ── Guard: schedule check
            if (prefs.scheduleEnabled && !isWithinSchedule(prefs.scheduleStartHour, prefs.scheduleEndHour)) {
                respondToCall(rawNumber, allow = true, reason = "Fora do horário de bloqueio")
                return@launch
            }

            // ── Guard: contacts whitelist
            if (repo.isInContacts(this@RoboCallScreeningService, rawNumber)) {
                logAndRespond(rawNumber, allow = true, reason = "Contato salvo", confidence = 0f)
                return@launch
            }

            // ── 1. Check manual blacklist
            val manualBlock = repo.findByNumber(rawNumber)
            if (manualBlock != null) {
                repo.incrementBlockCount(rawNumber)
                logAndRespond(rawNumber, allow = false, reason = "Lista negra manual", confidence = 1f, category = SpamCategory.PATTERN_MATCH)
                return@launch
            }

            // ── 2. Check pattern blocklist
            val patternBlock = repo.matchesPattern(rawNumber)
            if (patternBlock != null) {
                logAndRespond(rawNumber, allow = false, reason = "Padrão: ${patternBlock.number}", confidence = 0.95f, category = SpamCategory.PATTERN_MATCH)
                return@launch
            }

            // ── 3. Frequency abuse check
            if (prefs.frequencyBlockEnabled) {
                val oneHourAgo = System.currentTimeMillis() - 3_600_000L
                val freq = repo.callFrequency(rawNumber, oneHourAgo)
                SpamPatternDetector.isFrequencyAbuse(freq, prefs.frequencyThreshold)?.let { result ->
                    // Auto-add to blacklist
                    repo.addBlockedNumber(
                        BlockedNumber(number = rawNumber, reason = BlockReason.FREQUENCY_ABUSE,
                            label = "Auto: abuso de frequência")
                    )
                    logAndRespond(rawNumber, allow = false, reason = result.reason,
                        confidence = result.confidence, category = result.category)
                    return@launch
                }
            }

            // ── 4. Local heuristic pattern detection
            if (prefs.blockSpamPatterns) {
                val heuristic = SpamPatternDetector.analyze(rawNumber)
                if (heuristic.isSpam) {
                    // Auto-add to blacklist
                    repo.addBlockedNumber(
                        BlockedNumber(number = rawNumber, reason = BlockReason.PATTERN_MATCH,
                            label = "Auto: ${heuristic.category.name.lowercase()}")
                    )
                    logAndRespond(rawNumber, allow = false, reason = heuristic.reason,
                        confidence = heuristic.confidence, category = heuristic.category)
                    return@launch
                }

                // If borderline, escalate to AI
                if (heuristic.needsAiCheck && prefs.aiDetectionEnabled) {
                    val aiResult = runAiAnalysis(rawNumber, prefs.aiSensitivity)
                    if (aiResult != null && aiResult.isSpam) {
                        repo.addBlockedNumber(
                            BlockedNumber(number = rawNumber, reason = BlockReason.AI_DETECTED,
                                label = "Auto: IA - ${aiResult.category.name.lowercase()}")
                        )
                        logAndRespond(rawNumber, allow = false, reason = aiResult.reason,
                            confidence = aiResult.confidence, category = aiResult.category, isAi = true)
                        return@launch
                    }
                }
            }

            // ── 5. Block unknown numbers (optional)
            if (prefs.blockUnknownNumbers && rawNumber.isEmpty()) {
                logAndRespond(rawNumber, allow = false, reason = "Número oculto bloqueado", confidence = 1f)
                return@launch
            }

            // ── 6. Block all international numbers (optional)
            if (prefs.blockInternational && rawNumber.startsWith("+") && !rawNumber.startsWith("+55")) {
                logAndRespond(rawNumber, allow = false, reason = "Internacional bloqueado", confidence = 1f)
                return@launch
            }

            // ── Default: allow
            logAndRespond(rawNumber, allow = true, reason = "Sem ameaça detectada", confidence = 0f)
        }
    }

    private suspend fun runAiAnalysis(number: String, sensitivity: Int): SpamAnalysisResult? {
        return try {
            aiAnalyzer?.analyze(number, sensitivity)
        } catch (e: Exception) {
            Log.e(TAG, "AI analysis failed: ${e.message}")
            null
        }
    }

    private suspend fun logAndRespond(
        number: String,
        allow: Boolean,
        reason: String,
        confidence: Float = 0f,
        category: SpamCategory = SpamCategory.UNKNOWN,
        isAi: Boolean = false
    ) {
        val prefs = App.instance.preferences
        val repo = App.instance.repository

        // Log to DB
        repo.logCall(
            CallLogEntry(
                number = number,
                action = if (allow) CallAction.ALLOWED else CallAction.BLOCKED,
                reason = reason,
                aiConfidence = confidence,
                spamCategory = category.name
            )
        )

        // Show notification for blocked calls
        if (!allow && prefs.showBlockedNotification) {
            NotificationHelper.showBlockedCallNotification(
                context = this,
                number = number,
                reason = reason,
                isAi = isAi
            )
        }

        respondToCall(number, allow, reason)
    }

    private fun respondToCall(number: String, allow: Boolean, reason: String) {
        val response = if (allow) {
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
        } else {
            Log.i(TAG, "BLOCKING call from $number — $reason")
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSilenceCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }
        respondToCall(response)
    }

    private fun isWithinSchedule(startHour: Int, endHour: Int): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) {
            hour >= startHour || hour < endHour
        } else {
            hour in startHour until endHour
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[SupervisorJob]?.cancel()
    }
}
