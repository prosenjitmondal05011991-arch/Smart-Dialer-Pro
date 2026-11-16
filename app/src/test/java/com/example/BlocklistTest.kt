package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.smartdialer.data.local.SmartDialerDatabase
import com.example.smartdialer.data.repository.BlocklistRepository
import com.example.smartdialer.util.BlocklistMatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BlocklistTest {

    private lateinit var database: SmartDialerDatabase
    private lateinit var repository: BlocklistRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SmartDialerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BlocklistRepository(database.blockedNumberDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `normalizeNumber strips formatting and preserves leading plus`() {
        assertEquals("+8801712345678", BlocklistMatcher.normalizeNumber(" +880 1712-345678 "))
        assertEquals("01712345678", BlocklistMatcher.normalizeNumber("(01712) 345-678"))
        assertEquals("", BlocklistMatcher.normalizeNumber("   ---   "))
    }

    @Test
    fun `isMatch handles international and local subscriber formats`() {
        assertTrue(BlocklistMatcher.isMatch("+8801712345678", "01712345678"))
        assertTrue(BlocklistMatcher.isMatch("01712-345678", "+880 1712 345678"))
        assertTrue(BlocklistMatcher.isMatch("+1 (555) 234-5678", "5552345678"))
        assertTrue(BlocklistMatcher.isMatch("121", "121"))

        assertFalse(BlocklistMatcher.isMatch("01712345679", "+8801712345678"))
        assertFalse(BlocklistMatcher.isMatch("121", "1210"))
        assertFalse(BlocklistMatcher.isMatch("", "01712345678"))
    }

    @Test
    fun `repository persists blocks prevents equivalent duplicates and unblocks cleanly`() = runTest {
        val addResult = repository.addBlockedNumber("+880 1712-345678", "Spam Caller")
        assertTrue(addResult.isSuccess)

        // Adding equivalent local format should not create a duplicate row
        val duplicateResult = repository.addBlockedNumber("01712345678", "Duplicate")
        assertTrue(duplicateResult.isSuccess)

        val snapshot = database.blockedNumberDao().getAllBlockedSnapshot()
        assertEquals(1, snapshot.size)
        assertEquals("+8801712345678", snapshot.first().normalizedNumber)
        assertEquals("Spam Caller", snapshot.first().displayLabel)

        assertTrue(repository.isBlocked("01712345678"))
        assertTrue(repository.isBlocked("+8801712345678"))
        assertFalse(repository.isBlocked("01812345678"))

        repository.unblockNumber("01712345678")
        assertFalse(repository.isBlocked("+8801712345678"))
        assertTrue(database.blockedNumberDao().getAllBlockedSnapshot().isEmpty())
    }

    @Test
    fun `repository rejects blank or non-numeric numbers and supports unblockById`() = runTest {
        val invalidResult = repository.addBlockedNumber("abc")
        assertTrue(invalidResult.isFailure)

        repository.addBlockedNumber("01911000111", "Telemarketer")
        val inserted = database.blockedNumberDao().getAllBlockedSnapshot().single()
        repository.unblockById(inserted.id)

        assertTrue(database.blockedNumberDao().getAllBlockedSnapshot().isEmpty())
    }
}
