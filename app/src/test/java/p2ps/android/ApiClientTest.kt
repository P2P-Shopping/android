package p2ps.android

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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
        accuracy = 8f,
        timestamp = 1700000000000L
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun sendPing_returnsBoolean() = runBlocking {
        val result = apiClient.sendPing(testPing)
        // Result must be a Boolean (compile-time guaranteed, but confirms the API contract)
        assertTrue(result is Boolean)
    }

    @Test
    fun sendPing_overManyInvocations_returnsBothTrueAndFalse() = runBlocking {
        // simulateNetworkSuccess() returns true ~80% of the time (Random.nextFloat() > 0.2f)
        // Over 200 invocations the probability of all being true or all false is negligible
        val results = (1..200).map { apiClient.sendPing(testPing) }
        assertTrue("Expected at least one success", results.any { it })
        assertTrue("Expected at least one failure", results.any { !it })
    }

    @Test
    fun sendPing_successRate_isApproximately80Percent() = runBlocking {
        // Expected success rate is ~80% (threshold > 0.2f).
        // With 500 samples the observed rate should be between 70% and 90%.
        val n = 500
        val successes = (1..n).count { apiClient.sendPing(testPing) }
        val rate = successes.toDouble() / n
        assertTrue(
            "Success rate $rate is outside expected range [0.70, 0.90]",
            rate in 0.70..0.90
        )
    }

    @Test
    fun sendPing_withMinimalPing_doesNotThrow() = runBlocking {
        val minimalPing = TelemetryPing("", "", "", "", 0.0, 0.0, 0f, 0L)
        // Must not throw regardless of ping content
        val result = apiClient.sendPing(minimalPing)
        assertTrue(result is Boolean)
    }

    @Test
    fun sendPing_withExtremeCoordinates_doesNotThrow() = runBlocking {
        val extremePing = TelemetryPing("d", "s", "i", "t", -90.0, -180.0, 0f, Long.MAX_VALUE)
        val result = apiClient.sendPing(extremePing)
        assertTrue(result is Boolean)
    }

    @Test
    fun sendPing_differentPings_returnBoolean() = runBlocking {
        val pings = listOf(
            testPing.copy(itemId = "a", timestamp = 1L),
            testPing.copy(itemId = "b", timestamp = 2L),
            testPing.copy(itemId = "c", timestamp = 3L)
        )
        pings.forEach { ping ->
            val result = apiClient.sendPing(ping)
            assertTrue(result is Boolean)
        }
    }
}