package p2ps.android

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import p2ps.android.data.TelemetryPing

class ApiClientTest {

    private val apiClient = ApiClient()

    private val testPing = TelemetryPing(
        deviceId = "dev_001",
        storeId = "store_001",
        itemId = "item_001",
        triggerType = "BACKGROUND",
        lat = 44.4268,
        lng = 26.1025,
        accuracyMeters = 10.0f,
        timestamp = 1700000000000L
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
    fun `sendPing returns true for current simulation`() {
        val result = apiClient.sendPing(testPing)
        assertTrue("sendPing should return true in simulation mode", result)
    }

    @Test
    fun `sendPing returns Boolean type`() {
        val result = apiClient.sendPing(testPing)
        // Result is a primitive boolean - just verifying type contract
        assertTrue(result is Boolean)
    }

    @Test
    fun `sendPing accepts ping with zero coordinates`() {
        val zeroPing = TelemetryPing("d", "s", "i", "t", 0.0, 0.0, 0f, 0L)
        val result = apiClient.sendPing(zeroPing)
        assertTrue(result)
    }

    @Test
    fun `sendPing accepts ping with boundary coordinates`() {
        val boundaryPing = TelemetryPing("d", "s", "i", "t", 90.0, 180.0, 1f, 0L)
        val result = apiClient.sendPing(boundaryPing)
        assertTrue(result)
    }

    @Test
    fun `sendPing accepts ping with negative coordinates`() {
        val southPing = TelemetryPing("d", "s", "i", "BACKGROUND", -33.8688, -70.6693, 5f, 0L)
        val result = apiClient.sendPing(southPing)
        assertTrue(result)
    }

    @Test
    fun `sendPing logs info about POST endpoint`() {
        apiClient.sendPing(testPing)
        verify { Log.i("ApiClient", "POST /api/telemetry/ping") }
    }

    @Test
    fun `sendPing logs acceptance on success`() {
        apiClient.sendPing(testPing)
        verify { Log.i("ApiClient", "Simulation: Telemetry accepted (202 Accepted)") }
    }

    @Test
    fun `sendPing accepts ping with MANUAL trigger type`() {
        val manualPing = testPing.copy(triggerType = "MANUAL")
        val result = apiClient.sendPing(manualPing)
        assertTrue(result)
    }

    @Test
    fun `sendPing accepts ping with STARTUP_AUTO_SCAN trigger type`() {
        val startupPing = testPing.copy(triggerType = "STARTUP_AUTO_SCAN")
        val result = apiClient.sendPing(startupPing)
        assertTrue(result)
    }

    @Test
    fun `sendPing called multiple times always returns true`() {
        repeat(5) {
            val result = apiClient.sendPing(testPing.copy(timestamp = it.toLong()))
            assertTrue("sendPing should return true on call #$it", result)
        }
    }
}