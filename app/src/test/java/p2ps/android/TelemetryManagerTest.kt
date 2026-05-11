package p2ps.android

import android.content.Context
import android.util.Log
import io.mockk.*
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
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        mockkObject(AppDatabase.Companion)
        every { AppDatabase.getDatabase(any()) } returns mockDatabase
        every { mockDatabase.telemetryDao() } returns mockDao

        // Mock suspend functions explicitly
        coEvery { mockDao.getAllPings() } returns emptyList()
        coEvery { mockDao.insertPing(any()) } returns Unit
        coEvery { mockDao.deletePings(any()) } returns Unit
        coEvery { mockDao.clearCache() } returns Unit

        telemetryManager = TelemetryManager(mockContext)
    }

    @After
    fun tearDown() { unmockkAll() }

    private fun entity(id: Int = 0, pingId: String = "p1") = TelemetryEntity(
        id = id, deviceId = "dev", storeId = "store", itemId = "item",
        triggerType = "BACKGROUND", latitude = 44.0, longitude = 26.0,
        accuracy = 5f, timestamp = 1000L, pingId = pingId
    )

    @Test
    fun getAllStoredPings_returnsEmptyList() = runBlocking {
        coEvery { mockDao.getAllPings() } returns emptyList()
        assertTrue(telemetryManager.getAllStoredPings().isEmpty())
    }

    @Test
    fun getAllStoredPings_returnsEntities() = runBlocking {
        val entities = listOf(entity(1, "p1"), entity(2, "p2"))
        coEvery { mockDao.getAllPings() } returns entities
        assertEquals(2, telemetryManager.getAllStoredPings().size)
    }

    @Test
    fun getAllStoredPings_delegatesToDao() = runBlocking {
        coEvery { mockDao.getAllPings() } returns emptyList()
        telemetryManager.getAllStoredPings()
        coVerify(exactly = 1) { mockDao.getAllPings() }
    }

    @Test
    fun deletePings_callsDao() = runBlocking {
        val pings = listOf(entity(1))
        telemetryManager.deletePings(pings)
        coVerify(exactly = 1) { mockDao.deletePings(pings) }
    }

    @Test
    fun deletePings_emptyList_doesNotThrow() = runBlocking {
        telemetryManager.deletePings(emptyList())
        coVerify(exactly = 1) { mockDao.deletePings(emptyList()) }
    }

    @Test
    fun clearCache_callsDao() = runBlocking {
        telemetryManager.clearCache()
        coVerify(exactly = 1) { mockDao.clearCache() }
    }

    @Test
    fun clearCache_multipleCalls_eachDelegatesToDao() = runBlocking {
        telemetryManager.clearCache()
        telemetryManager.clearCache()
        coVerify(exactly = 2) { mockDao.clearCache() }
    }
    @Test
    fun savePing_triggersDaoInsertion() = runBlocking {
        val ping = TelemetryPing("dev", "store", "item", "BACKGROUND",
            44.0, 26.0, 5f, 1000L, "ping-001")
        telemetryManager.savePing(ping)
        coVerify(timeout = 1_000, exactly = 1) { mockDao.insertPing(any()) }
    }
}