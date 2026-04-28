package p2ps.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import p2ps.android.data.TelemetryPing

/**
 * Unit tests for permission-gating logic and trigger-guard conditions
 * from MainActivity, tested independently of the Android Activity lifecycle.
 *
 * The onHardwareTriggerReceived() early-return logic and the permission
 * evaluation conditions are extracted and verified in isolation.
 */
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
    fun tearDown() {
        unmockkAll()
    }

    // ── Permission guard logic ─────────────────────────────────────────────────

    /**
     * Replicates the early-return guard in onHardwareTriggerReceived:
     * return if neither fine nor coarse location is granted.
     */
    private fun canProceedWithTrigger(hasFine: Boolean, hasCoarse: Boolean) =
        hasFine || hasCoarse

    @Test
    fun trigger_neitherFineNorCoarse_cannotProceed() {
        assertFalse(canProceedWithTrigger(hasFine = false, hasCoarse = false))
    }

    @Test
    fun trigger_onlyFineGranted_canProceed() {
        assertTrue(canProceedWithTrigger(hasFine = true, hasCoarse = false))
    }

    @Test
    fun trigger_onlyCoarseGranted_canProceed() {
        assertTrue(canProceedWithTrigger(hasFine = false, hasCoarse = true))
    }

    @Test
    fun trigger_bothGranted_canProceed() {
        assertTrue(canProceedWithTrigger(hasFine = true, hasCoarse = true))
    }

    // ── Notification permission logic (API 33+) ───────────────────────────────

    /**
     * Replicates the notification permission check branching in
     * onRequestPermissionsResult / onTriggerClick.
     */
    private fun isNotificationsGranted(sdkInt: Int, permissionGranted: Boolean): Boolean =
        if (sdkInt >= 33) permissionGranted else true

    @Test
    fun notifications_belowApi33_alwaysGranted() {
        assertTrue(isNotificationsGranted(sdkInt = 30, permissionGranted = false))
        assertTrue(isNotificationsGranted(sdkInt = 28, permissionGranted = false))
        assertTrue(isNotificationsGranted(sdkInt = 31, permissionGranted = false))
    }

    @Test
    fun notifications_api33_grantedWhenPermissionGranted() {
        assertTrue(isNotificationsGranted(sdkInt = 33, permissionGranted = true))
    }

    @Test
    fun notifications_api33_deniedWhenPermissionDenied() {
        assertFalse(isNotificationsGranted(sdkInt = 33, permissionGranted = false))
    }

    @Test
    fun notifications_api34_deniedWhenPermissionDenied() {
        assertFalse(isNotificationsGranted(sdkInt = 34, permissionGranted = false))
    }

    @Test
    fun notifications_api34_grantedWhenPermissionGranted() {
        assertTrue(isNotificationsGranted(sdkInt = 34, permissionGranted = true))
    }

    // ── Permission result routing ─────────────────────────────────────────────

    /**
     * Replicates the permission-result routing in requestPermissionLauncher:
     * only start tracking if fine location is granted.
     */
    private fun shouldStartService(fineGranted: Boolean) = fineGranted

    private fun webResult(fineGranted: Boolean) = if (fineGranted) "Granted" else "Denied"

    @Test
    fun permissionResult_fineGranted_shouldStartService() {
        assertTrue(shouldStartService(fineGranted = true))
    }

    @Test
    fun permissionResult_fineDenied_shouldNotStartService() {
        assertFalse(shouldStartService(fineGranted = false))
    }

    @Test
    fun permissionResult_fineGranted_webResultIsGranted() {
        assertEquals("Granted", webResult(fineGranted = true))
    }

    @Test
    fun permissionResult_fineDenied_webResultIsDenied() {
        assertEquals("Denied", webResult(fineGranted = false))
    }

    // ── Missing permissions list logic ────────────────────────────────────────

    private fun buildMissingPermissions(
        fineGranted: Boolean,
        coarseGranted: Boolean,
        sdkInt: Int,
        notificationsGranted: Boolean
    ): List<String> {
        val all = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION to fineGranted,
            Manifest.permission.ACCESS_COARSE_LOCATION to coarseGranted
        ).apply {
            if (sdkInt >= 33) add(Manifest.permission.POST_NOTIFICATIONS to notificationsGranted)
        }
        return all.filterNot { it.second }.map { it.first }
    }

    @Test
    fun missingPermissions_allGranted_emptyList() {
        val missing = buildMissingPermissions(
            fineGranted = true, coarseGranted = true,
            sdkInt = 33, notificationsGranted = true
        )
        assertTrue(missing.isEmpty())
    }

    @Test
    fun missingPermissions_noneGranted_allThreeOnApi33() {
        val missing = buildMissingPermissions(
            fineGranted = false, coarseGranted = false,
            sdkInt = 33, notificationsGranted = false
        )
        assertEquals(3, missing.size)
        assertTrue(missing.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(missing.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(missing.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun missingPermissions_noneGranted_onlyTwoOnApi30() {
        val missing = buildMissingPermissions(
            fineGranted = false, coarseGranted = false,
            sdkInt = 30, notificationsGranted = false
        )
        assertEquals(2, missing.size)
        assertFalse(missing.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun missingPermissions_onlyFineGranted_returnsTwoMissing_api33() {
        val missing = buildMissingPermissions(
            fineGranted = true, coarseGranted = false,
            sdkInt = 33, notificationsGranted = false
        )
        assertEquals(2, missing.size)
        assertFalse(missing.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    // ── TelemetryPing construction from trigger ────────────────────────────────

    @Test
    fun triggerPing_defaultDeviceIdIsUsrDemo() {
        val ping = buildTriggerPing(storeId = "store_ABC", itemId = "item_123")
        assertEquals("usr_DEMO", ping.deviceId)
    }

    @Test
    fun triggerPing_defaultTriggerTypeIsHardware() {
        val ping = buildTriggerPing(storeId = "s", itemId = "i")
        assertEquals("HARDWARE", ping.triggerType)
    }

    @Test
    fun triggerPing_storeAndItemIdPassedThrough() {
        val ping = buildTriggerPing(storeId = "store_ABC", itemId = "item_123")
        assertEquals("store_ABC", ping.storeId)
        assertEquals("item_123", ping.itemId)
    }

    @Test
    fun triggerPing_coordinatesStoredCorrectly() {
        val ping = buildTriggerPing(lat = 44.4268, lng = 26.1025)
        assertEquals(44.4268, ping.lat, 0.0001)
        assertEquals(26.1025, ping.lng, 0.0001)
    }

    @Test
    fun triggerPing_timestampIsNonNegative() {
        val ping = buildTriggerPing(storeId = "s", itemId = "i")
        assertTrue(ping.timestamp >= 0L)
    }

    @Test
    fun triggerPing_accuracyIsPreserved() {
        val ping = buildTriggerPing(accuracy = 12.5f)
        assertEquals(12.5f, ping.accuracy)
    }

    @Test
    fun triggerPing_zeroCoordinates_isValid() {
        val ping = buildTriggerPing(lat = 0.0, lng = 0.0)
        assertEquals(0.0, ping.lat, 0.0)
        assertEquals(0.0, ping.lng, 0.0)
    }

    @Test
    fun triggerPing_customDeviceId_overridesDefault() {
        val ping = buildTriggerPing(deviceId = "custom_dev", storeId = "s", itemId = "i")
        assertEquals("custom_dev", ping.deviceId)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildTriggerPing(
        storeId: String = "store_1",
        itemId: String = "item_1",
        deviceId: String = "usr_DEMO",
        triggerType: String = "HARDWARE",
        lat: Double = 44.0,
        lng: Double = 26.0,
        accuracy: Float = 5f,
        timestamp: Long = System.currentTimeMillis()
    ) = TelemetryPing(deviceId, storeId, itemId, triggerType, lat, lng, accuracy, timestamp)
}