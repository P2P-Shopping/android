package p2ps.android.proximity

import android.util.Log
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import p2ps.android.proximity.data.ProximityPing
import retrofit2.Response

class ProximityClientTest {

    private lateinit var mockApiService: ProximityApiService
    private lateinit var client: ProximityClient

    private val testPing = ProximityPing(
        deviceId = "device_001", lat = 47.15, lng = 27.59,
        timestamp = 1700000000000L, fcmToken = "fcm_token_abc"
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        mockApiService = mockk()
        client = ProximityClient(apiService = mockApiService)
    }

    @After
    fun tearDown() { unmockkAll() }

    @Test
    fun sendPing_whenResponseIs200_returnsTrue() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } returns Response.success(Unit)
        assertTrue(client.sendPing(testPing))
    }

    @Test
    fun sendPing_whenResponseIs202_returnsTrue() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } returns Response.success(202, Unit)
        assertTrue(client.sendPing(testPing))
    }

    @Test
    fun sendPing_whenResponseIs500_returnsFalse() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } returns
                Response.error(500, "".toResponseBody(null))
        assertFalse(client.sendPing(testPing))
    }

    @Test
    fun sendPing_whenResponseIs400_returnsFalse() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } returns
                Response.error(400, "".toResponseBody(null))
        assertFalse(client.sendPing(testPing))
    }

    @Test
    fun sendPing_whenNetworkException_returnsFalse() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } throws RuntimeException("Network error")
        assertFalse(client.sendPing(testPing))
    }

    @Test
    fun sendPing_whenTimeoutException_returnsFalse() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } throws java.net.SocketTimeoutException("Timeout")
        assertFalse(client.sendPing(testPing))
    }

    @Test
    fun sendPing_whenException_doesNotThrow() {
        coEvery { mockApiService.sendProximityPing(any()) } throws Exception("IO error")
        runBlocking {
            try {
                client.sendPing(testPing)
            } catch (e: Exception) {
                fail("sendPing should not throw: ${e.message}")
            }
        }
    }

    @Test
    fun sendPing_passesCorrectPingToApiService() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } returns Response.success(Unit)
        client.sendPing(testPing)
        coVerify(exactly = 1) { mockApiService.sendProximityPing(testPing) }
    }

    @Test
    fun sendPing_multipleCalls_eachIndependent() = runBlocking {
        coEvery { mockApiService.sendProximityPing(any()) } returns Response.success(Unit)
        client.sendPing(testPing.copy(deviceId = "dev_A"))
        client.sendPing(testPing.copy(deviceId = "dev_B"))
        coVerify(exactly = 2) { mockApiService.sendProximityPing(any()) }
    }
}