package p2ps.android

import p2ps.android.data.TelemetryBatch
import p2ps.android.data.TelemetryPing
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("v1/telemetry/ping")
    fun sendPing(@Body ping: TelemetryPing): Call<Unit>

    @POST("v1/telemetry/batch")
    fun sendBatchPings(@Body batch: TelemetryBatch): Call<Unit>
}
