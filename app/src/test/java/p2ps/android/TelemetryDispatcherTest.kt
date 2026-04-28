package p2ps.android

import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing

class TelemetryDispatcherTest {

    // Inject Unconfined dispatcher so scope.launch {} runs synchronously in tests
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val apiClient = mockk<ApiClient>(relaxed = true)
    private val telemetryManager = mockk<TelemetryManager>(relaxed = true)

    private val dispatcher = TelemetryDispatcher(apiClient, telemetryManager, testScope)

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
    fun `dispatch calls savePing when API returns false`() {
        coEvery { apiClient.sendPing(any()) } returns false

        dispatcher.dispatch(testPing)

        verify(exactly = 1) { telemetryManager.savePing(testPing) }
    }

    @Test
    fun `dispatch does not call savePing when API returns true`() {
        coEvery { apiClient.sendPing(any()) } returns true

        dispatcher.dispatch(testPing)

        verify(exactly = 0) { telemetryManager.savePing(any()) }
    }

    @Test
    fun `dispatch calls savePing when API throws exception`() {
        coEvery { apiClient.sendPing(any()) } throws RuntimeException("Network error")

        dispatcher.dispatch(testPing)

        verify(exactly = 1) { telemetryManager.savePing(testPing) }
    }

    @Test
    fun `dispatch passes the exact ping to savePing on failure`() {
        val specificPing = TelemetryPing(
            deviceId = "device_XYZ",
            storeId = "store_ABC",
            itemId = "item_999",
            triggerType = "BACKGROUND",
            lat = 44.4268,
            lng = 26.1025,
            accuracy = 5.5f,
            timestamp = 987654321L
        )
        coEvery { apiClient.sendPing(specificPing) } returns false

        dispatcher.dispatch(specificPing)

        verify(exactly = 1) { telemetryManager.savePing(specificPing) }
    }

    @Test
    fun `dispatch multiple pings – each failing ping is individually saved`() {
        val ping1 = testPing.copy(itemId = "item_A", timestamp = 100L)
        val ping2 = testPing.copy(itemId = "item_B", timestamp = 200L)
        coEvery { apiClient.sendPing(any()) } returns false

        dispatcher.dispatch(ping1)
        dispatcher.dispatch(ping2)

        verify(exactly = 1) { telemetryManager.savePing(ping1) }
        verify(exactly = 1) { telemetryManager.savePing(ping2) }
    }

    @Test
    fun `dispatch multiple pings – all succeed so savePing is never called`() {
        val ping1 = testPing.copy(itemId = "item_A", timestamp = 100L)
        val ping2 = testPing.copy(itemId = "item_B", timestamp = 200L)
        coEvery { apiClient.sendPing(any()) } returns true

        dispatcher.dispatch(ping1)
        dispatcher.dispatch(ping2)

        verify(exactly = 0) { telemetryManager.savePing(any()) }
    }

    @Test
    fun `dispatch falls back on generic Exception, not only RuntimeException`() {
        coEvery { apiClient.sendPing(any()) } throws Exception("Timeout")

        dispatcher.dispatch(testPing)

        verify(exactly = 1) { telemetryManager.savePing(testPing) }
    }
}