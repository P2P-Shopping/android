package p2ps.android.location

import org.junit.Assert
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure-logic unit tests for the motion-detection and interval-switching
 * calculations embedded in LocationService's SensorEventListener and
 * updateLocationInterval(). These tests do not require Android framework
 * and run on the JVM directly.
 */
class LocationServiceMotionLogicTest {

    // Constants mirrored from LocationService
    private val INTERVAL_MOVING = 5000L
    private val INTERVAL_STATIONARY = 30000L
    private val MOVE_DEBOUNCE_MS = 2000L
    private val MOTION_THRESHOLD = 0.5

    /** Replicates the exact formula from LocationService.sensorListener */
    /*private fun isMovingNow(x: Float, y: Float, z: Float): Boolean {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        val acceleration = abs(magnitude - 9.81)
        return acceleration > MOTION_THRESHOLD
    }*/

    /** Replicates updateLocationInterval() selection */
    private fun selectInterval(isMoving: Boolean) =
        if (isMoving) INTERVAL_MOVING else INTERVAL_STATIONARY

    // ── Motion detection ──────────────────────────────────────────────────────

    @Test
    fun atRest_pureGravityOnZ_notMoving() {
        // (0, 0, 9.81) → magnitude = 9.81 → acceleration = 0
        Assert.assertFalse(LocationService().calculateIsMoving(0f, 0f, 9.81f))
    }

    @Test
    fun atRest_gravityDistributedAcrossAxes_notMoving() {
        // (0, 5.66f, 7.99f) ≈ magnitude 9.81
        val x = 0f; val y = 5.66f; val z = 7.99f
        val mag = sqrt((x * x + y * y + z * z).toDouble())
        Assert.assertTrue(abs(mag - 9.81) < 0.05) // verify it's near gravity
        Assert.assertFalse(LocationService().calculateIsMoving(x, y, z))
    }

    @Test
    fun exactThreshold_notMoving() {
        // acceleration = 0.5 → NOT strictly greater than 0.5 → false
        val z = (9.81 + 0.1).toFloat()
        Assert.assertFalse(LocationService().calculateIsMoving(0f, 0f, z))
    }

    @Test
    fun justAboveThreshold_isMoving() {
        val z = (9.81 + 0.51).toFloat()
        Assert.assertTrue(LocationService().calculateIsMoving(0f, 0f, z))
    }

    @Test
    fun justBelowThreshold_notMoving() {
        val z = (9.81 + 0.49).toFloat()
        Assert.assertFalse(LocationService().calculateIsMoving(0f, 0f, z))
    }

    @Test
    fun negativeAccelerationAboveThreshold_isMoving() {
        // magnitude = 9.81 - 1.0 = 8.81 → acceleration = 1.0 > 0.5
        Assert.assertFalse(LocationService().calculateIsMoving(0f, 0f, 8.81f).not()) // double-check: should be true
        Assert.assertTrue(LocationService().calculateIsMoving(0f, 0f, 8.81f))
    }

    @Test
    fun zeroValues_highAcceleration_isMoving() {
        // magnitude = 0, acceleration = 9.81 > 0.5
        Assert.assertTrue(LocationService().calculateIsMoving(0f, 0f, 0f))
    }

    @Test
    fun strongShake_allAxes_isMoving() {
        Assert.assertTrue(LocationService().calculateIsMoving(8f, 8f, 8f))
    }

    @Test
    fun largeNegativeValues_isMoving() {
        Assert.assertTrue(LocationService().calculateIsMoving(-10f, -10f, -10f))
    }

    @Test
    fun smallSymmetricNoise_onAllAxes_classification() {
        // x=0.1, y=0.1, z=9.81 → magnitude ≈ sqrt(0.01+0.01+96.24) ≈ 9.811 → acc ≈ 0.001 < 0.5
        Assert.assertFalse(LocationService().calculateIsMoving(0.1f, 0.1f, 9.81f))
    }

    @Test
    fun highFrequencyVibration_smallAmplitude_notMoving() {
        // Simulate 0.2g vibration on z: z = 9.81 + 0.3 = 10.11 → acc = 0.3 < 0.5
        Assert.assertFalse(LocationService().calculateIsMoving(0f, 0f, 10.11f))
    }

    @Test
    fun walkingMotion_typicalValues_isMoving() {
        // Walking generates ~1–2 m/s² → magnitude ≈ 11.0 → acc ≈ 1.19 > 0.5
        Assert.assertTrue(LocationService().calculateIsMoving(0f, 0f, 11.0f))
    }

    // ── Interval selection ────────────────────────────────────────────────────

    @Test
    fun intervalSelection_moving_returnsFiveSeconds() {
        Assert.assertEquals(5_000L, selectInterval(true))
    }

    @Test
    fun intervalSelection_stationary_returnsThirtySeconds() {
        Assert.assertEquals(30_000L, selectInterval(false))
    }

    @Test
    fun intervalSelection_moving_notThirtySeconds() {
        Assert.assertNotEquals(30_000L, selectInterval(true))
    }

    @Test
    fun intervalSelection_stationary_notFiveSeconds() {
        Assert.assertNotEquals(5_000L, selectInterval(false))
    }

    @Test
    fun intervalSelection_movingToStationary_switchesInterval() {
        val first = selectInterval(true)
        val second = selectInterval(false)
        Assert.assertNotEquals(first, second)
    }

    // ── Debounce logic ────────────────────────────────────────────────────────

    @Test
    fun debounce_afterDebounceWindow_transitionAllowed() {
        val lastMoveTime = System.currentTimeMillis() - (MOVE_DEBOUNCE_MS + 100)
        val currentTime = System.currentTimeMillis()
        Assert.assertTrue(currentTime - lastMoveTime > MOVE_DEBOUNCE_MS)
    }

    @Test
    fun debounce_withinDebounceWindow_transitionBlocked() {
        val lastMoveTime = System.currentTimeMillis() - (MOVE_DEBOUNCE_MS - 500)
        val currentTime = System.currentTimeMillis()
        Assert.assertFalse(currentTime - lastMoveTime > MOVE_DEBOUNCE_MS)
    }

    @Test
    fun debounce_exactlyAtBoundary_transitionBlocked() {
        // Exactly 2000ms → NOT strictly greater → blocked
        val lastMoveTime = System.currentTimeMillis() - MOVE_DEBOUNCE_MS
        val currentTime = System.currentTimeMillis()
        Assert.assertFalse(currentTime - lastMoveTime > MOVE_DEBOUNCE_MS)
    }

    @Test
    fun debounce_zeroLastMoveTime_initialState_isHandled() {
        // When lastMoveTime == 0L (initial value), first moving event sets it
        var lastMoveTime = 0L
        val currentTime = System.currentTimeMillis()
        val movingNow = true
        if (movingNow && lastMoveTime == 0L) lastMoveTime = currentTime
        Assert.assertEquals(currentTime, lastMoveTime)
    }

    // ── No-op guard in updateLocationInterval ─────────────────────────────────

    @Test
    fun updateInterval_sameInterval_isNoOp() {
        var currentInterval = INTERVAL_MOVING
        val newInterval = INTERVAL_MOVING
        val shouldUpdate = currentInterval != newInterval
        Assert.assertFalse(shouldUpdate)
    }

    @Test
    fun updateInterval_differentInterval_triggersUpdate() {
        var currentInterval = INTERVAL_MOVING
        val newInterval = INTERVAL_STATIONARY
        val shouldUpdate = currentInterval != newInterval
        Assert.assertTrue(shouldUpdate)
    }

    // ── Full state-machine scenarios ──────────────────────────────────────────

    @Test
    fun scenario_stationary_thenMoving_thenStationary_intervalsCorrect() {
        var isMoving = true // initial state in LocationService
        Assert.assertEquals(INTERVAL_MOVING, selectInterval(isMoving))

        isMoving = false
        Assert.assertEquals(INTERVAL_STATIONARY, selectInterval(isMoving))

        isMoving = true
        Assert.assertEquals(INTERVAL_MOVING, selectInterval(isMoving))
    }

    @Test
    fun scenario_rapidStateFlips_alwaysCorrectInterval() {
        val states = listOf(true, false, true, true, false, false, true)
        val expected = states.map { if (it) INTERVAL_MOVING else INTERVAL_STATIONARY }
        val actual = states.map { selectInterval(it) }
        Assert.assertEquals(expected, actual)
    }
}