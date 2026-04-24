package com.roboblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Blocked Number ───────────────────────────────────────────────────────────

@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val label: String = "",
    val reason: BlockReason = BlockReason.MANUAL,
    val addedAt: Long = System.currentTimeMillis(),
    val timesBlocked: Int = 0,
    val isPattern: Boolean = false    // true = number is a prefix/regex pattern
)

enum class BlockReason {
    MANUAL,
    AI_DETECTED,
    PATTERN_MATCH,
    FREQUENCY_ABUSE,
    IMPORTED
}

// ─── Call Log Entry ───────────────────────────────────────────────────────────

@Entity(tableName = "call_logs")
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: CallAction,
    val reason: String = "",
    val aiConfidence: Float = 0f,
    val spamCategory: String = ""
)

enum class CallAction {
    BLOCKED,
    ALLOWED,
    WHITELISTED
}
