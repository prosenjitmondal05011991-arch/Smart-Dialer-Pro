package com.example.smartdialer.data.repository

import com.example.smartdialer.data.local.BlockedNumberDao
import com.example.smartdialer.data.local.BlockedNumberEntity
import com.example.smartdialer.util.BlocklistMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BlocklistRepository(
    private val blockedNumberDao: BlockedNumberDao
) {
    val blockedNumbersFlow: Flow<List<BlockedNumberEntity>> =
        blockedNumberDao.observeBlockedNumbers()

    suspend fun addBlockedNumber(rawNumber: String, label: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            val normalized = BlocklistMatcher.normalizeNumber(rawNumber)
            if (normalized.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid phone number"))
            }

            runCatching {
                val existing = blockedNumberDao.getAllBlockedSnapshot()
                val alreadyBlocked = existing.any {
                    BlocklistMatcher.isMatch(normalized, it.normalizedNumber)
                }
                if (!alreadyBlocked) {
                    blockedNumberDao.insertBlocked(
                        BlockedNumberEntity(
                            normalizedNumber = normalized,
                            displayLabel = label?.trim()?.takeIf { it.isNotEmpty() },
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

    suspend fun unblockById(id: Long) = withContext(Dispatchers.IO) {
        blockedNumberDao.deleteById(id)
    }

    suspend fun unblockNumber(rawNumber: String) = withContext(Dispatchers.IO) {
        val existing = blockedNumberDao.getAllBlockedSnapshot()
        existing.filter { BlocklistMatcher.isMatch(rawNumber, it.normalizedNumber) }
            .forEach { blockedNumberDao.deleteById(it.id) }
    }

    suspend fun isBlocked(rawNumber: String): Boolean = withContext(Dispatchers.IO) {
        val existing = blockedNumberDao.getAllBlockedSnapshot()
        existing.any { BlocklistMatcher.isMatch(rawNumber, it.normalizedNumber) }
    }
}
