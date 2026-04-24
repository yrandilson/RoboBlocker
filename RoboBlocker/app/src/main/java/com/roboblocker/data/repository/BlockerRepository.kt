package com.roboblocker.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.roboblocker.data.db.*
import com.roboblocker.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlockerRepository(
    private val db: AppDatabase,
    private val prefs: AppPreferences
) {
    private val blockedDao = db.blockedNumberDao()
    private val logDao = db.callLogDao()

    // ─── Blocked numbers ──────────────────────────────────────────────────────

    val allBlockedNumbers = blockedDao.getAllLive()
    val blockedCount = blockedDao.countLive()

    suspend fun addBlockedNumber(number: BlockedNumber) = blockedDao.insert(number)

    suspend fun removeBlockedNumber(number: BlockedNumber) = blockedDao.delete(number)

    suspend fun removeById(id: Long) = blockedDao.deleteById(id)

    suspend fun isBlocked(number: String): Boolean = blockedDao.isBlocked(normalize(number))

    suspend fun findByNumber(number: String): BlockedNumber? = blockedDao.findByNumber(normalize(number))

    suspend fun importNumbers(numbers: List<String>) {
        val entries = numbers.map {
            BlockedNumber(number = normalize(it), reason = BlockReason.IMPORTED)
        }
        blockedDao.insertAll(entries)
    }

    suspend fun exportAll(): List<BlockedNumber> = blockedDao.getAll()

    suspend fun clearAll() = blockedDao.deleteAll()

    // ─── Call logs ────────────────────────────────────────────────────────────

    val recentCallLogs = logDao.getRecentLive(100)
    val totalBlocked = logDao.blockedCountLive()

    fun blockedToday(todayStart: Long) = logDao.blockedTodayLive(todayStart)

    suspend fun logCall(entry: CallLogEntry) = logDao.insert(entry)

    suspend fun clearLogs() = logDao.deleteAll()

    suspend fun incrementBlockCount(number: String) = blockedDao.incrementBlockCount(number)

    suspend fun callFrequency(number: String, since: Long): Int =
        logDao.callFrequency(number, since)

    // ─── Contacts whitelist ───────────────────────────────────────────────────

    suspend fun isInContacts(context: Context, number: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!prefs.neverBlockContacts) return@withContext false
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normalized = normalize(number)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val contactNum = normalize(
                        cursor.getString(0) ?: continue
                    )
                    if (contactNum == normalized || contactNum.endsWith(normalized.takeLast(8))) {
                        return@withContext true
                    }
                }
            }
            false
        }

    // ─── Pattern matching ─────────────────────────────────────────────────────

    suspend fun matchesPattern(number: String): BlockedNumber? = withContext(Dispatchers.IO) {
        blockedDao.getAll()
            .filter { it.isPattern }
            .firstOrNull { blocked ->
                number.startsWith(blocked.number) ||
                        Regex(blocked.number).containsMatchIn(number)
            }
    }

    private fun normalize(number: String) = number
        .replace(" ", "").replace("-", "")
        .replace("(", "").replace(")", "")
}
