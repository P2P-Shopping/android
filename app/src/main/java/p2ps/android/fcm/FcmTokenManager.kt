package p2ps.android.fcm

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages the FCM registration token.
 * Stores it persistently and provides async retrieval.
 */
object FcmTokenManager {
    private const val TAG = "FcmTokenManager"
    private const val PREFS_NAME = "p2ps_fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_FCM_TOKEN, token)
        }
        Log.d(TAG, "FCM token saved")
    }

    fun getStoredToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Suspends until the current FCM token is fetched.
     * Returns null if Firebase fails.
     */
    suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result)
                } else {
                    Log.e(TAG, "Failed to fetch FCM token", task.exception)
                    cont.resume(null)
                }
            }
    }
}