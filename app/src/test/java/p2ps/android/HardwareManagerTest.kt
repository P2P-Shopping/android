package p2ps.android

import android.util.Log
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*
import p2ps.android.data.TelemetryPing

@OptIn(ExperimentalCoroutinesApi::class)
class HardwareManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ── initialize() ──────────────────────────────────────────────────────────

    @Test
    fun initialize_logsSDKStartMessage() {
        HardwareManager().initialize()
        verify { Log.d("HardwareManager", "Initializing Hardware SDK Scaffolding...") }
    }

    @Test
    fun initialize_logsConnectionSuccess() {
        HardwareManager().initialize()
        verify { Log.i("HardwareManager", "Hardware connection established successfully.") }
    }

    @Test
    fun initialize_isIdempotent_onlyInitializesOnce() {
        val manager = HardwareManager()
        manager.initialize()
        manager.initialize()
        // "Initializing…" should be logged exactly once
        verify(exactly = 1) { Log.d("HardwareManager", "Initializing Hardware SDK Scaffolding...") }
    }

    @Test
    fun initialize_thirdCallIsAlsoNoOp() {
        val manager = HardwareManager()
        repeat(3) { manager.initialize() }
        verify(exactly = 1) { Log.d("HardwareManager", "Initializing Hardware SDK Scaffolding...") }
    }

    // ── handleHardwareTrigger(ping) ───────────────────────────────────────────

    @Test
    fun handleHardwareTrigger_whenNotInitialized_logsError() {
        val manager = HardwareManager()
        manager.handleHardwareTrigger(buildPing())
        verify { Log.e("HardwareManager", "Trigger failed: Hardware not initialized.") }
    }

    @Test
    fun handleHardwareTrigger_whenNotInitialized_doesNotCrash() {
        val manager = HardwareManager()
        try {
            manager.handleHardwareTrigger(buildPing())
        } catch (e: Exception) {
            fail("Should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun handleHardwareTrigger_whenInitialized_logsItemId() {
        val manager = HardwareManager().also { it.initialize() }
        mockkConstructor(ApiClient::class)
        coEvery { anyConstructed<ApiClient>().sendPing(any()) } returns true

        manager.handleHardwareTrigger(buildPing(itemId = "item_XYZ"))
        Thread.sleep(100) // allow IO coroutine to execute

        verify { Log.i("HardwareManager", "Hardware Trigger Detected for item: item_XYZ") }
        unmockkConstructor(ApiClient::class)
    }

    @Test
    fun handleHardwareTrigger_whenInitialized_doesNotLogNotInitializedError() {
        val manager = HardwareManager().also { it.initialize() }
        mockkConstructor(ApiClient::class)
        coEvery { anyConstructed<ApiClient>().sendPing(any()) } returns true

        manager.handleHardwareTrigger(buildPing())
        Thread.sleep(100)

        verify(exactly = 0) { Log.e("HardwareManager", "Trigger failed: Hardware not initialized.") }
        unmockkConstructor(ApiClient::class)
    }

    @Test
    fun handleHardwareTrigger_whenInitialized_callsSendPing() = runTest {
        val mockApi = mockk<ApiClient>(relaxed = true)
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        // Cream managerul cu Mock-ul de API
        val manager = HardwareManager(
            dispatcher = testDispatcher,
            apiClient = mockApi
        ).also { it.initialize() } // Aici rulează connectToDevice() al tău, cel real!

        manager.handleHardwareTrigger(buildPing())

        // Acum MockK va vedea apelul 100%
        coVerify(exactly = 1) { mockApi.sendPing(any()) }
    }

    @Test
    fun handleHardwareTrigger_sendPingReturningFalse_doesNotCrash() {
        val manager = HardwareManager().also { it.initialize() }
        mockkConstructor(ApiClient::class)
        coEvery { anyConstructed<ApiClient>().sendPing(any()) } returns false

        try {
            manager.handleHardwareTrigger(buildPing())
            Thread.sleep(100)
        } catch (e: Exception) {
            fail("Should not throw, but got: ${e.message}")
        }
        unmockkConstructor(ApiClient::class)
    }

    @Test
    fun handleHardwareTrigger_sendPingThrowsRuntimeException_catchesAndLogs() = runTest {
        val mockApi = mockk<ApiClient>()
        coEvery { mockApi.sendPing(any()) } throws RuntimeException("Network error")

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = HardwareManager(dispatcher = testDispatcher, apiClient = mockApi).also { it.initialize() }

        manager.handleHardwareTrigger(buildPing())

        // Verificăm dacă log-ul de eroare a fost apelat
        verify { Log.e("HardwareManager", "Failed to send ping from hardware trigger", any()) }
    }

    @Test
    fun handleHardwareTrigger_sendPingThrowsGenericException_catchesAndLogs() = runTest {
        val mockApi = mockk<ApiClient>()
        // Simulăm o excepție generică (nu RuntimeException)
        coEvery { mockApi.sendPing(any()) } throws Exception("Timeout generic")

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = HardwareManager(
            dispatcher = testDispatcher,
            apiClient = mockApi
        ).also { it.initialize() }

        manager.handleHardwareTrigger(buildPing())

        verify { Log.e("HardwareManager", "Failed to send ping from hardware trigger", any()) }
    }

    @Test
    fun handleHardwareTrigger_withEmptyStringFields_doesNotCrash() {
        val manager = HardwareManager().also { it.initialize() }
        mockkConstructor(ApiClient::class)
        coEvery { anyConstructed<ApiClient>().sendPing(any()) } returns true

        try {
            manager.handleHardwareTrigger(buildPing(deviceId = "", storeId = "", itemId = ""))
            Thread.sleep(100)
        } catch (e: Exception) {
            fail("Should not throw")
        }
        unmockkConstructor(ApiClient::class)
    }

    @Test
    fun handleHardwareTrigger_withExtremeCoordinates_doesNotCrash() {
        val manager = HardwareManager().also { it.initialize() }
        mockkConstructor(ApiClient::class)
        coEvery { anyConstructed<ApiClient>().sendPing(any()) } returns true

        try {
            manager.handleHardwareTrigger(buildPing(lat = 90.0, lng = 180.0))
            Thread.sleep(100)
        } catch (e: Exception) {
            fail("Should not throw")
        }
        unmockkConstructor(ApiClient::class)
    }

    @Test
    fun handleHardwareTrigger_multiplePings_eachSentIndependently() = runTest {
        val mockApi = mockk<ApiClient>(relaxed = true)
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = HardwareManager(dispatcher = testDispatcher, apiClient = mockApi).also { it.initialize() }

        manager.handleHardwareTrigger(buildPing(itemId = "A"))
        manager.handleHardwareTrigger(buildPing(itemId = "B"))

        coVerify(exactly = 2) { mockApi.sendPing(any()) }
    }

    // ── handleHardwareTrigger(String, String, String) — deprecated ────────────

    @Test
    fun handleHardwareTrigger_deprecatedOverload_logsWarning() {
        HardwareManager().handleHardwareTrigger("store_1", "item_1", "BUTTON_PRESS")
        verify { Log.w("HardwareManager", "Manual trigger received without location. Pings should be created via MainActivity.") }
    }

    @Test
    fun handleHardwareTrigger_deprecatedOverload_defaultTriggerType_logsWarning() {
        HardwareManager().handleHardwareTrigger("store_X", "item_Y")
        verify { Log.w("HardwareManager", "Manual trigger received without location. Pings should be created via MainActivity.") }
    }

    @Test
    fun handleHardwareTrigger_deprecatedOverload_whenNotInitialized_doesNotCrash() {
        try {
            HardwareManager().handleHardwareTrigger("s", "i")
        } catch (e: Exception) {
            fail("Should not throw")
        }
    }

    @Test
    fun handleHardwareTrigger_deprecatedOverload_whenInitialized_doesNotCrash() {
        val manager = HardwareManager().also { it.initialize() }
        try {
            manager.handleHardwareTrigger("s", "i")
        } catch (e: Exception) {
            fail("Should not throw")
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildPing(
        deviceId: String = "dev_001",
        storeId: String = "store_001",
        itemId: String = "item_001",
        triggerType: String = "HARDWARE",
        lat: Double = 44.4268,
        lng: Double = 26.1025,
        accuracy: Float = 5f,
        timestamp: Long = 1_700_000_000_000L
    ) = TelemetryPing(deviceId, storeId, itemId, triggerType, lat, lng, accuracy, timestamp)
}