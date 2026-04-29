package p2ps.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_cache")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "trigger_type") val triggerType: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "accuracy") val accuracy: Float,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "ping_id") val pingId: String
)