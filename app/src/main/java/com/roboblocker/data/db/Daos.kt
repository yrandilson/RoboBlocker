package com.roboblocker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

// ─── BlockedNumber DAO ────────────────────────────────────────────────────────

@Dao
interface BlockedNumberDao {

    @Query("SELECT * FROM blocked_numbers ORDER BY addedAt DESC")
    fun getAllLive(): LiveData<List<BlockedNumber>>

    @Query("SELECT * FROM blocked_numbers ORDER BY addedAt DESC")
    suspend fun getAll(): List<BlockedNumber>

    @Query("SELECT * FROM blocked_numbers WHERE number = :number LIMIT 1")
    suspend fun findByNumber(number: String): BlockedNumber?

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    suspend fun isBlocked(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(number: BlockedNumber): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(numbers: List<BlockedNumber>)

    @Delete
    suspend fun delete(number: BlockedNumber)

    @Query("DELETE FROM blocked_numbers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_numbers")
    suspend fun deleteAll()

    @Query("UPDATE blocked_numbers SET timesBlocked = timesBlocked + 1 WHERE number = :number")
    suspend fun incrementBlockCount(number: String)

    @Query("SELECT COUNT(*) FROM blocked_numbers")
    fun countLive(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM blocked_numbers")
    suspend fun count(): Int
}

// ─── CallLog DAO ──────────────────────────────────────────────────────────────

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllLive(): LiveData<List<CallLogEntry>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLive(limit: Int = 50): LiveData<List<CallLogEntry>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<CallLogEntry>

    @Insert
    suspend fun insert(log: CallLogEntry): Long

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM call_logs WHERE action = 'BLOCKED'")
    fun blockedCountLive(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM call_logs WHERE action = 'BLOCKED' AND timestamp > :since")
    suspend fun blockedSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM call_logs WHERE number = :number AND timestamp > :since")
    suspend fun callFrequency(number: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM call_logs WHERE action = 'BLOCKED' AND timestamp >= :todayStart")
    fun blockedTodayLive(todayStart: Long): LiveData<Int>
}
