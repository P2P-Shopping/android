package p2ps.android.core

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import p2ps.android.ApiClient
import p2ps.android.data.AppDatabase
import p2ps.android.data.TelemetryEntity
import p2ps.android.data.toPing

class TelemetrySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val telemetryDao = AppDatabase.getDatabase(appContext).telemetryDao()
    private val apiClient = ApiClient(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var needsRetry = false

        while (true) {
            val entities = telemetryDao.getBatch(200)
            if (entities.isEmpty()) break

            val success = try {
                processBatch(entities)
            } catch (e: Exception) {
                Log.e("TelemetrySyncWorker", "Batch sync failed: ${e.message}")
                false
            }

            if (!success) {
                needsRetry = true
                break
            }
        }

        if (needsRetry) Result.retry() else Result.success()
    }

    private suspend fun processBatch(entities: List<TelemetryEntity>): Boolean {
        // 1. Clean up invalid data first
        val validEntities = filterAndCleanupCorrupt(entities)
        
        // If the batch only contained corrupt data, we consider it "processed" and continue
        if (validEntities.isEmpty()) return true

        // 2. Send valid data to server
        val pings = validEntities.map { it.toPing() }
        val apiSuccess = apiClient.sendBatch(pings)

        if (apiSuccess) {
            // Delete the entire original batch from local DB upon server confirmation
            telemetryDao.deleteByIds(entities.map { it.id })
            return true
        }

        return false
    }

    private suspend fun filterAndCleanupCorrupt(entities: List<TelemetryEntity>): List<TelemetryEntity> {
        val invalid = entities.filter { it.itemId.isBlank() }
        if (invalid.isNotEmpty()) {
            Log.w("TelemetrySyncWorker", "Cleaning up ${invalid.size} corrupt records")
            telemetryDao.deleteByIds(invalid.map { it.id })
        }
        return entities.filter { it.itemId.isNotBlank() }
    }
}
