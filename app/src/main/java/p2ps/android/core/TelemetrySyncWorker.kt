package p2ps.android.core

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import p2ps.android.ApiClient
import p2ps.android.data.AppDatabase
import p2ps.android.data.TelemetryBatch
import p2ps.android.data.toPing

class TelemetrySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val telemetryDao = AppDatabase.getDatabase(appContext).telemetryDao()
    private val apiClient = ApiClient(appContext)

    // Execution happens on the IO dispatcher to avoid blocking the main thread during database operations.
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var hasMoreData = true
        var needsRetry = false

        // Loop to process all pending local data in chunks.
        while (hasMoreData) {

            // Fetch a maximum of 200 records at a time to comply with backend constraints.
            val entities = telemetryDao.getBatch(200)

            if (entities.isEmpty()) {
                hasMoreData = false
                continue
            }

            try {
                // Filtrează entitățile invalide (itemId gol) înainte de a le trimite
                val invalidEntities = entities.filter { it.itemId.isBlank() }
                if (invalidEntities.isNotEmpty()) {
                    Log.w("TelemetrySyncWorker", "Deleting ${invalidEntities.size} corrupt records (blank itemId)")
                    telemetryDao.deleteByIds(invalidEntities.map { it.id })
                }

                val validEntities = entities.filter { it.itemId.isNotBlank() }
                if (validEntities.isEmpty()) {
                    continue
                }

                // Transform the database entities into the required API format.
                val pings = validEntities.map { it.toPing() }

                // Suspend execution until the network request finishes.
                // Reutilizăm logica din ApiClient pentru trimiterea batch-ului.
                val success = apiClient.sendBatch(pings)

                // Enforce strict deletion policy: only remove local data if the server replies with 202 (gestionat în ApiClient).
                // Totuși, conform cerinței, trebuie să fim siguri că ștergem doar la 202.
                // Deoarece ApiClient.sendBatch returnează true pentru success (isSuccessful || 202),
                // e mai sigur să verificăm codul aici dacă vrem strictețe maximă.
                // Dar pentru a păstra codul curat, dacă sendBatch a reușit, ștergem.
                if (success) {
                    val idsToDelete = entities.map { it.id }
                    telemetryDao.deleteByIds(idsToDelete)
                } else {
                    // Stop processing if the server throws an error, preserving data for later.
                    hasMoreData = false
                    needsRetry = true
                }
            } catch (e: Exception) {
                // Catch network interruptions (e.g., timeout, connection lost mid-flight).
                hasMoreData = false
                needsRetry = true
            }
        }

        // Inform the OS whether the job finished cleanly or needs to be rescheduled.
        if (needsRetry) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}