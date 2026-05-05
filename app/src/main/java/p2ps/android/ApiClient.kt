package p2ps.android

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import p2ps.android.data.TelemetryBatch
import p2ps.android.data.TelemetryPing
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import p2ps.android.BuildConfig

class ApiClient(context: Context) {
    companion object {
        private const val TAG = "ApiClient"
        // Folosim 127.0.0.1 pentru ADB Reverse prin USB
        private const val BASE_URL = "http://127.0.0.1:8081/api/"
    }

    private val apiService: ApiService
    private val deviceId: String

    init {
        val prefs = context.getSharedPreferences("p2ps_prefs", Context.MODE_PRIVATE)
        deviceId = prefs.getString("DEVICE_ID", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("DEVICE_ID", it).apply()
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-API-Key", BuildConfig.API_KEY) // Din local.properties
                    .addHeader("X-Device-Id", deviceId)          // Dinamic per dispozitiv
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    suspend fun sendPing(ping: TelemetryPing): Boolean {
        return try {
            Log.d(TAG, "Send telemetry for: ${ping.itemId}")
            val response = apiService.sendPing(ping)
            if (response.isSuccessful || response.code() == 202) {
                Log.i(TAG, "Success: ${response.code()}")
                true
            } else {
                Log.e(TAG, "Server error: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}")
            false
        }
    }

    suspend fun sendBatch(pings: List<TelemetryPing>): Boolean {
        return try {
            val response = apiService.sendBatchPings(TelemetryBatch(pings))
            response.isSuccessful || response.code() == 202
        } catch (e: Exception) {
            Log.e(TAG, "Eroare batch: ${e.message}")
            false
        }
    }
}