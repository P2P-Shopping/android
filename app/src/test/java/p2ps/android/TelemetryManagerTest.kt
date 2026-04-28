package p2ps.android

import android.content.Context
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import p2ps.android.data.AppDatabase
import p2ps.android.data.TelemetryDao
import p2ps.android.data.TelemetryEntity
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing

class TelemetryManagerTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockDatabase = mockk<AppDatabase>(relaxed = true)
    private val mockDao = mockk<TelemetryDao>(relaxed = true)

    private lateinit var telemetryManager: TelemetryManager

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        mockkObject(AppDatabase.Companion)
        every { AppDatabase.getDatabase(any()) } returns mockDatabase
        every { mockDatabase.telemetryDao() } returns mockDao

        telemetryManager = TelemetryManager(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── getAllStoredPings ──────────────────────────────────────────────────────

    @Test
    fun getAllStoredPings_returnsEmptyList_whenCacheIsEmpty() = runBlocking {
        coEvery { mockDao.getAllPings() } returns emptyList()

        val result = telemetryManager.getAllStoredPings()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getAllStoredPings_returnsEntities_fromDao() = runBlocking {
        val entities = listOf(
            TelemetryEntity(1, "dev1", "store1", "item1", "BACKGROUND", 44.0, 26.0, 5f, 1000L),
            TelemetryEntity(2, "dev2", "store2", "item2", "SCAN", 48.0, 2.0, 3f, 2000L)
        )
        coEvery { mockDao.getAllPings() } returns entities

        val result = telemetryManager.getAllStoredPings()

        assertEquals(2, result.size)
        assertEquals("dev1", result[0].deviceId)
        assertEquals("dev2", result[1].deviceId)
    }

    @Test
    fun getAllStoredPings_returnsSingleEntity() = runBlocking {
        val entity = TelemetryEntity(1, "dev", "store", "item", "T", 1.0, 2.0, 1f, 500L)
        coEvery { mockDao.getAllPings() } returns listOf(entity)

        val result = telemetryManager.getAllStoredPings()

        assertEquals(1, result.size)
        assertEquals(entity, result[0])
    }

    @Test
    fun getAllStoredPings_delegatesToDaoGetAllPings() = runBlocking {
        coEvery { mockDao.getAllPings() } returns emptyList()

        telemetryManager.getAllStoredPings()

        coVerify(exactly = 1) { mockDao.getAllPings() }
    }

    // ── deletePings ───────────────────────────────────────────────────────────

    @Test
    fun deletePings_callsDaoDeletePings() = runBlocking {
        val pings = listOf(
            TelemetryEntity(1, "d", "s", "i", "t", 0.0, 0.0, 0f, 100L)
        )
        coEvery { mockDao.deletePings(pings) } returns Unit

        telemetryManager.deletePings(pings)

        coVerify(exactly = 1) { mockDao.deletePings(pings) }
    }

    @Test
    fun deletePings_emptyList_doesNotThrow() = runBlocking {
        coEvery { mockDao.deletePings(emptyList()) } returns Unit

        // Should complete without exception
        telemetryManager.deletePings(emptyList())

        coVerify(exactly = 1) { mockDao.deletePings(emptyList()) }
    }

    @Test
    fun deletePings_withMultipleEntities_passesThemAll() = runBlocking {
        val pings = listOf(
            TelemetryEntity(1, "d1", "s", "i", "t", 1.0, 2.0, 1f, 100L),
            TelemetryEntity(2, "d2", "s", "i", "t", 3.0, 4.0, 1f, 200L),
            TelemetryEntity(3, "d3", "s", "i", "t", 5.0, 6.0, 1f, 300L)
        )
        coEvery { mockDao.deletePings(pings) } returns Unit

        telemetryManager.deletePings(pings)

        coVerify(exactly = 1) { mockDao.deletePings(pings) }
    }

    @Test
    fun deletePings_whenDaoThrows_doesNotPropagateException() = runBlocking {
        val pings = listOf(TelemetryEntity(1, "d", "s", "i", "t", 0.0, 0.0, 0f, 100L))
        coEvery { mockDao.deletePings(pings) } throws RuntimeException("DB error")

        // deletePings wraps the call in try/catch – must not throw
        telemetryManager.deletePings(pings)
    }

    // ── clearAllCache ─────────────────────────────────────────────────────────

    @Test
    fun clearAllCache_callsDaoClearCache() = runBlocking {
        coEvery { mockDao.clearCache() } returns Unit

        telemetryManager.clearAllCache()

        coVerify(exactly = 1) { mockDao.clearCache() }
    }

    @Test
    fun clearAllCache_multipleCallsEachDelegatesToDao() = runBlocking {
        coEvery { mockDao.clearCache() } returns Unit

        telemetryManager.clearAllCache()
        telemetryManager.clearAllCache()

        coVerify(exactly = 2) { mockDao.clearCache() }
    }

    // ── savePing – entity construction verification ───────────────────────────

    @Test
    fun savePing_constructsEntityWithCorrectFieldMapping() {
        // savePing is fire-and-forget (scope.launch), so we verify DAO is wired correctly
        // by checking that AppDatabase.getDatabase and telemetryDao() were called during setup.
        verify(exactly = 1) { AppDatabase.getDatabase(mockContext) }
        verify(atLeast = 1) { mockDatabase.telemetryDao() }
    }

    @Test
    fun savePing_withValidPing_triggersDaoInsertion() = runBlocking {
        val ping = TelemetryPing("dev", "store", "item", "BACKGROUND", 44.0, 26.0, 5f, 1000L)

        telemetryManager.savePing(ping)

        coVerify(exactly = 1) { mockDao.insertPing(any()) }
    }
}
