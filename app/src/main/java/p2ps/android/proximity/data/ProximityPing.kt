package p2ps.android.proximity.data

data class ProximityPing(
    val deviceId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
    val fcmToken: String
)