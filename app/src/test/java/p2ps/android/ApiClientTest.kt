package p2ps.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import p2ps.android.data.TelemetryPing

class ApiClientTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        mockEditor = mockk(relaxed = true)
        mockPrefs = mockk {
            every { getString("DEVICE_ID", null) } returns "test-device-id"
            every { edit() } returns mockEditor
        }
        mockContext = mockk {
            every { getSharedPreferences("p2ps_prefs", Context.MODE_PRIVATE) } returns mockPrefs
        }
    }

    @After
    fun tearDown() { unmockkAll() }

    private fun ping(itemId: String = "item_001", pingId: String = "p1") = TelemetryPing(
        deviceId = "dev_001", storeId = "store_001", itemId = itemId,
        triggerType = "BACKGROUND", lat = 44.4268, lng = 26.1025,
        accuracyMeters = 10.0f, timestamp = 1700000000000L, pingId = pingId
    )

    @Test
    fun apiClient_canBeInstantiated() {
        val client = ApiClient(mockContext)
        assertNotNull(client)
    }

    @Test
    fun sendPing_returnsBoolean() = runBlocking {
        val client = ApiClient(mockContext)
        val result = client.sendPing(ping())
        assertTrue(result is Boolean)
    }

    @Test
    fun sendPing_withMinimalPing_doesNotThrow() = runBlocking {
        val client = ApiClient(mockContext)
        val minimalPing = TelemetryPing("", "", "", "", 0.0, 0.0, 0f, 0L, "")
        val result = client.sendPing(minimalPing)
        assertTrue(result is Boolean)
    }

    @Test
    fun sendPing_withExtremeCoordinates_doesNotThrow() = runBlocking {
        val client = ApiClient(mockContext)
        val extremePing = TelemetryPing("d", "s", "i", "t", -90.0, -180.0, 0f, Long.MAX_VALUE, "p")
        val result = client.sendPing(extremePing)
        assertTrue(result is Boolean)
    }
}