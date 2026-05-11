package p2ps.android

import android.Manifest

/**
 * Pure utility object for permission-related decisions in MainActivity.
 * Extracted to allow unit testing without Android Activity lifecycle.
 */
object PermissionDecider {

    /**
     * Returns true if the app can proceed with a hardware trigger
     * (at least one location permission granted).
     */
    fun canProceedWithTrigger(hasFine: Boolean, hasCoarse: Boolean): Boolean =
        hasFine || hasCoarse

    /**
     * Returns true if notifications are considered granted.
     * On API < 33, notifications are always granted (no runtime permission needed).
     */
    fun isNotificationsGranted(sdkInt: Int, permissionGranted: Boolean): Boolean =
        if (sdkInt >= 33) permissionGranted else true

    /**
     * Returns true if the location tracking service should be started.
     */
    fun shouldStartService(fineGranted: Boolean): Boolean = fineGranted

    /**
     * Returns the web result string based on fine location grant status.
     */
    fun webResult(fineGranted: Boolean): String = if (fineGranted) "Granted" else "Denied"

    /**
     * Returns the list of permissions that are missing (not yet granted).
     */
    fun buildMissingPermissions(
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
}