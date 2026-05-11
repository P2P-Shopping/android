package p2ps.android.proximity

import p2ps.android.proximity.data.ProximityPing
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ProximityApiService {
    @POST("v1/proximity/ping")
    suspend fun sendProximityPing(@Body ping: ProximityPing): Response<Unit>
}