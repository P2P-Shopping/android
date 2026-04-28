package p2ps.android

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import p2ps.android.ApiClient
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before

class TelemetryDispatcherTest {

    private val apiClient = mockk<ApiClient>(relaxed = true)
    private val telemetryManager = mockk<TelemetryManager>(relaxed = true)

    private val dispatcher = TelemetryDispatcher(apiClient, telemetryManager)

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
    private val testPing = TelemetryPing(
        deviceId = "test_id",
        storeId = "store_1",
        itemId = "item_1",
        triggerType = "test",
        lat = 0.0, lng = 0.0, accuracy = 0f,
        timestamp = 123456789L
    )

    @Test
    fun `test dispatch fallback when API fails`() {
        every { apiClient.sendPing(any()) } returns false
        dispatcher.dispatch(testPing)
        verify(exactly = 1) { telemetryManager.savePing(testPing) }
    }

    @Test
    fun `test dispatch success when API works`() {
        every { apiClient.sendPing(any()) } returns true
        dispatcher.dispatch(testPing)
        verify(exactly = 0) { telemetryManager.savePing(any()) }
    }

    @Test
    fun `test dispatch fallback when API throws exception`() {
        every { apiClient.sendPing(any()) } throws RuntimeException("Network unavailable")
        dispatcher.dispatch(testPing)
        verify(exactly = 1) { telemetryManager.savePing(testPing) }
    }

    @Test
    fun `test dispatch fallback when API throws IOException`() {
        every { apiClient.sendPing(any()) } throws java.io.IOException("Connection refused")
        dispatcher.dispatch(testPing)
        verify(exactly = 1) { telemetryManager.savePing(testPing) }
    }

    @Test
    fun `test dispatch does not save when API succeeds for different pings`() {
        val pingA = testPing.copy(itemId = "item_A")
        val pingB = testPing.copy(itemId = "item_B")

        every { apiClient.sendPing(any()) } returns true

        dispatcher.dispatch(pingA)
        dispatcher.dispatch(pingB)

        verify(exactly = 0) { telemetryManager.savePing(any()) }
    }

    @Test
    fun `test dispatch calls savePing once per failed dispatch`() {
        every { apiClient.sendPing(any()) } returns false

        dispatcher.dispatch(testPing)
        dispatcher.dispatch(testPing)

        verify(exactly = 2) { telemetryManager.savePing(testPing) }
    }

    @Test
    fun `test dispatch forwards exact ping object to telemetryManager on failure`() {
        val specificPing = TelemetryPing(
            deviceId = "device_xyz",
            storeId = "store_abc",
            itemId = "item_999",
            triggerType = "MANUAL",
            lat = 45.0,
            lng = 25.0,
            accuracy = 5.5f,
            timestamp = 999999999L
        )
        every { apiClient.sendPing(any()) } returns false

        dispatcher.dispatch(specificPing)

        verify(exactly = 1) { telemetryManager.savePing(specificPing) }
    }
}
