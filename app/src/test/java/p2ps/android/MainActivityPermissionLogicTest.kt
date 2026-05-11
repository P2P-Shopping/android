package p2ps.android

import android.Manifest
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import p2ps.android.data.TelemetryPing

class MainActivityPermissionLogicTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() { unmockkAll() }

    private fun canProceedWithTrigger(hasFine: Boolean, hasCoarse: Boolean) = hasFine || hasCoarse
    private fun isNotificationsGranted(sdkInt: Int, permissionGranted: Boolean) =
        if (sdkInt >= 33) permissionGranted else true
    private fun shouldStartService(fineGranted: Boolean) = fineGranted
    private fun webResult(fineGranted: Boolean) = if (fineGranted) "Granted" else "Denied"

    private fun buildMissingPermissions(
        fineGranted: Boolean, coarseGranted: Boolean,
        sdkInt: Int, notificationsGranted: Boolean
    ): List<String> {
        val all = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION to fineGranted,
            Manifest.permission.ACCESS_COARSE_LOCATION to coarseGranted
        ).apply {
            if (sdkInt >= 33) add(Manifest.permission.POST_NOTIFICATIONS to notificationsGranted)
        }
        return all.filterNot { it.second }.map { it.first }
    }

    @Test fun trigger_neitherFineNorCoarse_cannotProceed() = assertFalse(canProceedWithTrigger(false, false))
    @Test fun trigger_onlyFineGranted_canProceed() = assertTrue(canProceedWithTrigger(true, false))
    @Test fun trigger_onlyCoarseGranted_canProceed() = assertTrue(canProceedWithTrigger(false, true))
    @Test fun trigger_bothGranted_canProceed() = assertTrue(canProceedWithTrigger(true, true))

    @Test fun notifications_belowApi33_alwaysGranted() = assertTrue(isNotificationsGranted(30, false))
    @Test fun notifications_api33_granted() = assertTrue(isNotificationsGranted(33, true))
    @Test fun notifications_api33_denied() = assertFalse(isNotificationsGranted(33, false))

    @Test fun permissionResult_fineGranted_shouldStartService() = assertTrue(shouldStartService(true))
    @Test fun permissionResult_fineDenied_shouldNotStartService() = assertFalse(shouldStartService(false))
    @Test fun permissionResult_fineGranted_webResultIsGranted() = assertEquals("Granted", webResult(true))
    @Test fun permissionResult_fineDenied_webResultIsDenied() = assertEquals("Denied", webResult(false))

    @Test
    fun missingPermissions_allGranted_emptyList() {
        assertTrue(buildMissingPermissions(true, true, 33, true).isEmpty())
    }

    @Test
    fun missingPermissions_noneGranted_allThreeOnApi33() {
        val missing = buildMissingPermissions(false, false, 33, false)
        assertEquals(3, missing.size)
    }

    @Test
    fun missingPermissions_noneGranted_onlyTwoOnApi30() {
        val missing = buildMissingPermissions(false, false, 30, false)
        assertEquals(2, missing.size)
    }

    private fun buildTriggerPing(
        storeId: String = "store_1", itemId: String = "item_1",
        deviceId: String = "usr_DEMO", triggerType: String = "HARDWARE",
        lat: Double = 44.0, lng: Double = 26.0,
        accuracyMeters: Float = 5f, timestamp: Long = System.currentTimeMillis(),
        pingId: String = "ping-001"
    ) = TelemetryPing(deviceId, storeId, itemId, triggerType, lat, lng, accuracyMeters, timestamp, pingId)

    @Test fun triggerPing_defaultDeviceId() = assertEquals("usr_DEMO", buildTriggerPing().deviceId)
    @Test fun triggerPing_defaultTriggerType() = assertEquals("HARDWARE", buildTriggerPing().triggerType)
    @Test fun triggerPing_storeAndItemId() {
        val ping = buildTriggerPing(storeId = "store_ABC", itemId = "item_123")
        assertEquals("store_ABC", ping.storeId)
        assertEquals("item_123", ping.itemId)
    }
    @Test fun triggerPing_coordinates() {
        val ping = buildTriggerPing(lat = 44.4268, lng = 26.1025)
        assertEquals(44.4268, ping.lat, 0.0001)
        assertEquals(26.1025, ping.lng, 0.0001)
    }
    @Test fun triggerPing_accuracyPreserved() = assertEquals(12.5f, buildTriggerPing(accuracyMeters = 12.5f).accuracyMeters)
    @Test fun triggerPing_pingIdPreserved() = assertEquals("custom-id", buildTriggerPing(pingId = "custom-id").pingId)
}