package p2ps.android.proximity

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import p2ps.android.BuildConfig
import p2ps.android.proximity.data.ProximityPing
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProximityClient(
    private val apiService: ProximityApiService = buildDefaultApiService()
) {
    companion object {
        private const val TAG = "ProximityClient"
        private const val BASE_URL = "http://127.0.0.1:8081/api/"

        private fun buildDefaultApiService(): ProximityApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(ProximityApiService::class.java)
        }
    }

    suspend fun sendPing(ping: ProximityPing): Boolean {
        return try {
            val response = apiService.sendProximityPing(ping)
            val success = response.isSuccessful || response.code() == 202
            if (success) {
                Log.i(TAG, "Proximity ping sent successfully")
            } else {
                Log.e(TAG, "Proximity ping failed: ${response.code()}")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Proximity ping network error: ${e.message}")
            false
        }
    }
}