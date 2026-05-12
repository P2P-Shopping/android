package p2ps.android.proximity

import p2ps.android.proximity.data.ProximityPing
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API descriptor for the backend proximity endpoint. Must remain a
 * plain interface (not a `fun interface` / functional interface): Retrofit
 * builds the implementation at runtime by reflecting over annotated methods,
 * and that mechanism relies on a normal interface declaration. Even if a
 * single abstract method is present today, additional endpoints may be added
 * later, and SAM conversion is not the intended consumption model here.
 */
@Suppress("FunctionalInterface")
interface ProximityApiService {
    @POST("v1/proximity/ping")
    suspend fun sendProximityPing(@Body ping: ProximityPing): Response<Unit>
}