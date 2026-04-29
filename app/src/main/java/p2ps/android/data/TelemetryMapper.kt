package p2ps.android.data

// Convert network model to database entity
fun TelemetryPing.toEntity(): TelemetryEntity {
    return TelemetryEntity(
        deviceId = this.deviceId,
        storeId = this.storeId,
        itemId = this.itemId,
        triggerType = this.triggerType,
        latitude = this.lat,
        longitude = this.lng,
        accuracy = this.accuracyMeters,
        timestamp = this.timestamp,
        pingId = this.pingId
    )
}

// Convert database entity to network payload
fun TelemetryEntity.toPing(): TelemetryPing {
    return TelemetryPing(
        deviceId = this.deviceId,
        storeId = this.storeId,
        itemId = this.itemId,
        triggerType = this.triggerType,
        lat = this.latitude,
        lng = this.longitude,
        accuracyMeters = this.accuracy,
        timestamp = this.timestamp,
        pingId = this.pingId
    )
}