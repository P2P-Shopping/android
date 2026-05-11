package p2ps.android

import android.Manifest
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import p2ps.android.data.TelemetryPing

/**
 * Unit tests for permission-gating logic via PermissionDecider.
 * Tests production logic directly — changes to PermissionDecider will break these tests.
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
    fun tearDown() { unmockkAll() }

    // ── canProceedWithTrigger ─────────────────────────────────────────────────

    @Test
    fun trigger_neitherFineNorCoarse_cannotProceed() =
        assertFalse(PermissionDecider.canProceedWithTrigger(hasFine = false, hasCoarse = false))

    @Test
    fun trigger_onlyFineGranted_canProceed() =
        assertTrue(PermissionDecider.canProceedWithTrigger(hasFine = true, hasCoarse = false))

    @Test
    fun trigger_onlyCoarseGranted_canProceed() =
        assertTrue(PermissionDecider.canProceedWithTrigger(hasFine = false, hasCoarse = true))

    @Test
    fun trigger_bothGranted_canProceed() =
        assertTrue(PermissionDecider.canProceedWithTrigger(hasFine = true, hasCoarse = true))

    // ── isNotificationsGranted ────────────────────────────────────────────────

    @Test
    fun notifications_belowApi33_alwaysGranted() =
        assertTrue(PermissionDecider.isNotificationsGranted(sdkInt = 30, permissionGranted = false))

    @Test
    fun notifications_api33_grantedWhenPermissionGranted() =
        assertTrue(PermissionDecider.isNotificationsGranted(sdkInt = 33, permissionGranted = true))

    @Test
    fun notifications_api33_deniedWhenPermissionDenied() =
        assertFalse(PermissionDecider.isNotificationsGranted(sdkInt = 33, permissionGranted = false))

    @Test
    fun notifications_api34_deniedWhenPermissionDenied() =
        assertFalse(PermissionDecider.isNotificationsGranted(sdkInt = 34, permissionGranted = false))

    @Test
    fun notifications_api34_grantedWhenPermissionGranted() =
        assertTrue(PermissionDecider.isNotificationsGranted(sdkInt = 34, permissionGranted = true))

    // ── shouldStartService ────────────────────────────────────────────────────

    @Test
    fun permissionResult_fineGranted_shouldStartService() =
        assertTrue(PermissionDecider.shouldStartService(fineGranted = true))

    @Test
    fun permissionResult_fineDenied_shouldNotStartService() =
        assertFalse(PermissionDecider.shouldStartService(fineGranted = false))

    // ── webResult ─────────────────────────────────────────────────────────────

    @Test
    fun permissionResult_fineGranted_webResultIsGranted() =
        assertEquals("Granted", PermissionDecider.webResult(fineGranted = true))

    @Test
    fun permissionResult_fineDenied_webResultIsDenied() =
        assertEquals("Denied", PermissionDecider.webResult(fineGranted = false))

    // ── buildMissingPermissions ───────────────────────────────────────────────

    @Test
    fun missingPermissions_allGranted_emptyList() {
        assertTrue(
            PermissionDecider.buildMissingPermissions(
                fineGranted = true, coarseGranted = true,
                sdkInt = 33, notificationsGranted = true
            ).isEmpty()
        )
    }

    @Test
    fun missingPermissions_noneGranted_allThreeOnApi33() {
        val missing = PermissionDecider.buildMissingPermissions(
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
        val missing = PermissionDecider.buildMissingPermissions(
            fineGranted = false, coarseGranted = false,
            sdkInt = 30, notificationsGranted = false
        )
        assertEquals(2, missing.size)
        assertFalse(missing.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun missingPermissions_onlyFineGranted_returnsTwoMissing_api33() {
        val missing = PermissionDecider.buildMissingPermissions(
            fineGranted = true, coarseGranted = false,
            sdkInt = 33, notificationsGranted = false
        )
        assertEquals(2, missing.size)
        assertFalse(missing.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    // ── TelemetryPing construction ────────────────────────────────────────────

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
    @Test fun triggerPing_accuracyPreserved() =
        assertEquals(12.5f, buildTriggerPing(accuracyMeters = 12.5f).accuracyMeters)
    @Test fun triggerPing_pingIdPreserved() =
        assertEquals("custom-id", buildTriggerPing(pingId = "custom-id").pingId)
}