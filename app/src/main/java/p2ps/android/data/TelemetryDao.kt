package p2ps.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPing(ping: TelemetryEntity)

    @Query("SELECT * FROM telemetry_cache ORDER BY timestamp ASC")
    suspend fun getAllPings(): List<TelemetryEntity>

    @Query("SELECT COUNT(*) FROM telemetry_cache")
    suspend fun getCount(): Int

    @Delete
    suspend fun deletePings(pings: List<TelemetryEntity>)

    @Query("DELETE FROM telemetry_cache")
    suspend fun clearCache()
}