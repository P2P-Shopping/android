package p2ps.android.core

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

// Call this function whenever new telemetry data is saved locally.
fun scheduleTelemetrySync(context: Context) {

    // Ensure the job only runs when an active internet connection is available.
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val syncRequest = OneTimeWorkRequestBuilder<TelemetrySyncWorker>()
        .setConstraints(constraints)
        // Apply exponential backoff to prevent overwhelming the server if it is down.
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            WorkRequest.MIN_BACKOFF_MILLIS, // Aici am făcut corectura
            TimeUnit.MILLISECONDS
        )
        .build()

    // Enqueue unique work with the KEEP policy.
    // This guarantees that multiple quick hardware triggers won't spawn duplicate sync jobs.
    WorkManager.getInstance(context).enqueueUniqueWork(
        "ResilientTelemetrySync",
        ExistingWorkPolicy.KEEP,
        syncRequest
    )
}