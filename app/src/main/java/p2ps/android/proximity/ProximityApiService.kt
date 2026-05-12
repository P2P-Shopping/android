package p2ps.android.proximity

import p2ps.android.proximity.data.ProximityPing
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API descriptor for the backend proximity endpoint.
 *
 * Declared as a `fun interface` because it currently has a single abstract
 * method (Sonar S6517). The functional-interface marker is purely a Kotlin
 * compile-time hint enabling SAM conversion; the JVM bytecode is identical
 * to a plain interface, so Retrofit's runtime reflection-based proxy
 * generation (`retrofit.create(...)`) works without change. If additional
 * endpoints are added later, simply drop the `fun` keyword.
 */
fun interface ProximityApiService {
    @POST("v1/proximity/ping")
    suspend fun sendProximityPing(@Body ping: ProximityPing): Response<Unit>
}