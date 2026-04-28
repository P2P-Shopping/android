package p2ps.android

import p2ps.android.data.TelemetryBatch
import p2ps.android.data.TelemetryPing
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("v1/telemetry/ping")
    suspend fun sendPing(@Body ping: TelemetryPing): Response<Unit>

    @POST("v1/telemetry/batch")
    suspend fun sendBatchPings(@Body batch: TelemetryBatch): Response<Unit>
}