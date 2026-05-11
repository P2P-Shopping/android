package p2ps.android

import android.content.Context
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryDao
import p2ps.android.data.TelemetryPing

class TelemetryDispatcherTest {

    private val mockDao = mockk<TelemetryDao>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)
    private val dispatcher = TelemetryDispatcher(mockDao, mockContext)

    private val testPing = TelemetryPing(
        deviceId = "test_id", storeId = "store_1", itemId = "item_1",
        triggerType = "test", lat = 0.0, lng = 0.0,
        accuracyMeters = 0f, timestamp = 123456789L, pingId = "ping-001"
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `dispatch inserts ping into dao`() = runBlocking {
        dispatcher.dispatch(testPing)
        coVerify(exactly = 1) { mockDao.insertPing(any()) }
    }

    @Test
    fun `dispatch with different pings inserts each`() = runBlocking {
        val ping1 = testPing.copy(itemId = "item_A", pingId = "p1")
        val ping2 = testPing.copy(itemId = "item_B", pingId = "p2")
        dispatcher.dispatch(ping1)
        dispatcher.dispatch(ping2)
        coVerify(exactly = 2) { mockDao.insertPing(any()) }
    }

    @Test
    fun `dispatch when dao throws does not propagate exception`() = runBlocking {
        coEvery { mockDao.insertPing(any()) } throws RuntimeException("DB error")
        dispatcher.dispatch(testPing) // should not throw
    }

    @Test
    fun `dispatch when dao throws logs error`() = runBlocking {
        coEvery { mockDao.insertPing(any()) } throws RuntimeException("DB error")
        dispatcher.dispatch(testPing)
        io.mockk.verify { Log.e(any(), any<String>()) }
    }
}