package p2ps.android

import android.util.Log
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryPing
import io.mockk.clearMocks

class HardwareManagerTest {

    private val mockDispatcher = mockk<TelemetryDispatcher>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        coEvery { mockDispatcher.dispatch(any()) } returns Unit

        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() { unmockkAll() }

    private fun buildPing(itemId: String = "item_001", pingId: String = "ping-001") =
        TelemetryPing("dev_001", "store_001", itemId, "HARDWARE",
            44.4268, 26.1025, 5f, 1_700_000_000_000L, pingId)

    private fun manager(initialized: Boolean = false): HardwareManager {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        return HardwareManager(mockDispatcher, scope).also {
            if (initialized) it.initialize()
        }
    }

    @Test
    fun initialize_logsMessage() {
        manager().initialize()
        verify { Log.d("HardwareManager", any()) }
    }

    @Test
    fun initialize_isIdempotent() {
        val m = manager()
        m.initialize()
        m.initialize()
        verify(exactly = 1) { Log.d("HardwareManager", "Initializing Hardware SDK...") }
    }

    @Test
    fun handleHardwareTrigger_whenNotInitialized_logsError() {
        manager().handleHardwareTrigger(buildPing())
        verify { Log.e("HardwareManager", "Trigger failed: Hardware not initialized.") }
    }

    @Test
    fun handleHardwareTrigger_whenNotInitialized_doesNotCrash() {
        try { manager().handleHardwareTrigger(buildPing()) }
        catch (e: Exception) { fail("Should not throw: ${e.message}") }
    }

    @Test
    fun handleHardwareTrigger_whenInitialized_callsDispatch() = runTest {
        manager(initialized = true).handleHardwareTrigger(buildPing())
        coVerify(exactly = 1) { mockDispatcher.dispatch(any()) }
    }

    @Test
    fun handleHardwareTrigger_whenInitialized_doesNotLogError() = runTest {
        manager(initialized = true).handleHardwareTrigger(buildPing())
        verify(exactly = 0) { Log.e("HardwareManager", "Trigger failed: Hardware not initialized.") }
    }

    @Test
    fun handleHardwareTrigger_multiplePings_eachDispatched() = runTest {
        val m = manager(initialized = true)
        m.handleHardwareTrigger(buildPing(itemId = "A", pingId = "p1"))
        m.handleHardwareTrigger(buildPing(itemId = "B", pingId = "p2"))
        coVerify(exactly = 2) { mockDispatcher.dispatch(any()) }
    }

    @Test
    fun handleHardwareTrigger_deprecatedOverload_logsWarning() {
        manager().handleHardwareTrigger("store_1", "item_1")
        verify { Log.w("HardwareManager", any<String>()) }
    }

    @Test
    fun handleHardwareTrigger_deprecatedOverload_doesNotCrash() {
        try { manager().handleHardwareTrigger("s", "i") }
        catch (e: Exception) { fail("Should not throw") }
    }
}